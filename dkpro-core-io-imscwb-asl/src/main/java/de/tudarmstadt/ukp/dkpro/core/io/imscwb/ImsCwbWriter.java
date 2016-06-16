/*
 * Copyright 2011
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.dkpro.core.io.imscwb;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This Consumer outputs the content of all CASes into the IMS workbench format.
 *
 * This writer produces a text file which needs to be converted to the binary IMS CWB index files
 * using the command line tools that come with the CWB.
 *
 * It is possible to set the parameter {@link #PARAM_CQP_HOME} to directly create output in the
 * native binary CQP format via the original CWB command line tools.
 *
 */

@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma" })
public class ImsCwbWriter
    extends JCasAnnotator_ImplBase
{
	public static final String E_SENTENCE = "s";
	public static final String E_TEXT = "text";
	public static final String E_DOCUMENT = "document";
	public static final String ATTR_BEGIN = "begin";
	public static final String ATTR_END = "end";
	public static final String ATTR_POS = "pos";
	public static final String ATTR_CPOS = "cpos";
	public static final String ATTR_LEMMA = "lemma";
	public static final String ATTR_ID = "id";
	public static final String ATTR_URI = "uri";

	/**
	 * Location to which the output is written.
	 */
	public static final String PARAM_TARGET_LOCATION = ComponentParameters.PARAM_TARGET_LOCATION;
	@ConfigurationParameter(name = PARAM_TARGET_LOCATION, mandatory = true)
	private File outputFile;

	/**
	 * Character encoding of the output data.
	 */
	public static final String PARAM_TARGET_ENCODING = ComponentParameters.PARAM_TARGET_ENCODING;
	@ConfigurationParameter(name = PARAM_TARGET_ENCODING, mandatory = true, defaultValue = "UTF-8")
	private String encoding;

	/**
	 * Write the document ID for each token. It is usually a better idea to generate a
	 * {@link #PARAM_WRITE_DOCUMENT_TAG document tag} or a {@link #PARAM_WRITE_TEXT_TAG text tag}
	 * which also contain the document ID that can be queried in CQP.
	 */
	public static final String PARAM_WRITE_DOC_ID = "writeDocId";
	@ConfigurationParameter(name = PARAM_WRITE_DOC_ID, mandatory = true, defaultValue = "false")
	private boolean writeDocId;

	/**
	 * Write part-of-speech tags.
	 */
	public static final String PARAM_WRITE_POS = "writePOS";
	@ConfigurationParameter(name = PARAM_WRITE_POS, mandatory = true, defaultValue = "true")
	private boolean writePOS;

	/**
	 * Write coarse-grained part-of-speech tags. These are the simple names of the UIMA types used
	 * to represent the part-of-speech tag.
	 */
	public static final String PARAM_WRITE_CPOS = "writeCPOS";
	@ConfigurationParameter(name = PARAM_WRITE_CPOS, mandatory = true, defaultValue = "false")
	private boolean writeCPOS;

	/**
	 * Write lemmata.
	 */
	public static final String PARAM_WRITE_LEMMA = "writeLemma";
	@ConfigurationParameter(name = PARAM_WRITE_LEMMA, mandatory = true, defaultValue = "true")
	private boolean writeLemma;

	/**
	 * Write a pseudo-XML tag with the name {@code document} to mark the start and end of a
	 * document.
	 */
	public static final String PARAM_WRITE_DOCUMENT_TAG = "writeDocumentTag";
	@ConfigurationParameter(name = PARAM_WRITE_DOCUMENT_TAG, mandatory = true, defaultValue = "false")
	private boolean writeDocumentTag;

	/**
	 * Write a pseudo-XML tag with the name {@code text} to mark the start and end of a document.
	 * This is used by CQPweb.
	 */
	public static final String PARAM_WRITE_TEXT_TAG = "writeTextTag";
	@ConfigurationParameter(name = PARAM_WRITE_TEXT_TAG, mandatory = true, defaultValue = "true")
	private boolean writeTextTag;

	/**
	 * Write the start and end position of each token.
	 */
	public static final String PARAM_WRITE_OFFSETS = "writeOffsets";
	@ConfigurationParameter(name = PARAM_WRITE_OFFSETS, mandatory = true, defaultValue = "false")
	private boolean writeOffsets;

	/**
	 * Write additional token-level annotation features. These have to be given as an array of fully
	 * qualified feature paths (fully.qualified.classname/featureName). The names for these
	 * annotations in CQP are their lowercase shortnames.
	 */
	public static final String PARAM_ADDITIONAL_FEATURES = "additionalFeatures";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_FEATURES, mandatory = false)
	private String[] additionalFeatures;

	/**
	 * Make document IDs compatible with CQPweb. CQPweb demands an id consisting of only letters,
	 * numbers and underscore.
	 */
	public static final String PARAM_CQPWEB_COMPATIBILITY = "cqpwebCompatibility";
	@ConfigurationParameter(name = PARAM_CQPWEB_COMPATIBILITY, mandatory = true, defaultValue = "false")
	private boolean cqpwebCompatibility;

	/**
	 * Set this parameter to the directory containing the cwb-encode and cwb-makeall commands if you
	 * want the write to directly encode into the CQP binary format.
	 */
	public static final String PARAM_CQP_HOME = "cqpHome";
	@ConfigurationParameter(name = PARAM_CQP_HOME, mandatory = false)
	private File cqpHome;

	/**
	 * Set this parameter to compress the token streams and the indexes using cwb-huffcode and
	 * cwb-compress-rdx. With modern hardware, this may actually slow down queries, so we turn it
	 * off by default. If you have large data sets, you best try yourself what works best for you.
	 * (default: false)
	 */
	public static final String PARAM_CQP_COMPRESS = "cqpCompress";
	@ConfigurationParameter(name = PARAM_CQP_COMPRESS, mandatory = true, defaultValue = "false")
	private boolean cqpCompress;

	/**
	 * The name of the generated corpus.
	 */
	public static final String PARAM_CORPUS_NAME = "corpusName";
	@ConfigurationParameter(name = PARAM_CORPUS_NAME, mandatory = true, defaultValue = "corpus")
	private String corpusName;

    public static final String PARAM_SENTENCE_TAG = "sentenceTag";
    @ConfigurationParameter(name = PARAM_SENTENCE_TAG, mandatory = true, defaultValue = E_SENTENCE)
    private String sentenceTag;

	private static final String LS = "\n";
	private static final String TAB = "\t";
	private Writer bw;
	private int currentId;

	private Process childProcess;
	private File dataDirectory;
	private File registryDirectory;

	@Override
	public void initialize(UimaContext context)
	    throws ResourceInitializationException
	{
		super.initialize(context);

		try {
			File parentFile = outputFile.getParentFile();
			if (parentFile != null) {
				forceMkdir(parentFile);
			}
		}
		catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

		try {
			bw = getWriter();
		}
		catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

		currentId = 0;
	}

	@Override
	public void process(JCas jcas)
	    throws AnalysisEngineProcessException
	{
		String documentId = DocumentMetaData.get(jcas).getDocumentId();
		String documentUri = DocumentMetaData.get(jcas).getDocumentUri();

		// CQPweb demands an id consisting of only letters, numbers and underscore
		if (cqpwebCompatibility) {
			// if the documentTag is written as well keep the id, else use the uri instead
			if (writeDocumentTag) {
				if (documentId == null || documentId.length() == 0) {
					documentId = Integer.toString(currentId);
				}
				documentId = documentId.replaceAll("[^\\d\\w_]", "_");
			}
			else {
				if (documentUri == null || documentUri.length() == 0) {
					documentUri = Integer.toString(currentId);
				}
				documentId = documentUri.replaceAll("[^\\d\\w_]", "_");
			}
		}

		try {
			if (writeTextTag) {
				startElement(E_TEXT, ATTR_ID, documentId);
			}
			if (writeDocumentTag) {
				startElement(E_DOCUMENT, ATTR_URI, documentUri);
			}
			for (Sentence sentence : select(jcas, Sentence.class)) {
				attendChildProceess();
				startElement(sentenceTag);
				for (Token token : selectCovered(jcas, Token.class, sentence)) {
					// write token
					bw.write(escapeXml(token.getCoveredText()));

					// write pos tag
					if (writePOS) {
						field(token.getPos() != null ? token.getPos().getPosValue() : "-");
					}

					// write coarse grained pos tag
					if (writeCPOS) {
						field(token.getPos() != null ? token.getPos().getType().getShortName()
						        : "-");
					}

					// write lemma
					if (writeLemma) {
						field(token.getLemma() != null ? token.getLemma().getValue() : "-");
					}

					// write doc-id
					if (writeDocId) {
						field(documentId);
					}

					// write offsets
					if (writeOffsets) {
						field(String.valueOf(token.getBegin()));
						field(String.valueOf(token.getEnd()));
					}

					// write additional tags
					if (additionalFeatures != null) {
						// try {
						for (String featurePath : additionalFeatures) {
							String val = getCoveredAnnotationFeatureValue(featurePath, token);
							field(val);
						}
						// }
					}

					bw.write(LS);
				}
				endElement(sentenceTag);
			}
			if (writeDocumentTag) {
				endElement(E_DOCUMENT);
			}
			if (writeTextTag) {
				endElement(E_TEXT);
			}

			currentId++;
		}
		catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	private void startElement(String aElement, String... aAttributes)
	    throws IOException
	{
		bw.write('<');
		bw.write(aElement);
		if (aAttributes != null && aAttributes.length > 0) {
			bw.write(" ");
			for (int i = 0; i < aAttributes.length; i += 2) {
				bw.write(aAttributes[i]);
				bw.write("=\"");
				bw.write(escapeXml(aAttributes[i + 1]));
				bw.write('"');
			}
		}
		bw.write('>');
		bw.write(LS);
	}

	private void endElement(String aElement)
	    throws IOException
	{
		bw.write("</");
		bw.write(aElement);
		bw.write('>');
		bw.write(LS);

	}

	private void field(String aValue)
	    throws IOException
	{
		bw.write(TAB);
		bw.write(escapeXml(aValue));
	}

	private Writer getWriter()
	    throws IOException
	{
		if (cqpHome != null) {
			dataDirectory = new File(outputFile, "data");
			registryDirectory = new File(outputFile, "registry");
			forceMkdir(dataDirectory);
			forceMkdir(registryDirectory);

			List<String> cmd = new ArrayList<String>();
			cmd.add(new File(cqpHome, "cwb-encode").getAbsolutePath());

			cmd.add("-c");
			cmd.add(getCwbCharset(encoding));
			// -x XML-aware (replace XML entities and ignore <!.. and <?..)
			cmd.add("-x");
			// -s skip empty lines in input data (recommended)
			cmd.add("-s");
			// -B strip leading/trailing blanks from (input lines & token annotations)
			cmd.add("-B");
			// -d <dir> directory for data files created by ./cwb-encode
			cmd.add("-d");
			cmd.add(dataDirectory.getPath());
			// -R <rf> create registry entry (named <rf>) listing all encoded attributes
			cmd.add("-R");
			cmd.add(new File(registryDirectory, corpusName).getPath());

			// -P <att> declare additional p-attribute <att>
			if (writePOS) {
				cmd.add("-P");
				cmd.add(ATTR_POS);
			}

			if (writeCPOS) {
				cmd.add("-P");
				cmd.add(ATTR_CPOS);
			}

			if (writeLemma) {
				cmd.add("-P");
				cmd.add(ATTR_LEMMA);
			}

			if (writeDocId) {
				cmd.add("-P");
				cmd.add(ATTR_URI);
			}

			if (writeOffsets) {
				cmd.add("-P");
				cmd.add(ATTR_BEGIN);
				cmd.add("-P");
				cmd.add(ATTR_END);
			}

			if (additionalFeatures != null) {
				for (String featurePath : additionalFeatures) {
					String[] segments = featurePath.split("/", 2);
					if (segments.length != 2) {
						throw new IllegalArgumentException("Given feature path is malformed: ["
						        + featurePath + "] (exactly one \"/\" (slash) must exist).");
					}
					String typeName = segments[0];
					String featureName = segments.length > 1 ? segments[1] : "";
					String name = (StringUtils.substringAfterLast(typeName, ".") + "_" + featureName)
					        .toLowerCase();
					cmd.add("-P");
					cmd.add(name);
				}
			}

			if (writeDocumentTag) {
				cmd.add("-S");
				cmd.add(E_DOCUMENT + ":0+" + ATTR_URI);
			}

			if (writeTextTag) {
				cmd.add("-S");
				cmd.add(E_TEXT + ":0+" + ATTR_ID);
			}

			{
				cmd.add("-S");
				cmd.add(sentenceTag + ":0");
			}

			getLogger().info("Spawning cwb-encode: " + join(cmd, " "));

			final ProcessBuilder pb = new ProcessBuilder();
			pb.command(cmd);
			childProcess = pb.start();
			return new OutputStreamWriter(childProcess.getOutputStream(), encoding);
		}
		else {
			return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),
			        encoding));
		}
	}

	private void attendChildProceess()
	{
		if (childProcess != null) {
			try {
				InputStream stdout = childProcess.getInputStream();
				if (stdout.available() > 0) {
					byte[] data = new byte[stdout.available()];
					stdout.read(data);
					getLogger().info(new String(data, "UTF-8"));
				}
				InputStream stderr = childProcess.getErrorStream();
				if (stderr.available() > 0) {
					byte[] data = new byte[stderr.available()];
					stderr.read(data);
					getLogger().error(new String(data, "UTF-8"));
				}
			}
			catch (IOException e) {
				getLogger().error("Unable to communicate with child process");
			}
		}
	}

	@Override
	public void collectionProcessComplete()
	    throws AnalysisEngineProcessException
	{
		IOUtils.closeQuietly(bw);
		if (childProcess != null) {
			try {
				childProcess.waitFor();
				attendChildProceess();
				childProcess = null;
			}
			catch (InterruptedException e) {
				throw new AnalysisEngineProcessException(e);
			}

			runCwbCommand("cwb-makeall", "-r", registryDirectory.getPath(), "-V",
			        corpusName.toUpperCase());

			if (cqpCompress) {
				// Compress the token sequence of a positional attribute. Creates .huf, .hcd,
				// and .huf.syn files, which replace the corresponding .corpus files. After
				// running this tool successfully, the .corpus files can be deleted.
				runCwbCommand("cwb-huffcode", "-r", registryDirectory.getPath(), "-A",
				        corpusName.toUpperCase());
				for (File f : listFiles(dataDirectory, new String[] { "huf" }, false)) {
					deleteQuietly(new File(removeExtension(f.getPath()) + ".corpus"));
				}

				// Compress the index of a positional attribute. Creates .crc and .crx files
				// which replace the corresponding .corpus.rev and .corpus.rdx files. After
				// running this tool successfully, the latter files can be deleted.
				runCwbCommand("cwb-compress-rdx", "-r", registryDirectory.getPath(), "-A",
				        corpusName.toUpperCase());
				for (File f : listFiles(dataDirectory, new String[] { "crc" }, false)) {
					deleteQuietly(new File(removeExtension(f.getPath()) + ".corpus.rev"));
					deleteQuietly(new File(removeExtension(f.getPath()) + ".corpus.rdx"));
				}
			}
		}
	}

	private void runCwbCommand(String aCommand, String... aArguments)
	    throws AnalysisEngineProcessException
	{
		try {
			List<String> args = new ArrayList<String>(aArguments.length + 1);
			args.add(new File(cqpHome, aCommand).getAbsolutePath());
			for (String arg : aArguments) {
				args.add(arg);
			}

			ProcessBuilder pb = new ProcessBuilder(args);
			getLogger().info("Spawning " + aCommand + ": " + join(args, " "));
			childProcess = pb.start();
			childProcess.waitFor();
		}
		catch (InterruptedException e) {
			throw new AnalysisEngineProcessException(e);
		}
		catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
		finally {
			attendChildProceess();
			childProcess = null;
		}
	}

	private static Map<String, String> CHARSET_MAPPING = new HashMap<String, String>();
	static {
		CHARSET_MAPPING.put("ISO-8859-1", "latin1");
		CHARSET_MAPPING.put("UTF-8", "utf8");
	}

	private static String getCwbCharset(String aEncoding)
	{
		String enc = CHARSET_MAPPING.get(aEncoding);
		if (enc == null) {
			throw new IllegalArgumentException("Encoding [" + enc + "] not supported by CWB.");
		}
		return enc;
	}

	private static String escapeXml(String aString)
	{
		return aString.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
		        .replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
	}

	/**
	 * Get the feature value of an annotation which is covered by another annotation.
	 *
	 * @param aFeaturePath
	 *            The fully qualified feature path of the feature in question:
	 *            your.package.and.annotation.class.name/featureName
	 * @param aCoveringAnnotation
	 *            The annotation that covers the annotation for which the feature value should be
	 *            extracted.
	 * @return the feature value if a feature name is given; coveredText if only the annotation is
	 *         given
	 */
	public String getCoveredAnnotationFeatureValue(String aFeaturePath,
	        AnnotationFS aCoveringAnnotation)
	{
		String[] segments = aFeaturePath.split("/", 2);
		if (segments.length != 2) {
			throw new IllegalArgumentException("Given feature path is malformed: [" + aFeaturePath
			        + "] (exactly one \"/\" (slash) must exist).");
		}
		String typeName = segments[0];
		String featureName = segments[1];
		Type type = CasUtil.getAnnotationType(aCoveringAnnotation.getCAS(), typeName);
		Feature feature = type.getFeatureByBaseName(featureName);
		if (feature == null) {
			throw new IllegalArgumentException("Feature [" + featureName
			        + "] is not defined for type [" + type + "] (check lower/uppercase spelling).");
		}

		List<AnnotationFS> covered = CasUtil.selectCovered(type, aCoveringAnnotation);
		switch (covered.size()) {
		case 0:
			if (getLogger().isWarnEnabled()) {
				getLogger().warn(
				        "There is no annotation of type [" + typeName
				                + "] available which is covered by [" + aCoveringAnnotation
				                + "], returning empty string.");
			}
			return "";
		case 1:
			return covered.get(0).getFeatureValueAsString(feature);
		default:
			if (getLogger().isWarnEnabled()) {
				getLogger().warn(
				        "There are multiple annotations of type [" + typeName
				                + "] available which are covered by [" + aCoveringAnnotation
				                + "], returning the first.");
			}
			return covered.get(0).getFeatureValueAsString(feature);
		}
	}
}
