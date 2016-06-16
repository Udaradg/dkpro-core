/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.dkpro.core.io.brat.internal.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

public abstract class BratDrawingDecl
{
    private final String type;

    public BratDrawingDecl(String aType)
    {
        super();
        type = aType;
    }

    public String getType()
    {
        return type;
    }

    public abstract String getSpec();
    
    public abstract void write(JsonGenerator aJG)
        throws IOException;
    
    @Override
    public String toString()
    {
        return type + '\t' + getSpec();
    }
}
