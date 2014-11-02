/*******************************************************************************
 * Copyright 2011
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universit√§t Darmstadt
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
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.core.io.bnc;

import static de.tudarmstadt.ukp.dkpro.core.testing.IOTestRunner.*;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Writer;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class BncReaderTest
{
	@Test
	public void test() throws Exception
	{
	    testOneWay(BncReader.class, Conll2006Writer.class, 
	            "reference/FX8.conll",
	            "FX8.xml");
	}
	
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
