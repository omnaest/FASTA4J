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

import java.util.stream.Stream;

/**
 * @see StreamUtils
 * @see UnaryStreamModifier
 * @author Omnaest
 * @param <F>
 */
public interface StreamModifier<F, T>
{
	public Stream<T> modify(Stream<F> stream);

	/**
	 * @see StreamModifier
	 * @author Omnaest
	 * @param <E>
	 */
	public static interface UnaryStreamModifier<E> extends StreamModifier<E, E>
	{

	}

	/**
	 * @return a new {@link UnaryStreamModifier} which does not alter the given {@link Stream}
	 */
	public static <E> UnaryStreamModifier<E> UNMODIFYING()
	{
		return stream -> stream;
	}
}
