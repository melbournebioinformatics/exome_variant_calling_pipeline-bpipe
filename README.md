Exome Variant Calling pipeline - Bpipe
======================================

An exome variant calling pipeline written in Bpipe

Usage: bpipe run pipeline.groovy *.fastq.gz

Set up:
------
Edit the locations of installed software, reference data, target region etc. in pipeline_stages_config.groovy

Requirements:
------------

Software:
* FASTQC 0.10.1
* BWA 0.7.5a
* Samtools 0.1.19
* Picard 1.96
* GATK 2.8-1

Reference data:
* GATK reference bundle

Note: Tested with the above versions, but may work with other versions.

Pipeline tool: 
-------------
Bpipe code.google.com/p/bpipe/

Requires a variant of the bpipe script to handle module specification. If this is not used, then the required modules must be loaded in the relevant .bashrc script.

bpipe variant: [slugger70-bpipe](https://code.google.com/r/slugger70-bpipe/)

Pipeline overview:
-----------------
* Check reads (FastQC)
* Align reads to reference genome (BWA mem)
* Improve alignment (Picard, GATK)
* Call variants (GATK)
