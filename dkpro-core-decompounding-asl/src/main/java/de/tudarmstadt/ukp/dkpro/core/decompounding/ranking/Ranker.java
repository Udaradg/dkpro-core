/*
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
 **/
package de.tudarmstadt.ukp.dkpro.core.decompounding.ranking;

import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.DecompoundedWord;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.DecompoundingTree;
import de.tudarmstadt.ukp.dkpro.core.decompounding.web1t.Finder;

/**
 * The ranking algorithm interface
 */
public interface Ranker
{
    /**
     * Returns the highest ranked split
     *
     * @param splits
     *            A split tree generated by a splitting algorithm
     * @return the highest ranked split
     */
    public DecompoundedWord highestRank(DecompoundingTree splits);

    /**
     * Sets the finder to the ranking algorithm
     * 
     * @param aFinder
     *            the finder.
     */
    public void setFinder(Finder aFinder);

}
