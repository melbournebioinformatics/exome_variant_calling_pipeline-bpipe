// vim: ts=4:sw=4:expandtab:cindent:number
//
// bpipe stages for an exome variant calling pipeline
// Authors: Simon Sadedin, MCRI
//          Harriet Dashnow, VLSCI


////////////////////////////////////////
// You will probably need to edit these:
////////////////////////////////////////
BASE="/vlsci/VR0320/shared/production"
TARGET="$BASE/designs/nextera_rapid_capture_exome_1.2/target_regions.bed"
// For temporary files *** required?
TMPDIR="/scratch/VR0002"
// Sequencing platform
PLATFORM="illumina"
// Where you installed GATK
GATK="gatk"
// Set location of you reference files 
REFBASE="hg19"
REF="$REFBASE/gatk.ucsc.hg19.fasta"
DBSNP="$REFBASE/dbsnp_132.hg19.vcf"
GOLD_STANDARD_INDELS="$REFBASE/Mills_and_1000G_gold_standard.indels.b37.chr.vcf"
////////////////////////////////////////


// Parse filenames
def parse_filename(filename) {
    info = filename.split("/")[-1].split("\\.")[0].split("_")
    sample = info[0]
    id = info[1]
    return([id, sample])
}

def get_id(filename) {
    info = filename.split("/")[-1].split("\\.")[0].split("_")
    sample = info[0]
    id = info[1]
    return(id)
}

def get_sample(filename) {
    info = filename.split("/")[-1].split("\\.")[0].split("_")
    sample = info[0]
    id = info[1]
    return(sample)
}

// Pipeline stages

fastqc = {
    doc "Run FASTQC on raw reads"
    output.dir = "fastqc"
    transform('.fastq.gz')  to('_fastqc.zip') {
        exec "fastqc -o ${output.dir} $inputs.gz"
    }
}

align_bwa = {
    transform("bam") {
        doc "Align reads to reference using BWA mem"
        output.dir = "align"
        var seed_length : 19

        def ID = get_id(input)
        def SAMPLE = get_sample(input)

            //  Note: alignments with flag 0x100 filtered 
            //  because they cause problem in GATK and Picard.
    //            -R "@RG\\tID:1\\tPL:illumina\\tPU:${info.unit}\\tLB:$info.unit\\tSM:${info.sample}"
    //        set -o pipefail
        exec """
            bwa mem -M -t $threads -k $seed_length 
                -R "@RG\\tID:${ID}\\tPL:$PLATFORM\\tPU:None\\tLB:None\\tSM:${SAMPLE}"  
                $REF $input1.gz $input2.gz | 
                samtools view -F 0x100 -bSu - | samtools sort - ${output.prefix}
        ""","bwamem"
    }
}

index_bam = {

    doc "Create BAM file index"
    
    //make sure index is in same folder as bam
    output.dir=file(input.bam).absoluteFile.parentFile.absolutePath 
    transform("bam") to ("bam.bai") {
        exec "samtools index $input.bam","index_bam"
    }
    forward input
}

realignIntervals = {
    doc "Select regions for realignment with GATK in 'realign' stage"
    output.dir="align"
    exec """
        java -Xmx4g -jar $GATK/GenomeAnalysisTK.jar 
            -T RealignerTargetCreator 
            -R $REF 
            -I $input.bam 
            --known $GOLD_STANDARD_INDELS 
            -o $output.intervals
    """, "realign_target_creator"
}

realign = {
    doc "Apply GATK local realignment to indevals selected in stage 'realignIntervals' "
    output.dir="align"
    exec """
        java -Xmx5g -jar $GATK/GenomeAnalysisTK.jar 
             -T IndelRealigner 
             -R $REF 
             -I $input.bam 
             -targetIntervals $input.intervals 
             -o $output.bam
    ""","local_realign"
}

dedup = {
    doc "Remove PCR duplicate reads from BAM"
    output.dir="align"
    exec """
        MarkDuplicates
             TMP_DIR=$TMPDIR
             INPUT=$input.bam 
             REMOVE_DUPLICATES=true 
             VALIDATION_STRINGENCY=LENIENT 
             AS=true 
             METRICS_FILE=$output.metrics
             OUTPUT=$output.bam
    ""","MarkDuplicates"
}

recal_count = {
    doc "Recalibrate base qualities in a BAM file using observed error rates"
    output.dir="align"
    INDEL_QUALS=""
    // To use lite version of GATK uncomment below
    // INDEL_QUALS="--disable_indel_quals"

    exec """
        java -Xmx5g -jar $GATK/GenomeAnalysisTK.jar 
             -T BaseRecalibrator 
             -I $input.bam 
             -R $REF 
             --knownSites $DBSNP $INDEL_QUALS
             -l INFO 
             -cov ReadGroupCovariate -cov QualityScoreCovariate -cov CycleCovariate -cov ContextCovariate 
             -o $output.counts
    """, "recalibrate_bam"
}

recal = {
    doc "Apply recalibration quality adjustments"
    output.dir="align"
    exec """
        java -Xmx4g -jar $GATK/GenomeAnalysisTK.jar 
           -T PrintReads 
           -I $input.bam 
           -BQSR $input.counts 
           -R $REF 
           -l INFO 
           -o $output.bam
        """, "recalibrate_bam"
}

call_variants = {
    doc "Call SNPs/SNVs using GATK Unified Genotyper"
    output.dir="variants"

    // Default values from Broad
    var call_conf:5.0, 
        emit_conf:5.0

    transform("bam","bam") to("metrics","vcf") {
        exec """
            java -Xmx8g -jar $GATK/GenomeAnalysisTK.jar 
                   -T UnifiedGenotyper 
                   -R $REF 
                   -I ${inputs.bam.withFlag("-I")} 
                   -nt 4
                   --dbsnp $DBSNP 
                   -stand_call_conf $call_conf -stand_emit_conf $emit_conf
                   -dcov 1600 
                   -l INFO 
                   -L $TARGET
                   -A AlleleBalance -A Coverage -A FisherStrand 
                   -glm BOTH
                   -metrics $output.metrics
                   -o $output.vcf
            ""","gatk_call_variants"
    }
}


