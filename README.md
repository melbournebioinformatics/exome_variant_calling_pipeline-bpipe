exome_variant_calling_pipeline
==============================

An exome variant calling pipeline

Pipeline tool: Bpipe code.google.com/p/bpipe/

Pipeline overview:
* Align reads to reference genome (BWA mem)
* Improve alignment (Picard, GATK)
* Call variants (GATK)
* Annotate variants (VEP)


