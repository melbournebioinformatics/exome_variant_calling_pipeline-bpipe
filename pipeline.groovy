// vim: set ts=4:sw=4:expandtab:cindent
//
// Exome Variant Calling Pipeline
//
// Usage:
//   bpipe run pipeline.groovy reads_1.fastq.gz reads_2.fastq.gz ...
// 
// Authors: Simon Sadedin, MCRI
//          Harriet Dashnow, VLSCI
//
// Assumes illumina paired_end reads

// Load the pipeline stages
load 'pipeline_stages_config.groovy'

run {
    "%.fastq.gz" * [ fastqc ] + 
    "%_*.fastq.gz" * [ 
        align_bwa + index_bam +
        dedup + index_bam + 
        realignIntervals + realign + index_bam +
        recal_count + recal + index_bam
        ] +
            // Call variants from all samples at once
            // VCF will be named for first fastq file
            call_variants
}

