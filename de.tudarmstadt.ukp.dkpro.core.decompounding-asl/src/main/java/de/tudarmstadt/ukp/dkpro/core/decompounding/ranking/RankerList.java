/*******************************************************************************
 * Copyright 2010
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
 *******************************************************************************/

package de.tudarmstadt.ukp.dkpro.core.decompounding.ranking;

import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.DecompoundedWord;

/**
 * Ranking interface for list of splits
 * 
 * @author <a href="mailto:je.haase@googlemail.com">Jens Haase</a>
 */
public interface RankerList extends Ranker
{

	/**
	 * Ranks the given splits and returns the split with highest rank
	 * 
	 * @param aSplits
	 *            The highest ranked split
	 */
	public DecompoundedWord highestRank(List<DecompoundedWord> aSplits);

	/**
	 * Ranks all splits and returns a order list
	 */
	public List<DecompoundedWord> rank(List<DecompoundedWord> aSplits);
}