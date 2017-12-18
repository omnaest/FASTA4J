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
package org.omnaest.genetics.fasta.domain;

import java.util.stream.Stream;

import org.omnaest.genetics.translator.TranslatableCode;

/**
 * Representation of a code position
 * 
 * @see #getRawCode()
 * @see #getReadPosition()
 * @see #asTranslatableCode()
 * @see #newInstanceWithReplacedCode(char)
 * @author omnaest
 */
public interface Code
{
	/**
	 * Returns the raw code which has been read for the current position
	 *
	 * @return
	 */
	public char getRawCode();

	/**
	 * Returns a {@link TranslatableCode} wrapper around the raw {@link #getRawCode()}
	 *
	 * @return
	 */
	public TranslatableCode asTranslatableCode();

	/**
	 * Returns the read position within the {@link Stream} of codes
	 *
	 * @return
	 */
	public long getReadPosition();

	/**
	 * Returns a new {@link Code} instance with the same read position but new code reference
	 * 
	 * @param replacement
	 * @return
	 */
	public Code newInstanceWithReplacedCode(char rawCode);

	public static Code of(char codeReference, long readPosition)
	{
		return new DefaultCode(codeReference, readPosition);
	}
}