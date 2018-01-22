/*
 * Copyright 2017 Danny Kunz Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package org.omnaest.genetics.fasta;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.omnaest.genetics.fasta.domain.Code;
import org.omnaest.genetics.fasta.domain.DefaultCode;
import org.omnaest.genetics.fasta.domain.FASTAData;
import org.omnaest.genetics.fasta.domain.FASTADataWriter;
import org.omnaest.genetics.translator.TranslatableCode;
import org.omnaest.genetics.translator.domain.AminoAcidCodeSequence;
import org.omnaest.genetics.translator.domain.NucleicAcidCodeSequence;

/**
 * Utils to read and write FASTA file format
 * 
 * @see #load()
 * @author omnaest
 */
public class FastaUtils
{

    private static final String LINE_BREAK         = "\n";
    private static final String PREFIX_COMMENT     = ";";
    private static final String PREFIX_DESCRIPTION = ">";

    private static class FASTADataImpl implements FASTAData
    {
        private Stream<CodeAndMeta> sequence;

        public FASTADataImpl(Stream<CodeAndMeta> sequence)
        {
            this.sequence = sequence;
        }

        @Override
        public Stream<CodeAndMeta> getSequence()
        {
            return this.sequence;
        }

        @Override
        public NucleicAcidCodeSequence asNucleicAcidCodeSequence()
        {
            return NucleicAcidCodeSequence.valueOf(this.getSequence()
                                                       .map(cam -> cam.asTranslatableCode()
                                                                      .asNucleicAcidCode()));
        }

        @Override
        public AminoAcidCodeSequence asAminoAcidCodeSequence()
        {
            return AminoAcidCodeSequence.valueOf(this.getSequence()
                                                     .map(cam -> cam.asTranslatableCode()
                                                                    .asAminoAcidCode()));
        }

        @Override
        public char[] asCharacters()
        {
            return ArrayUtils.toPrimitive(this.getSequence()
                                              .map(cam -> cam.getRawCode())
                                              .toArray(Character[]::new));
        }

        @Override
        public String asString()
        {
            return String.valueOf(this.asCharacters());
        }

        @Override
        public String toString()
        {
            return this.asString();
        }

        @Override
        public Code[] asCodes()
        {
            return this.getSequence()
                       .map(cam -> cam.asCode())
                       .toArray(Code[]::new);
        }

        @Override
        public FASTADataWriter write()
        {
            return new FASTADataWriter()
            {

                @Override
                public void to(File file) throws IOException
                {
                    FileUtils.forceMkdirParent(file);
                    Writer writer = new FileWriterWithEncoding(file, StandardCharsets.UTF_8);
                    this.to(writer);
                }

                @Override
                public void toGZIP(File file) throws IOException
                {
                    FileUtils.forceMkdirParent(file);
                    this.to(new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file))), StandardCharsets.UTF_8));
                }

                @Override
                public void to(Writer writer) throws IOException
                {
                    writeTo(FASTADataImpl.this.getSequence(), writer);
                }

                @Override
                public String toString()
                {
                    StringWriter writer = new StringWriter();
                    try
                    {
                        this.to(writer);
                    }
                    catch (IOException e)
                    {
                        throw new IllegalStateException(e);
                    }
                    return writer.toString();
                }

            };
        }
    }

    public static interface FASTADataLoader
    {
        /**
         * Similar to {@link #from(File, Charset)} using {@link StandardCharsets#UTF_8}
         * 
         * @param file
         * @return
         * @throws IOException
         */
        FASTAData from(File file) throws IOException;

        /**
         * Similar to {@link #fromGZIP(File, Charset)} with {@link StandardCharsets#UTF_8}
         * 
         * @param file
         * @return
         * @throws IOException
         */
        FASTAData fromGZIP(File file) throws IOException;

        /**
         * Similar to {@link #from(File)} or {@link #fromGZIP(File)}. The decission wether to use gzip or not is dependent on the presence of the '.gz' file
         * ending
         * 
         * @param file
         * @return
         * @throws IOException
         */
        FASTAData fromGZipIfSuffixPresent(File file) throws IOException;

        /**
         * Loads from a GZIP file
         * 
         * @see StandardCharsets
         * @param file
         * @param charset
         * @return
         * @throws IOException
         */
        FASTAData fromGZIP(File file, Charset charset) throws IOException;

        /**
         * Loads from a {@link File}
         * 
         * @see StandardCharsets
         * @param file
         * @param charset
         * @return
         * @throws IOException
         */
        FASTAData from(File file, Charset charset) throws IOException;

        /**
         * Similar to {@link #fromGZIP(InputStream, Charset)} using {@link StandardCharsets#UTF_8}
         * 
         * @param inputStream
         * @return
         * @throws IOException
         */
        FASTAData fromGZIP(InputStream inputStream) throws IOException;

        /**
         * Loads from a GZIP {@link InputStream}
         * 
         * @see StandardCharsets
         * @param inputStream
         * @param charset
         * @return
         * @throws IOException
         */
        FASTAData fromGZIP(InputStream inputStream, Charset charset) throws IOException;

        /**
         * Similar to {@link #from(InputStream, Charset)} using {@link StandardCharsets#UTF_8}
         * 
         * @param inputStream
         * @return
         * @throws IOException
         */
        FASTAData from(InputStream inputStream) throws IOException;

        /**
         * Loads from a given {@link InputStream} and {@link Charset}
         * 
         * @see StandardCharsets
         * @param inputStream
         * @param charset
         * @return
         * @throws IOException
         */
        FASTAData from(InputStream inputStream, Charset charset) throws IOException;

        /**
         * Loads the FASTA data from a {@link Reader}
         * 
         * @param reader
         * @return
         * @throws IOException
         */
        FASTAData from(Reader reader) throws IOException;

        FASTAData fromSequence(Stream<CodeAndMeta> sequence);

        FASTAData fromCodeSequence(Stream<Code> sequence);

        FASTAData fromRawSequence(char[] sequence);

        FASTAData fromRawSequence(Stream<Character> sequence);
    }

    public static FASTADataLoader load()
    {
        return new FASTADataLoader()
        {

            @Override
            public FASTAData from(File file) throws IOException
            {
                return this.from(file, StandardCharsets.UTF_8);
            }

            @Override
            public FASTAData from(File file, Charset charset) throws IOException
            {
                return this.from(new FileInputStream(file), charset);
            }

            @Override
            public FASTAData fromGZIP(File file) throws IOException
            {
                return this.fromGZIP(file, StandardCharsets.UTF_8);
            }

            @Override
            public FASTAData fromGZipIfSuffixPresent(File file) throws IOException
            {
                return file.getName()
                           .endsWith(".gz") ? this.fromGZIP(file) : this.from(file);
            }

            @Override
            public FASTAData fromGZIP(File file, Charset charset) throws IOException
            {
                return this.fromGZIP(new FileInputStream(file), charset);
            }

            @Override
            public FASTAData fromGZIP(InputStream inputStream) throws IOException
            {
                return this.fromGZIP(inputStream, StandardCharsets.UTF_8);
            }

            @Override
            public FASTAData fromGZIP(InputStream inputStream, Charset charset) throws IOException
            {
                return this.from(new GZIPInputStream(new BufferedInputStream(inputStream)), charset);
            }

            @Override
            public FASTAData from(InputStream inputStream) throws IOException
            {
                return this.from(inputStream, StandardCharsets.UTF_8);
            }

            @Override
            public FASTAData from(InputStream inputStream, Charset charset) throws IOException
            {
                return this.from(new InputStreamReader(new BufferedInputStream(inputStream), charset));
            }

            @SuppressWarnings("deprecation")
            @Override
            public FASTAData from(Reader reader) throws IOException
            {
                Stream<CodeAndMeta> retval;
                Stream<String> lineStream = null;
                try
                {
                    LineIterator it = IOUtils.lineIterator(reader);
                    lineStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);

                    lineStream.onClose(() ->
                    {
                        IOUtils.closeQuietly(reader);
                    });

                    AtomicBoolean readingMetaData = new AtomicBoolean(false);
                    CodeAndMetaFactory codeAndMeta = new CodeAndMetaFactory();
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
                                codeAndMeta.addDescription(StringUtils.removeStart(line, PREFIX_DESCRIPTION));
                            }
                            else if (isCommentLine)
                            {
                                codeAndMeta.addComment(StringUtils.removeStart(line, PREFIX_COMMENT));
                            }
                        }
                        else if (StringUtils.isBlank(line))
                        {
                            //ignore blank lines
                        }
                        else
                        {
                            //
                            if (readingMetaData.get())
                            {
                                codeAndMeta.setCommentChanged(true);
                                codeAndMeta.setDescriptionChanged(true);
                                readingMetaData.set(false);
                            }

                            //remove asterix from end if present
                            codeLine = line;// StringUtils.removeEnd(line, "*");
                        }

                        AtomicBoolean firstCode = new AtomicBoolean(true);
                        return Arrays.asList(ArrayUtils.toObject(codeLine.toCharArray()))
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
                                         return codeAndMeta.createInstance();
                                     });
                    });
                }
                catch (Exception e)
                {
                    throw new IOException("Failed to read fasta source", e);
                }

                return new FASTADataImpl(retval);
            }

            @Override
            public FASTAData fromSequence(Stream<CodeAndMeta> sequence)
            {
                return new FASTADataImpl(sequence);
            }

            @Override
            public FASTAData fromCodeSequence(Stream<Code> sequence)
            {
                return this.fromSequence(sequence.map(code -> new CodeAndMetaImpl(code, new EmptyMetaImpl())));
            }

            @Override
            public FASTAData fromRawSequence(Stream<Character> sequence)
            {
                AtomicLong readPosition = new AtomicLong();
                return this.fromCodeSequence(sequence.map(codeReference -> (Code) new DefaultCode(codeReference, readPosition.getAndIncrement())));
            }

            @Override
            public FASTAData fromRawSequence(char[] sequence)
            {
                return this.fromRawSequence(Arrays.asList(ArrayUtils.toObject(sequence))
                                                  .stream());
            }

        };
    }

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
        }
        catch (IOException e)
        {
            throw new IOException("Failed to read FASTA source", e);
        }
        return retval;
    }

    private static class EmptyMetaImpl extends MetaImpl
    {

        public EmptyMetaImpl()
        {
            super(Collections.emptyList(), false, Collections.emptyList(), false);
        }

    }

    private static class MetaImpl implements Meta
    {
        private List<String> descriptions;
        private boolean      descriptionChanged;
        private List<String> comments;
        private boolean      commentsChanged;

        public MetaImpl(List<String> descriptions, boolean descriptionChanged, List<String> comments, boolean commentsChanged)
        {
            super();
            this.descriptions = descriptions;
            this.descriptionChanged = descriptionChanged;
            this.comments = comments;
            this.commentsChanged = commentsChanged;
        }

        @Override
        public List<String> getDescriptions()
        {
            return this.descriptions;
        }

        @Override
        public List<String> getComments()
        {
            return this.comments;
        }

        @Override
        public boolean hasDescriptionChanged()
        {
            return this.descriptionChanged;
        }

        @Override
        public boolean hasCommentChanged()
        {
            return this.commentsChanged;
        }

        @Override
        public String toString()
        {
            return "MetaImpl [descriptions=" + this.descriptions + ", descriptionChanged=" + this.descriptionChanged + ", comments=" + this.comments
                    + ", commentsChanged=" + this.commentsChanged + "]";
        }

    }

    private static class CodeAndMetaImpl implements CodeAndMeta
    {
        private Code code;
        private Meta meta;

        public CodeAndMetaImpl(Code code, Meta meta)
        {
            super();
            this.code = code;
            this.meta = meta;
        }

        @Override
        public Code asCode()
        {
            return this.code;
        }

        @Override
        public Code newInstanceWithReplacedCode(char rawCode)
        {
            return this.code.newInstanceWithReplacedCode(rawCode);
        }

        @Override
        public char getRawCode()
        {
            return this.code.getRawCode();
        }

        @Override
        public TranslatableCode asTranslatableCode()
        {
            return this.code.asTranslatableCode();
        }

        @Override
        public long getReadPosition()
        {
            return this.code.getReadPosition();
        }

        @Override
        public List<String> getDescriptions()
        {
            return this.meta.getDescriptions();
        }

        @Override
        public boolean hasDescriptionChanged()
        {
            return this.meta.hasDescriptionChanged();
        }

        @Override
        public List<String> getComments()
        {
            return this.meta.getComments();
        }

        @Override
        public boolean hasCommentChanged()
        {
            return this.meta.hasCommentChanged();
        }

        @Override
        public String toString()
        {
            return "CodeAndMetaImpl [code=" + this.code + ", meta=" + this.meta + "]";
        }

    }

    private static final class CodeAndMetaFactory
    {
        private AtomicReference<List<String>> descriptions       = new AtomicReference<>(new ArrayList<>());
        private AtomicBoolean                 descriptionChanged = new AtomicBoolean(false);
        private AtomicReference<List<String>> comments           = new AtomicReference<>(new ArrayList<>());
        private AtomicBoolean                 commentsChanged    = new AtomicBoolean(false);
        private AtomicReference<Character>    codeReference      = new AtomicReference<>(null);
        private AtomicLong                    readPosition       = new AtomicLong();

        public void setCodeReference(Character code)
        {
            this.codeReference.set(code);
        }

        public void addComment(String comment)
        {
            if (StringUtils.isNotBlank(comment))
            {
                this.comments.get()
                             .add(comment);
            }
        }

        public CodeAndMetaFactory addDescription(String description)
        {
            if (StringUtils.isNotBlank(description))
            {
                this.descriptions.get()
                                 .add(description);
            }
            return this;
        }

        public CodeAndMeta createInstance()
        {
            return new CodeAndMetaImpl(new DefaultCode(this.codeReference.get(), this.readPosition.get()),
                                       new MetaImpl(this.descriptions.get(), this.descriptionChanged.get(), this.comments.get(), this.commentsChanged.get()));
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
            return "CodeAndMetaFactory [descriptions=" + this.descriptions + ", descriptionChanged=" + this.descriptionChanged + ", comments=" + this.comments
                    + ", commentsChanged=" + this.commentsChanged + ", codeReference=" + this.codeReference + ", readPosition=" + this.readPosition + "]";
        }

        public void incrementReadPosition()
        {
            this.readPosition.incrementAndGet();
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

    public static interface Meta
    {

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
    }

    public static interface CodeAndMeta extends Code, Meta
    {
        public Code asCode();
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
        }
        catch (Exception e)
        {
            throw new IOException("Failed to read FASTA source: " + fileName, e);
        }
        return retval;
    }

    public static Stream<CodeAndMeta> loadToStream(Reader input) throws IOException
    {
        return load().from(input)
                     .getSequence();
    }

    public static void writeTo(Stream<CodeAndMeta> codeSequence, String fileName) throws IOException
    {
        File file = new File(fileName);
        try (OutputStream outputStream = new FileOutputStream(file))
        {
            Writer writer = new OutputStreamWriter(IOUtils.buffer(outputStream), StandardCharsets.UTF_8);
            writeTo(codeSequence, writer);
        }
        catch (Exception e)
        {
            throw new IOException("Failed to create writer for file " + fileName, e);
        }
    }

    private static void writeTo(Stream<CodeAndMeta> codeSequence, Writer writer) throws IOException
    {
        if (codeSequence != null)
        {
            final int columnsMax = 80;
            AtomicInteger columnCounter = new AtomicInteger(0);

            try
            {
                codeSequence.forEach(codeAndMeta ->
                {
                    try
                    {

                        if (codeAndMeta.hasDescriptionChanged())
                        {
                            for (String description : codeAndMeta.getDescriptions())
                            {
                                writer.write(LINE_BREAK + LINE_BREAK + PREFIX_DESCRIPTION + description + LINE_BREAK);
                            }
                        }
                        if (codeAndMeta.hasCommentChanged())
                        {
                            String comments = codeAndMeta.getComments()
                                                         .stream()
                                                         .collect(Collectors.joining(LINE_BREAK + PREFIX_COMMENT));
                            if (!StringUtils.isBlank(comments))
                            {
                                writer.write(LINE_BREAK + PREFIX_COMMENT + comments + LINE_BREAK);
                            }
                        }

                        writer.write(codeAndMeta.getRawCode());

                        int columnCount = columnCounter.incrementAndGet();
                        if (columnCount % columnsMax == 0)
                        {
                            writer.write(LINE_BREAK);
                        }

                    }
                    catch (Exception e)
                    {
                        throw new IllegalStateException(e);
                    }
                });
            }
            catch (Exception e)
            {
                if (e.getCause() instanceof IOException)
                {
                    throw (IOException) e.getCause();
                }
            }

            finally
            {
                writer.close();
            }
        }
    }
}
