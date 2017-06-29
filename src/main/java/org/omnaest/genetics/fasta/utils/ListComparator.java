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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ListComparator<T> implements Comparator<List<T>>
{
	private Comparator<T> elementComparator;

	public ListComparator(Comparator<T> elementComparator)
	{
		super();
		this.elementComparator = elementComparator;
	}

	@Override
	public int compare(List<T> list1, List<T> list2)
	{
		int retval = 0;

		if (list1 != null && list2 != null)
		{
			Iterator<T> iterator1 = list1.iterator();
			Iterator<T> iterator2 = list2.iterator();
			while (retval == 0 && (iterator1.hasNext() || iterator2.hasNext()))
			{
				T element1 = iterator1.hasNext() ? iterator1.next() : null;
				T element2 = iterator2.hasNext() ? iterator2.next() : null;
				retval = this.elementComparator.compare(element1, element2);
			}
		}
		return retval;
	}

}
