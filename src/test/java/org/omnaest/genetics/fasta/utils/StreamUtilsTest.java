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
package org.omnaest.genetics.fasta.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class StreamUtilsTest
{

	@Test
	public void testCloneForkAndJoin() throws Exception
	{
		Stream<Character> source = Arrays	.asList('a', 'b', 'c', 'd')
											.stream()
											.peek(System.out::println);
		String collectedCharacters = StreamUtils.cloneForkAndJoin(	source, stream -> stream.peek(System.out::println),
																	stream -> stream.peek(System.out::println))
												.peek(System.out::println)
												.map(be -> "" + be.first() + be.second())
												.collect(Collectors.joining());
		System.out.println(collectedCharacters);
		assertEquals("aabbccdd", collectedCharacters);
	}

}
