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
package org.omnaest.genetics.fasta;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class FastaUtilsTest
{

	@Test
	public void testLoad() throws Exception
	{
		long sequenceLength = FastaUtils.load()
										.fromGZIP(this	.getClass()
														.getResourceAsStream("/chr21.fa.gz"))
										.getSequence()
										.count();
		assertEquals(48129895, sequenceLength);
	}

	@Test
	public void testLoadExample() throws Exception
	{
		String parsedContent = FastaUtils	.load()
											.from(this	.getClass()
														.getResourceAsStream("/example.fasta"))
											.write()
											.toString();

		String content = IOUtils.toString(	this.getClass()
												.getResourceAsStream("/example.fasta"),
											StandardCharsets.UTF_8);
		assertEquals(content.replaceAll("[\r]*[\n]+", ""), parsedContent.replaceAll("[\r]*[\n]+", ""));

	}

}
