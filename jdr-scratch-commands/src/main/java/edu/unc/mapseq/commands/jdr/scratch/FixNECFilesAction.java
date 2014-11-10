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

@Command(scope = "jdr-scratch", name = "fix-nec-files", description = "Fix NEC files")
public class FixNECFilesAction extends AbstractAction {

    private final Logger logger = LoggerFactory.getLogger(FixNECFilesAction.class);

    @Option(name = "--sampleId", description = "sampleId", required = false, multiValued = false)
    private Long sampleId;

    @Option(name = "--flowcellId", description = "flowcellId", required = false, multiValued = false)
    private Long flowcellId;

    private MaPSeqDAOBean maPSeqDAOBean;

    public FixNECFilesAction() {
        super();
    }

    @Override
    public Object doExecute() {

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

                File necVariantCallingOutputDirectory = new File(sample.getOutputDirectory(), "NECVariantCalling");
                if (!necVariantCallingOutputDirectory.exists()) {
                    necVariantCallingOutputDirectory.mkdirs();
                }

                File necIDCheckOutputDirectory = new File(sample.getOutputDirectory(), "NECIDCheck");
                if (!necIDCheckOutputDirectory.exists()) {
                    necIDCheckOutputDirectory.mkdirs();
                }

                // work through managed files first
                for (FileData fd : sampleFileDatas) {

                    File srcFile = new File(fd.getPath(), fd.getName());

                    if (!srcFile.exists()) {
                        logger.error("file doesn't exist: {}", srcFile.getAbsolutePath());
                        continue;
                    }

                    if (fd.getPath().equals(necOutputDirectory.getAbsolutePath())) {
                        if (fd.getName().endsWith(".fixed-rg.bam") || fd.getName().endsWith(".fixed-rg.bai")
                                || fd.getName().endsWith(".fastqc.zip") || fd.getName().endsWith(".vcf.hdr")) {
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
                            fd.setPath(necAlignmentOutputDirectory.getAbsolutePath());
                            fileDataDAO.save(fd);
                        }
                    }

                    if (fd.getPath().equals(necOutputDirectory.getAbsolutePath())) {
                        if (fd.getName().contains(".fixed-rg.deduped.")) {
                            if (fd.getName().endsWith("ec.tsv") || fd.getName().endsWith(".fvcf")
                                    || fd.getName().endsWith(".sub.vcf")) {
                                continue;
                            }
                            File destFile = new File(necVariantCallingOutputDirectory, srcFile.getName());
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
                            fd.setPath(necVariantCallingOutputDirectory.getAbsolutePath());
                            fileDataDAO.save(fd);
                        }
                    }

                    if (fd.getPath().equals(necOutputDirectory.getAbsolutePath())) {
                        if (fd.getName().contains(".fixed-rg.deduped.")) {
                            if (fd.getName().endsWith("ec.tsv") || fd.getName().endsWith(".fvcf")
                                    || fd.getName().endsWith(".sub.vcf")) {
                                File destFile = new File(necIDCheckOutputDirectory, srcFile.getName());
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
                                fd.setPath(necIDCheckOutputDirectory.getAbsolutePath());
                                fileDataDAO.save(fd);
                            }
                        }
                    }

                }

                // the rest are unmanaged, so no need to update FileData paths

                for (File srcFile : necOutputDirectory.listFiles()) {
                    if (srcFile.getName().endsWith(".fixed-rg.bam") || srcFile.getName().endsWith(".fixed-rg.bai")
                            || srcFile.getName().endsWith(".fastqc.zip") || srcFile.getName().endsWith(".vcf.hdr")) {
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
                }

                for (File srcFile : necOutputDirectory.listFiles()) {
                    if (srcFile.getName().contains(".fixed-rg.deduped.")) {
                        if (srcFile.getName().endsWith("ec.tsv") || srcFile.getName().endsWith(".fvcf")
                                || srcFile.getName().endsWith(".sub.vcf")) {
                            continue;
                        }

                        File destFile = new File(necVariantCallingOutputDirectory, srcFile.getName());
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
                }

                for (File srcFile : necOutputDirectory.listFiles()) {
                    if (srcFile.getName().contains(".fixed-rg.deduped.")) {
                        if (srcFile.getName().endsWith("ec.tsv") || srcFile.getName().endsWith(".fvcf")
                                || srcFile.getName().endsWith(".sub.vcf")) {
                            continue;
                        }
                        File destFile = new File(necVariantCallingOutputDirectory, srcFile.getName());
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
                }

                for (File srcFile : necOutputDirectory.listFiles()) {
                    if (srcFile.getName().contains(".fixed-rg.deduped.")) {
                        if (srcFile.getName().endsWith("ec.tsv") || srcFile.getName().endsWith(".fvcf")
                                || srcFile.getName().endsWith(".sub.vcf")) {
                            File destFile = new File(necIDCheckOutputDirectory, srcFile.getName());
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
                    }
                }

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
