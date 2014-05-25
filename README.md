exome_variant_calling_pipeline
==============================

An exome variant calling pipeline

Command:
bpipe run pipeline.groovy *.fastq.gz


Requirements:

Software:
FASTQC 0.10.1
BWA 0.7.5a
Samtools 0.1.19
Picard 1.96
GATK 2.8-1

Reference data:
GATK reference bundle

Note: Tested with the above versions, but may work with other versions.


Pipeline tool: Bpipe code.google.com/p/bpipe/

Pipeline overview:
* Check reads (FastQC)
* Align reads to reference genome (BWA mem)
* Improve alignment (Picard, GATK)
* Call variants (GATK)
