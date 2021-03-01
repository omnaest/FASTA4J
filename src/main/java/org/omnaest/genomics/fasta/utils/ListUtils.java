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
package org.omnaest.genomics.fasta.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ListUtils
{
	/**
	 * Returns a new {@link List} instance which has the elements of the given {@link Collection} in reverse order
	 * <br>
	 * <br>
	 * If the given collection is null, an empty {@link List} is returned
	 *
	 * @param collection
	 * @return a new list instance
	 */
	public static <E> List<E> inverse(Collection<E> collection)
	{
		List<E> retlist = new ArrayList<>();
		if (collection != null)
		{
			retlist.addAll(collection);
			Collections.reverse(retlist);
		}
		return retlist;
	}

	public static <E> Optional<E> first(List<E> list)
	{
		return list != null && !list.isEmpty() ? Optional.of(list.get(0)) : Optional.empty();
	}

	public static <E, R> Optional<R> first(List<E> list, Function<E, R> mapper)
	{
		return list != null && !list.isEmpty() ? Optional	.of(list.get(0))
															.map(mapper)
				: Optional.empty();
	}

	public static <E> Optional<E> last(List<E> list)
	{
		return list != null && !list.isEmpty() ? Optional.of(list.get(list.size() - 1)) : Optional.empty();
	}

	public static <E, R> Optional<R> last(List<E> list, Function<E, R> mapper)
	{
		return list != null && !list.isEmpty() ? Optional	.of(list.get(list.size() - 1))
															.map(mapper)
				: Optional.empty();
	}
}
