/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
/*

	Copyright 2017 Danny Kunz

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.


*/
package org.omnaest.genomics.fasta.domain;

import java.util.stream.Stream;

import org.omnaest.genomics.fasta.FastaUtils;
import org.omnaest.genomics.fasta.FastaUtils.CodeAndMeta;
import org.omnaest.genomics.translator.domain.AminoAcidCodeSequence;
import org.omnaest.genomics.translator.domain.NucleicAcidCodeSequence;

/**
 * Representation of the data of a FASTA file
 * 
 * @see FastaUtils#load()
 * @author omnaest
 */
public interface FASTAData
{
    /**
     * Returns the {@link CodeAndMeta} {@link Stream}
     * 
     * @return
     */
    public Stream<CodeAndMeta> getSequence();

    public NucleicAcidCodeSequence asNucleicAcidCodeSequence();

    public AminoAcidCodeSequence asAminoAcidCodeSequence();

    /**
     * Returns an array representation of the {@link Code} sequence
     * 
     * @see #asCharacters()
     * @see #toString()
     * @return
     */
    public Code[] asCodes();

    /**
     * Returns an array representation of the {@link Character} sequence
     * 
     * @see #toString()
     * @return
     */
    public char[] asCharacters();

    /**
     * @see FASTADataWriter
     * @return
     */
    public FASTADataWriter write();

    /**
     * Similar to {@link #asString()}
     * 
     * @return
     */
    @Override
    public String toString();

    /**
     * Returns the {@link String} representation of {@link #asCharacters()}
     * 
     * @return
     */
    public String asString();

}
