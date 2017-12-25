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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.omnaest.genetics.fasta.FastaUtils.CodeAndMeta;
import org.omnaest.genetics.fasta.domain.FASTAData;
import org.omnaest.genetics.translator.domain.NucleicAcidCodeSequence;
import org.omnaest.utils.ThreadUtils;

public class FastaUtilsTest
{

	@Test
	@Ignore
	public void testLoad() throws Exception
	{
		FASTAData fastaData = FastaUtils.load()
										.fromGZIP(this	.getClass()
														.getResourceAsStream("/chr21.fa.gz"));
		char[] sequence = fastaData.asCharacters();
		long sequenceLength = sequence.length;
		assertEquals(48129895, sequenceLength);
	}

	@Test
	@Ignore
	public void testLoadGZIPExample() throws Exception
	{
		FASTAData fastaData = FastaUtils.load()
										.fromGZIP(this	.getClass()
														.getResourceAsStream("/chr6.placed.scaf.fa.gz"));
		List<CodeAndMeta> sequence = fastaData	.getSequence()
												.collect(Collectors.toList());
		long sequenceLength = sequence.size();
		assertEquals(4622290, sequenceLength);

		//		System.out.println(sequence	.stream()
		//									.skip(0)
		//									.limit(1000)
		//									.map(cam -> "" + cam.getCode())
		//									.collect(Collectors.joining()));

		String expected = "GAATTCAGCTCGCCGACGGCTGCCTCTGGACTGAGGAGCACTTCATTTGAGTTAGTATTGCGGGAGAAAA";

		assertEquals(expected, sequence	.stream()
										.limit(expected.length())
										.map(cam -> cam	.asTranslatableCode()
														.asNucleicAcidCode()
														.name())
										.collect(Collectors.joining()));
	}

	@Test
	@Ignore
	public void testLoadOBSCN() throws Exception
	{
		FASTAData fastaData = FastaUtils.load()
										.from(new File("C:/Temp/genome/genes4_primary/OBSCN"));
		String sequence = fastaData	.asNucleicAcidCodeSequence()
									.toString();
		System.out.println(sequence.length());
		System.out.println(sequence);
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

	@Test
	public void testFromRawSequence() throws IOException
	{
		char[] rawSequence = FastaUtils	.load()
										.from(this	.getClass()
													.getResourceAsStream("/example.fasta"))
										.asCharacters();

		char[] secondRawSequence = FastaUtils	.load()
												.fromRawSequence(rawSequence)
												.asCharacters();
		assertArrayEquals(rawSequence, secondRawSequence);
	}

	/**
	 * Performance test with chromosome 1 showed a normal consumption of about 1GB memory, after the compression about 280MB, which is a reduction by factor 3
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore("Manual test with large memory consumption")
	public void testLoadChromosome() throws Exception
	{
		FASTAData fastaData = FastaUtils.load()
										.fromGZIP(new File("C:\\Z\\databases\\genomes_reference\\hg19\\Primary_Assembly\\assembled_chromosomes\\FASTA\\chr1.fa.gz"));
		NucleicAcidCodeSequence nucleicAcidCodeSequence = fastaData.asNucleicAcidCodeSequence();

		System.gc();
		ThreadUtils.sleepSilently(10, TimeUnit.SECONDS);

		nucleicAcidCodeSequence.usingInMemoryCompression();

		System.gc();
		ThreadUtils.sleepSilently(10, TimeUnit.SECONDS);
	}

}
