# FASTA4J
FASTA file format utils

##Example

		long sequenceLength = FastaUtils.load()
										.fromGZIP(new File("/chr21.fa.gz"))
										.getSequence()
										.count();
		assertEquals(48129895, sequenceLength);