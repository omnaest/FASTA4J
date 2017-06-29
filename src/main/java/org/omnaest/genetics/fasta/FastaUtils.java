/*
 * Copyright 2017 Danny Kunz Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package org.omnaest.genetics.fasta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.omnaest.genetics.fasta.translator.TranslatableCode;
import org.omnaest.genetics.fasta.translator.TranslatableCodeImpl;

public class FastaUtils
{

	private static final String	LINE_BREAK			= "\n";
	private static final String	PREFIX_COMMENT		= ";";
	private static final String	PREFIX_DESCRIPTION	= ">";

	public static String loadRawTop(String fileName) throws IOException
	{
		String retval = null;
		File file = new File(fileName);
		try (FileInputStream openInputStream = FileUtils.openInputStream(file))
		{
			InputStreamReader reader = new InputStreamReader(openInputStream, StandardCharsets.UTF_8);
			retval = loadRawTop(reader);
		}
		return retval;
	}

	public static String loadRawTop(Reader input) throws IOException
	{
		String retval = null;
		try
		{
			StringWriter output = new StringWriter();
			long inputOffset = 0;
			long length = 80000000;
			IOUtils.copyLarge(input, output, inputOffset, length);
			retval = output.toString();
		} catch (IOException e)
		{
			throw new IOException("Failed to read FASTA source", e);
		}
		return retval;
	}

	private static final class CodeAndMetaImpl implements CodeAndMeta
	{
		private AtomicReference<List<String>>	descriptions		= new AtomicReference<>(new ArrayList<>());
		private AtomicBoolean					descriptionChanged	= new AtomicBoolean(false);
		private AtomicReference<List<String>>	comments			= new AtomicReference<>(new ArrayList<>());
		private AtomicBoolean					commentsChanged		= new AtomicBoolean(false);
		private AtomicReference<Character>		codeReference		= new AtomicReference<>(null);
		private AtomicLong						readPosition		= new AtomicLong();

		@Override
		public List<String> getDescriptions()
		{
			return this.descriptions.get();
		}

		@Override
		public List<String> getComments()
		{
			return this.comments.get();
		}

		@Override
		public char getCode()
		{
			return this.codeReference.get();
		}

		public void setCodeReference(Character code)
		{
			this.codeReference.set(code);
		}

		public void clearDescriptions()
		{
			this.descriptions.set(new ArrayList<>());
		}

		public void clearComments()
		{
			this.comments.set(new ArrayList<>());
		}

		@Override
		public String toString()
		{
			return "CodeAndMetaImpl [getDescriptions()=" + this.getDescriptions() + ", getComments()=" + this.getComments() + ", getCode()=" + this.getCode()
					+ "]";
		}

		@Override
		public TranslatableCode getTranslatableCode()
		{
			return new TranslatableCodeImpl(this.getCode());
		}

		public void incrementReadPosition()
		{
			this.readPosition.incrementAndGet();
		}

		@Override
		public long getReadPosition()
		{
			return this.readPosition.get();
		}

		@Override
		public boolean hasDescriptionChanged()
		{
			return this.descriptionChanged.get();
		}

		@Override
		public boolean hasCommentChanged()
		{
			return this.commentsChanged.get();
		}

		public void setDescriptionChanged(boolean changed)
		{
			this.descriptionChanged.set(changed);
		}

		public void setCommentChanged(boolean changed)
		{
			this.commentsChanged.set(changed);
		}

		public void resetCommentChanged()
		{
			this.setCommentChanged(false);
		}

		public void resetDescriptionChanged()
		{
			this.setDescriptionChanged(false);
		}
	}

	public static interface CodeAndMeta
	{
		/**
		 * Returns the raw code which has been read for the current position
		 *
		 * @return
		 */
		public char getCode();

		/**
		 * Returns a {@link TranslatableCode} wrapper around the raw {@link #getCode()}
		 *
		 * @return
		 */
		public TranslatableCode getTranslatableCode();

		/**
		 * Returns the last encountered descriptions for the current code read position.<br>
		 * <br>
		 * Example of a description:<br>
		 *
		 * <pre>
		 * >MCHU - Calmodulin - Human, rabbit, bovine, rat, and chicken
		 * </pre>
		 *
		 * @see #hasDescriptionChanged()
		 * @return
		 */
		public List<String> getDescriptions();

		/**
		 * Returns true, if the description has changed during reading this next position. This flag can be used to write out trace information, since mostly
		 * the description contain encoded position information within the genome
		 *
		 * @return
		 */
		public boolean hasDescriptionChanged();

		/**
		 * Returns the last encountered comments for the current read position.<br>
		 * <br>
		 * Example of comments:<br>
		 *
		 * <pre>
		 *  ;LCBO - Prolactin precursor - Bovine
		 *  ; a sample sequence in FASTA format
		 * </pre>
		 *
		 * @return
		 */
		public List<String> getComments();

		/**
		 * Returns true, if the comment has changed during reading this next position.
		 *
		 * @return
		 */
		public boolean hasCommentChanged();

		/**
		 * Returns the read position within the {@link Stream} of codes
		 *
		 * @return
		 */
		long getReadPosition();
	}

	public static Stream<CodeAndMeta> loadToStream(String fileName) throws IOException
	{
		Stream<CodeAndMeta> retval = null;
		File file = new File(fileName);
		try
		{
			FileInputStream openInputStream = FileUtils.openInputStream(file);
			InputStreamReader reader = new InputStreamReader(openInputStream, StandardCharsets.UTF_8);
			retval = loadToStream(reader);
		} catch (Exception e)
		{
			throw new IOException("Failed to read FASTA source: " + fileName, e);
		}
		return retval;
	}

	public static Stream<CodeAndMeta> loadToStream(Reader input) throws IOException
	{
		Stream<CodeAndMeta> retval = null;
		Stream<String> lineStream = null;
		try
		{
			LineIterator it = IOUtils.lineIterator(input);
			lineStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);

			lineStream.onClose(() ->
			{
				IOUtils.closeQuietly(input);
			});

			AtomicBoolean readingMetaData = new AtomicBoolean(false);
			CodeAndMetaImpl codeAndMeta = new CodeAndMetaImpl();
			retval = lineStream.flatMap(line ->
			{
				line = StringUtils.trim(line);

				String codeLine = "";
				boolean isDescriptionLine = StringUtils.startsWith(line, PREFIX_DESCRIPTION);
				boolean isCommentLine = StringUtils.startsWith(line, PREFIX_COMMENT);
				if (isDescriptionLine || isCommentLine)
				{
					if (!readingMetaData.get())
					{
						readingMetaData.set(true);
						codeAndMeta.clearDescriptions();
						codeAndMeta.clearComments();
						codeAndMeta.setCommentChanged(false);
						codeAndMeta.setDescriptionChanged(false);
					}

					if (isDescriptionLine)
					{
						codeAndMeta	.getDescriptions()
									.add(StringUtils.removeStart(line, PREFIX_DESCRIPTION));
					} else if (isCommentLine)
					{
						codeAndMeta	.getComments()
									.add(StringUtils.removeStart(line, PREFIX_COMMENT));
					}
				} else if (StringUtils.isBlank(line))
				{
					//ignore blank lines
				} else
				{
					//
					if (readingMetaData.get())
					{
						codeAndMeta.setCommentChanged(true);
						codeAndMeta.setDescriptionChanged(true);
						readingMetaData.set(false);
					}

					//remove asterix from end if present
					codeLine = StringUtils.removeEnd(line, "*");
				}

				AtomicBoolean firstCode = new AtomicBoolean(true);
				return Arrays	.asList(ArrayUtils.toObject(codeLine.toCharArray()))
								.stream()
								.map(code ->
								{
									if (!firstCode.getAndSet(false))
									{
										codeAndMeta.resetCommentChanged();
										codeAndMeta.resetDescriptionChanged();
									}

									codeAndMeta.incrementReadPosition();
									codeAndMeta.setCodeReference(code);
									return codeAndMeta;
								});
			});
		} catch (Exception e)
		{
			throw new IOException("Failed to read fasta source", e);
		}

		return retval;
	}

	public static void writeTo(Stream<CodeAndMeta> codeSequence, String fileName) throws IOException
	{
		File file = new File(fileName);
		try (OutputStream outputStream = new FileOutputStream(file))
		{
			Writer writer = new OutputStreamWriter(IOUtils.buffer(outputStream), StandardCharsets.UTF_8);
			writeTo(codeSequence, writer);
		} catch (Exception e)
		{
			throw new IOException("Failed to create writer for file " + fileName, e);
		}
	}

	public static void writeTo(Stream<CodeAndMeta> codeSequence, Writer writer)
	{
		if (codeSequence != null)
		{
			final int columnsMax = 80;
			AtomicInteger columnCounter = new AtomicInteger(0);
			codeSequence.forEach(codeAndMeta ->
			{
				try
				{
					if (codeAndMeta.hasDescriptionChanged())
					{
						for (String description : codeAndMeta.getDescriptions())
						{
							writer.write(LINE_BREAK + PREFIX_DESCRIPTION + description + LINE_BREAK);
						}
					}
					if (codeAndMeta.hasCommentChanged())
					{
						for (String comment : codeAndMeta.getComments())
						{
							writer.write(LINE_BREAK + PREFIX_COMMENT + comment + LINE_BREAK);
						}
					}

					writer.write(codeAndMeta.getCode());

					int columnCount = columnCounter.incrementAndGet();
					if (columnCount % columnsMax == 0)
					{
						writer.write(LINE_BREAK);
					}
				} catch (IOException e)
				{
					throw new RuntimeException("Failed to write code sequence to FASTA format", e);
				}
			});
		}
	}
}