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

import org.omnaest.genomics.translator.TranslatableCode;
import org.omnaest.genomics.translator.TranslatableCodeImpl;

public class DefaultCode implements Code
{
	private char	codeReference;
	private long	readPosition;

	public DefaultCode(char codeReference, long readPosition)
	{
		super();
		this.codeReference = codeReference;
		this.readPosition = readPosition;
	}

	@Override
	public long getReadPosition()
	{
		return this.readPosition;
	}

	@Override
	public char getRawCode()
	{
		return this.codeReference;
	}

	@Override
	public TranslatableCode asTranslatableCode()
	{
		return new TranslatableCodeImpl(this.getRawCode(), this.getReadPosition());
	}

	@Override
	public String toString()
	{
		return "DefaultCode [codeReference=" + this.codeReference + ", readPosition=" + this.readPosition + "]";
	}

	@Override
	public Code newInstanceWithReplacedCode(char rawCode)
	{
		return new DefaultCode(rawCode, this.readPosition);
	}

}