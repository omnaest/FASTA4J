# FASTA4J
FASTA file format utils

## Example

		long sequenceLength = FastaUtils.load()
										.fromGZIP(new File("/chr21.fa.gz"))
										.getSequence()
										.count();
		assertEquals(48129895, sequenceLength);
		
		
# Maven Snapshots

    <dependency>
      <groupId>org.omnaest.genomics</groupId>
      <artifactId>FASTA4J</artifactId>
      <version>0.0.1-SNAPSHOT</version>
    </dependency>
    
    <repositories>
        <repository>
            <id>ossrh</id>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>