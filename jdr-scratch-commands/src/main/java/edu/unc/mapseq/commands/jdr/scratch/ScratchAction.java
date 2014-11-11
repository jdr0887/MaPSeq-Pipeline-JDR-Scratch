package edu.unc.mapseq.commands.jdr.scratch;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.mapseq.dao.FileDataDAO;
import edu.unc.mapseq.dao.MaPSeqDAOBean;
import edu.unc.mapseq.dao.MaPSeqDAOException;
import edu.unc.mapseq.dao.SampleDAO;
import edu.unc.mapseq.dao.model.FileData;
import edu.unc.mapseq.dao.model.Sample;

@Command(scope = "jdr-scratch", name = "scratch", description = "Do anything")
public class ScratchAction extends AbstractAction {

    private final Logger logger = LoggerFactory.getLogger(ScratchAction.class);

    @Option(name = "--sampleId", description = "sampleId", required = false, multiValued = false)
    private Long sampleId;

    @Option(name = "--flowcellId", description = "flowcellId", required = false, multiValued = false)
    private Long flowcellId;

    private MaPSeqDAOBean maPSeqDAOBean;

    public ScratchAction() {
        super();
    }

    @Override
    public Object doExecute() {
        logger.info("ENTERING doExecute()");

        SampleDAO sampleDAO = maPSeqDAOBean.getSampleDAO();
        FileDataDAO fileDataDAO = maPSeqDAOBean.getFileDataDAO();

        Set<Sample> samples = new HashSet<Sample>();
        try {
            if (sampleId != null) {
                Sample sample = sampleDAO.findById(sampleId);
                samples.add(sample);
            }

            if (flowcellId != null) {
                List<Sample> sampleList = sampleDAO.findByFlowcellId(flowcellId);
                if (sampleList != null && !sampleList.isEmpty()) {
                    samples.addAll(sampleList);
                }
            }
        } catch (MaPSeqDAOException e1) {
            e1.printStackTrace();
        }

        for (Sample sample : samples) {

            try {

                Set<FileData> sampleFileDatas = sample.getFileDatas();

                File necOutputDirectory = new File(sample.getOutputDirectory(), "NEC");

                File necAlignmentOutputDirectory = new File(sample.getOutputDirectory(), "NECAlignment");
                if (!necAlignmentOutputDirectory.exists()) {
                    necAlignmentOutputDirectory.mkdirs();
                }

                // work through managed files first
                for (FileData fd : sampleFileDatas) {

                    File srcFile = new File(fd.getPath(), fd.getName());

                    if (fd.getPath().equals(necOutputDirectory.getAbsolutePath())) {
                        if (fd.getName().endsWith(".fixed-rg.bam") || fd.getName().endsWith(".fixed-rg.bai")
                                || fd.getName().endsWith(".fastqc.zip") || fd.getName().endsWith(".vcf.hdr")) {
                            if (srcFile.exists()) {
                                File destFile = new File(necAlignmentOutputDirectory, srcFile.getName());
                                if (destFile.exists() && destFile.length() == 0) {
                                    destFile.delete();
                                }
                                if (destFile.exists() && destFile.length() > 0) {
                                    if (destFile.lastModified() > srcFile.lastModified()) {
                                        srcFile.delete();
                                    } else {
                                        destFile.delete();
                                        FileUtils.moveFile(srcFile, destFile);
                                    }
                                    continue;
                                }
                                FileUtils.moveFile(srcFile, destFile);
                            }
                            fd.setPath(necAlignmentOutputDirectory.getAbsolutePath());
                            fileDataDAO.save(fd);
                        }
                    }

                }
                
                FileUtils.deleteDirectory(necOutputDirectory);

            } catch (MaPSeqDAOException | IOException e) {
                e.printStackTrace();
            }

        }

        return null;
    }

    public Long getFlowcellId() {
        return flowcellId;
    }

    public void setFlowcellId(Long flowcellId) {
        this.flowcellId = flowcellId;
    }

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public MaPSeqDAOBean getMaPSeqDAOBean() {
        return maPSeqDAOBean;
    }

    public void setMaPSeqDAOBean(MaPSeqDAOBean maPSeqDAOBean) {
        this.maPSeqDAOBean = maPSeqDAOBean;
    }

}
