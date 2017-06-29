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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.omnaest.genetics.fasta.utils.StreamModifier.UnaryStreamModifier;

/**
 * @see StreamModifier
 * @author Omnaest
 */
public class StreamUtils
{
	public static <T> Predicate<T> filterFirstPredicate(Consumer<T> consumer)
	{
		return new Predicate<T>()
		{
			private volatile boolean first = true;

			@Override
			public boolean test(T t)
			{
				if (this.first)
				{
					consumer.accept(t);
					this.first = false;
					return false;
				} else
				{
					return true;
				}
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <T> Stream<T> concat(Collection<Stream<T>> streams)
	{
		return streams != null ? concat(streams.toArray(new Stream[streams.size()])) : null;
	}

	@SafeVarargs
	public static <T> Stream<T> concat(Stream<T>... streams)
	{
		Stream<T> retstream = null;
		if (streams != null)
		{
			if (streams.length >= 1)
			{
				retstream = streams[0];
			}
			if (streams.length >= 2)
			{
				for (int ii = 1; ii < streams.length; ii++)
				{
					retstream = Stream.concat(retstream, streams[ii]);
				}
			}
		}
		return retstream;
	}

	public static <C> Predicate<List<C>> matchAllPredicate()
	{
		return t -> true;
	}

	/**
	 * Applies a given {@link StreamModifier} to the given {@link Stream} and returns the resulting {@link Stream}.<br>
	 * <br>
	 * If the given {@link StreamModifier} is null, then the stream is returned unmodified
	 *
	 * @param stream
	 * @param modifier
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <F, T> Stream<T> modify(Stream<F> stream, StreamModifier<F, T> modifier)
	{
		return modifier != null ? modifier.modify(stream) : (Stream<T>) stream;
	}

	public static <E> Stream<E> generateStreamFromIterator(Iterator<E> iterator)
	{
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}

	/**
	 * Returns a {@link Stream} on the given {@link BlockingQueue#take()} method
	 *
	 * @param queue
	 * @throws RuntimeException
	 *             for any {@link InterruptedException}
	 * @return
	 */
	public static <E> Stream<E> generateStreamFromBlockingQueue(BlockingQueue<E> queue)
	{
		AtomicBoolean isClosed = new AtomicBoolean(false);
		return generateStreamFromIterator(new Iterator<E>()
		{
			@Override
			public boolean hasNext()
			{
				return !isClosed.get();
			}

			@Override
			public E next()
			{
				try
				{
					return queue.take();
				} catch (InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
		}).onClose(() ->
		{
			isClosed.set(true);
		});
	}

	/**
	 * Representation of multiple {@link Stream}s
	 *
	 * @author Omnaest
	 * @param <E>
	 */
	public static interface UnaryMultiStream<E>
	{
		List<Stream<E>> streams();
	}

	/**
	 * Representation of two {@link Stream}s with a different signature
	 *
	 * @author Omnaest
	 * @param <E1>
	 * @param <E2>
	 */
	public static interface BiStream<E1, E2>
	{
		Stream<E1> first();

		Stream<E2> second();
	}

	/**
	 * Represents two {@link Stream}s with the same signature
	 *
	 * @author Omnaest
	 * @param <E>
	 */
	public static interface UnaryBiStream<E> extends BiStream<E, E>
	{
	}

	/**
	 * Similar to {@link #clone(Stream, int, int)} with an unlimited buffer
	 *
	 * @see #clone(Stream)
	 * @see #clone(Stream, int, int)
	 * @param stream
	 * @param number
	 * @return
	 */
	public static <E> UnaryMultiStream<E> clone(Stream<E> stream, int number)
	{
		int bufferLimit = Integer.MAX_VALUE;
		return clone(stream, number, bufferLimit);
	}

	/**
	 * Clones the given {@link Stream} into a {@link UnaryMultiStream} with a buffer which is limited to the given buffer limit. <br>
	 * If the buffer limit is reached the {@link Stream} blocks like described in {@link BlockingQueue#take()}
	 *
	 * @see #clone(Stream)
	 * @see #clone(Stream, int)
	 * @param stream
	 * @param number
	 * @param bufferLimit
	 * @return {@link UnaryMultiStream}
	 */
	public static <E> UnaryMultiStream<E> clone(Stream<E> stream, int number, int bufferLimit)
	{
		Iterator<E> iterator = stream.iterator();

		List<BlockingQueue<E>> queues = new ArrayList<>();
		for (int ii = 0; ii < number; ii++)
		{
			queues.add(new LinkedBlockingQueue<>(bufferLimit));
		}

		return new UnaryMultiStream<E>()
		{
			{
				//write inital element into the queues
				this.consumeNextElement();
			}

			@Override
			public List<Stream<E>> streams()
			{
				List<Stream<E>> streams = new ArrayList<>();
				for (BlockingQueue<E> queue : queues)
				{
					Stream<E> streamFromQueue = generateStreamFromBlockingQueue(queue);
					streams.add(streamFromQueue.peek(e ->
					{
						this.consumeNextElement();
						if (this.determineEndOfSource() && queue.isEmpty())
						{
							streamFromQueue.close();
						}
					}));
				}
				return streams;
			}

			private void consumeNextElement()
			{
				if (!this.determineEndOfSource())
				{
					E element = iterator.next();
					for (BlockingQueue<E> queue : queues)
					{
						queue.add(element);
					}

				}

			}

			private boolean determineEndOfSource()
			{
				return !iterator.hasNext();
			}
		};
	}

	/**
	 * Similar to {@link #clone(Stream, int)} with an {@link UnaryBiStream} returned
	 *
	 * @see #clone(Stream, int)
	 * @param stream
	 * @return
	 */
	public static <E> UnaryBiStream<E> clone(Stream<E> stream)
	{
		UnaryMultiStream<E> unaryMultiStream = clone(stream, 2);
		return new UnaryBiStream<E>()
		{
			@Override
			public Stream<E> first()
			{
				return unaryMultiStream	.streams()
										.get(0);
			}

			@Override
			public Stream<E> second()
			{
				return unaryMultiStream	.streams()
										.get(1);
			}
		};

	}

	public static interface UnaryMultiElement<E>
	{
		List<E> elements();
	}

	public static interface BiElement<E1, E2>
	{
		E1 first();

		E2 second();
	}

	public static interface TriElement<E1, E2, E3> extends BiElement<E1, E2>
	{
		E3 third();
	}

	public static interface UnaryBiElement<E> extends BiElement<E, E>
	{
	}

	public static interface UnaryTriElement<E> extends TriElement<E, E, E>
	{

	}

	/**
	 * Similar to
	 *
	 * @param source
	 * @param leftSideModifier
	 * @param middleModifier
	 * @param rightSideModifier
	 * @return
	 */
	public static <S, I> Stream<UnaryTriElement<I>> cloneForkAndJoinUnary(	Stream<S> source, StreamModifier<S, I> leftSideModifier,
																			StreamModifier<S, I> middleModifier, StreamModifier<S, I> rightSideModifier)
	{
		return cloneForkAndJoin(source, leftSideModifier, middleModifier,
								rightSideModifier).map((Function<TriElement<I, I, I>, UnaryTriElement<I>>) triElement -> new UnaryTriElement<I>()
								{
									@Override
									public I third()
									{
										return triElement.third();
									}

									@Override
									public I first()
									{
										return triElement.first();
									}

									@Override
									public I second()
									{
										return triElement.second();
									}
								});
	}

	/**
	 * Similar to {@link #cloneForkAndJoin(Stream, StreamModifier, StreamModifier)} but produces a {@link TriElement}
	 *
	 * @param source
	 * @param leftSideModifier
	 * @param middleModifier
	 * @param rightSideModifier
	 * @return
	 */
	public static <S, I1, I2, I3> Stream<TriElement<I1, I2, I3>> cloneForkAndJoin(	Stream<S> source, StreamModifier<S, I1> leftSideModifier,
																					StreamModifier<S, I2> middleModifier,
																					StreamModifier<S, I3> rightSideModifier)
	{
		return StreamUtils	.cloneForkAndJoin(source, leftSideModifier, stream -> StreamUtils.cloneForkAndJoin(stream, middleModifier, rightSideModifier))
							.map((Function<BiElement<I1, BiElement<I2, I3>>, TriElement<I1, I2, I3>>) (BiElement<I1, BiElement<I2, I3>> be) -> new TriElement<I1, I2, I3>()
							{
								@Override
								public I1 first()
								{
									return be.first();
								}

								@Override
								public I2 second()
								{
									return be	.second()
												.first();
								}

								@Override
								public I3 third()
								{
									return be	.second()
												.second();
								}
							});
	}

	public static <S, I1, I2> Stream<BiElement<I1, I2>> cloneForkAndJoin(	Stream<S> source, StreamModifier<S, I1> leftSideModifier,
																			StreamModifier<S, I2> rightSideModifier)
	{
		UnaryBiStream<S> unaryBiStream = clone(source);
		Stream<I1> modifiedStreamLeft = leftSideModifier.modify(unaryBiStream.first());
		Stream<I2> modifiedStreamRight = rightSideModifier.modify(unaryBiStream.second());

		Iterator<I1> iteratorLeft = modifiedStreamLeft.iterator();
		Iterator<I2> iteratorRight = modifiedStreamRight.iterator();

		return generateStreamFromIterator(new Iterator<BiElement<I1, I2>>()
		{
			@Override
			public BiElement<I1, I2> next()
			{
				if (this.hasAnyRemainingSourceElement(iteratorLeft, iteratorRight))
				{
					I1 first = iteratorLeft.hasNext() ? iteratorLeft.next() : null;
					I2 second = iteratorRight.hasNext() ? iteratorRight.next() : null;
					return new BiElement<I1, I2>()
					{
						@Override
						public I1 first()
						{
							return first;
						}

						@Override
						public I2 second()
						{
							return second;
						}

						@Override
						public String toString()
						{
							return "[" + first + "," + second + "]";
						}

					};
				} else
				{
					return null;
				}
			}

			private boolean hasAnyRemainingSourceElement(Iterator<?> iteratorLeft, Iterator<?> iteratorRight)
			{
				return iteratorLeft.hasNext() || iteratorRight.hasNext();
			}

			@Override
			public boolean hasNext()
			{
				return this.hasAnyRemainingSourceElement(iteratorLeft, iteratorRight);
			}
		});

	}

	public static <E> UnaryStreamModifier<E> concat(UnaryStreamModifier<E> firstModifier, UnaryStreamModifier<E> secondModifier)
	{
		UnaryStreamModifier<E> retval = null;
		if (firstModifier != null && secondModifier == null)
		{
			retval = stream -> firstModifier.modify(stream);
		} else if (firstModifier != null && secondModifier != null)
		{
			retval = stream -> secondModifier.modify(firstModifier.modify(stream));
		} else if (firstModifier == null && secondModifier != null)
		{
			retval = stream -> secondModifier.modify(stream);
		} else
		{
			retval = stream -> stream;
		}
		return retval;
	}

	public static <F, I, T> StreamModifier<F, T> concat(StreamModifier<F, I> firstModifier, StreamModifier<I, T> secondModifier)
	{
		assert (firstModifier != null);
		assert (secondModifier != null);
		return stream -> secondModifier.modify(firstModifier.modify(stream));
	}

}
