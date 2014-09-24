package edu.unc.mapseq.workflow.jdr.scratch;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.renci.jlrm.condor.CondorJob;
import org.renci.jlrm.condor.CondorJobBuilder;
import org.renci.jlrm.condor.CondorJobEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.mapseq.dao.model.Flowcell;
import edu.unc.mapseq.dao.model.Sample;
import edu.unc.mapseq.dao.model.WorkflowRunAttempt;
import edu.unc.mapseq.module.bwa.BWAAlignCLI;
import edu.unc.mapseq.module.bwa.BWASAMPairedEndCLI;
import edu.unc.mapseq.module.fastqc.FastQCCLI;
import edu.unc.mapseq.module.fastqc.IgnoreLevelType;
import edu.unc.mapseq.module.picard.PicardAddOrReplaceReadGroupsCLI;
import edu.unc.mapseq.module.picard.PicardSortOrderType;
import edu.unc.mapseq.module.samtools.SAMToolsIndexCLI;
import edu.unc.mapseq.workflow.WorkflowException;
import edu.unc.mapseq.workflow.impl.AbstractSampleWorkflow;
import edu.unc.mapseq.workflow.impl.WorkflowJobFactory;
import edu.unc.mapseq.workflow.impl.WorkflowUtil;

public class JDRScratchWorkflow extends AbstractSampleWorkflow {

    private final Logger logger = LoggerFactory.getLogger(JDRScratchWorkflow.class);

    public JDRScratchWorkflow() {
        super();
    }

    @Override
    public String getName() {
        return JDRScratchWorkflow.class.getSimpleName().replace("Workflow", "");
    }

    @Override
    public String getVersion() {
        ResourceBundle bundle = ResourceBundle.getBundle("edu/unc/mapseq/workflow/jdr/scratch/workflow");
        String version = bundle.getString("version");
        return StringUtils.isNotEmpty(version) ? version : "0.0.1-SNAPSHOT";
    }

    @Override
    public Graph<CondorJob, CondorJobEdge> createGraph() throws WorkflowException {
        logger.info("ENTERING createGraph()");

        DirectedGraph<CondorJob, CondorJobEdge> graph = new DefaultDirectedGraph<CondorJob, CondorJobEdge>(
                CondorJobEdge.class);

        int count = 0;

        Set<Sample> sampleSet = getAggregatedSamples();
        logger.info("sampleSet.size(): {}", sampleSet.size());

        String siteName = getWorkflowBeanService().getAttributes().get("siteName");
        String referenceSequence = getWorkflowBeanService().getAttributes().get("referenceSequence");
        String readGroupPlatform = getWorkflowBeanService().getAttributes().get("readGroupPlatform");
        String readGroupPlatformUnit = getWorkflowBeanService().getAttributes().get("readGroupPlatformUnit");

        WorkflowRunAttempt attempt = getWorkflowRunAttempt();

        for (Sample sample : sampleSet) {

            if ("Undetermined".equals(sample.getBarcode())) {
                continue;
            }

            Flowcell flowcell = sample.getFlowcell();
            File outputDirectory = new File(sample.getOutputDirectory(), getName());
            File tmpDirectory = new File(outputDirectory, "tmp");
            tmpDirectory.mkdirs();

            List<File> readPairList = WorkflowUtil.getReadPairList(sample.getFileDatas(), flowcell.getName(),
                    sample.getLaneIndex());

            if (readPairList.size() != 2) {
                throw new WorkflowException("ReadPairList is not 2");
            }

            File r1FastqFile = readPairList.get(0);
            String r1FastqRootName = WorkflowUtil.getRootFastqName(r1FastqFile.getName());

            File r2FastqFile = readPairList.get(1);
            String r2FastqRootName = WorkflowUtil.getRootFastqName(r2FastqFile.getName());

            String fastqLaneRootName = StringUtils.removeEnd(r2FastqRootName, "_R2");

            try {

                // new job
                CondorJobBuilder builder = WorkflowJobFactory.createJob(++count, FastQCCLI.class, attempt).siteName(
                        siteName);
                File fastqcR1Output = new File(outputDirectory, r1FastqRootName + ".fastqc.zip");
                builder.addArgument(FastQCCLI.INPUT, r1FastqFile.getAbsolutePath())
                        .addArgument(FastQCCLI.OUTPUT, fastqcR1Output.getAbsolutePath())
                        .addArgument(FastQCCLI.IGNORE, IgnoreLevelType.ERROR.toString());
                CondorJob fastQCR1Job = builder.build();
                logger.info(fastQCR1Job.toString());
                graph.addVertex(fastQCR1Job);

                // new job
                builder = WorkflowJobFactory.createJob(++count, BWAAlignCLI.class, attempt).siteName(siteName)
                        .numberOfProcessors(4);
                File saiR1OutFile = new File(outputDirectory, r1FastqRootName + ".sai");
                builder.addArgument(BWAAlignCLI.THREADS, "4")
                        .addArgument(BWAAlignCLI.FASTQ, r1FastqFile.getAbsolutePath())
                        .addArgument(BWAAlignCLI.FASTADB, referenceSequence)
                        .addArgument(BWAAlignCLI.OUTFILE, saiR1OutFile.getAbsolutePath());
                CondorJob bwaAlignR1Job = builder.build();
                logger.info(bwaAlignR1Job.toString());
                graph.addVertex(bwaAlignR1Job);
                graph.addEdge(fastQCR1Job, bwaAlignR1Job);

                // new job
                builder = WorkflowJobFactory.createJob(++count, FastQCCLI.class, attempt).siteName(siteName);
                File fastqcR2Output = new File(outputDirectory, r2FastqRootName + ".fastqc.zip");
                builder.addArgument(FastQCCLI.INPUT, r2FastqFile.getAbsolutePath())
                        .addArgument(FastQCCLI.OUTPUT, fastqcR2Output.getAbsolutePath())
                        .addArgument(FastQCCLI.IGNORE, IgnoreLevelType.ERROR.toString());
                CondorJob fastQCR2Job = builder.build();
                logger.info(fastQCR2Job.toString());
                graph.addVertex(fastQCR2Job);

                // new job
                builder = WorkflowJobFactory.createJob(++count, BWAAlignCLI.class, attempt).siteName(siteName)
                        .numberOfProcessors(4);
                File saiR2OutFile = new File(outputDirectory, r2FastqRootName + ".sai");
                builder.addArgument(BWAAlignCLI.THREADS, "4")
                        .addArgument(BWAAlignCLI.FASTQ, r2FastqFile.getAbsolutePath())
                        .addArgument(BWAAlignCLI.FASTADB, referenceSequence)
                        .addArgument(BWAAlignCLI.OUTFILE, saiR2OutFile.getAbsolutePath());
                CondorJob bwaAlignR2Job = builder.build();
                logger.info(bwaAlignR2Job.toString());
                graph.addVertex(bwaAlignR2Job);
                graph.addEdge(fastQCR2Job, bwaAlignR2Job);

                // new job
                builder = WorkflowJobFactory.createJob(++count, BWASAMPairedEndCLI.class, attempt).siteName(siteName);
                File bwaSAMPairedEndOutFile = new File(outputDirectory, fastqLaneRootName + ".sam");
                builder.addArgument(BWASAMPairedEndCLI.FASTADB, referenceSequence)
                        .addArgument(BWASAMPairedEndCLI.FASTQ1, r1FastqFile.getAbsolutePath())
                        .addArgument(BWASAMPairedEndCLI.FASTQ2, r2FastqFile.getAbsolutePath())
                        .addArgument(BWASAMPairedEndCLI.SAI1, saiR1OutFile.getAbsolutePath())
                        .addArgument(BWASAMPairedEndCLI.SAI2, saiR2OutFile.getAbsolutePath())
                        .addArgument(BWASAMPairedEndCLI.OUTFILE, bwaSAMPairedEndOutFile.getAbsolutePath());
                CondorJob bwaSAMPairedEndJob = builder.build();
                logger.info(bwaSAMPairedEndJob.toString());
                graph.addVertex(bwaSAMPairedEndJob);
                graph.addEdge(bwaAlignR1Job, bwaSAMPairedEndJob);
                graph.addEdge(bwaAlignR2Job, bwaSAMPairedEndJob);

                // new job
                builder = WorkflowJobFactory.createJob(++count, PicardAddOrReplaceReadGroupsCLI.class, attempt)
                        .siteName(siteName);
                File fixRGOutput = new File(outputDirectory, bwaSAMPairedEndOutFile.getName().replace(".sam",
                        ".fixed-rg.bam"));
                builder.addArgument(PicardAddOrReplaceReadGroupsCLI.INPUT, bwaSAMPairedEndOutFile.getAbsolutePath())
                        .addArgument(PicardAddOrReplaceReadGroupsCLI.OUTPUT, fixRGOutput.getAbsolutePath())
                        .addArgument(PicardAddOrReplaceReadGroupsCLI.SORTORDER,
                                PicardSortOrderType.COORDINATE.toString().toLowerCase())
                        .addArgument(
                                PicardAddOrReplaceReadGroupsCLI.READGROUPID,
                                String.format("%s-%s_L%03d", flowcell.getName(), sample.getBarcode(),
                                        sample.getLaneIndex()))
                        .addArgument(PicardAddOrReplaceReadGroupsCLI.READGROUPLIBRARY, sample.getName())
                        .addArgument(PicardAddOrReplaceReadGroupsCLI.READGROUPPLATFORM, readGroupPlatform)
                        .addArgument(PicardAddOrReplaceReadGroupsCLI.READGROUPPLATFORMUNIT, readGroupPlatformUnit)
                        .addArgument(PicardAddOrReplaceReadGroupsCLI.READGROUPSAMPLENAME, sample.getName())
                        .addArgument(PicardAddOrReplaceReadGroupsCLI.READGROUPCENTERNAME, "UNC");
                CondorJob picardAddOrReplaceReadGroupsJob = builder.build();
                logger.info(picardAddOrReplaceReadGroupsJob.toString());
                graph.addVertex(picardAddOrReplaceReadGroupsJob);
                graph.addEdge(bwaSAMPairedEndJob, picardAddOrReplaceReadGroupsJob);

                // new job
                builder = WorkflowJobFactory.createJob(++count, SAMToolsIndexCLI.class, attempt).siteName(siteName);
                File picardAddOrReplaceReadGroupsIndexOut = new File(outputDirectory, fixRGOutput.getName().replace(
                        ".bam", ".bai"));
                builder.addArgument(SAMToolsIndexCLI.INPUT, fixRGOutput.getAbsolutePath()).addArgument(
                        SAMToolsIndexCLI.OUTPUT, picardAddOrReplaceReadGroupsIndexOut.getAbsolutePath());
                CondorJob picardAddOrReplaceReadGroupsIndexJob = builder.build();
                logger.info(picardAddOrReplaceReadGroupsIndexJob.toString());
                graph.addVertex(picardAddOrReplaceReadGroupsIndexJob);
                graph.addEdge(picardAddOrReplaceReadGroupsJob, picardAddOrReplaceReadGroupsIndexJob);

            } catch (Exception e) {
                throw new WorkflowException(e);
            }

        }

        return graph;
    }

}
