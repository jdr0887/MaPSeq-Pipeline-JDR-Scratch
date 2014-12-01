package edu.unc.mapseq.commands.jdr.scratch;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.mapseq.dao.MaPSeqDAOBean;
import edu.unc.mapseq.dao.MaPSeqDAOException;
import edu.unc.mapseq.dao.SampleDAO;
import edu.unc.mapseq.dao.model.FileData;
import edu.unc.mapseq.dao.model.Sample;

@Command(scope = "jdr-scratch", name = "test-jargon-register", description = "Test Jargon Register")
public class TestJargonRegisterAction extends AbstractAction {

    private final Logger logger = LoggerFactory.getLogger(TestJargonRegisterAction.class);

    @Option(name = "--sampleId", description = "sampleId", required = true, multiValued = false)
    private Long sampleId;

    private MaPSeqDAOBean maPSeqDAOBean;

    public TestJargonRegisterAction() {
        super();
    }

    @Override
    public Object doExecute() {
        logger.debug("ENTERING doExecute()");
        SampleDAO sampleDAO = maPSeqDAOBean.getSampleDAO();

        Set<Sample> samples = new HashSet<Sample>();
        try {
            if (sampleId != null) {
                Sample sample = sampleDAO.findById(sampleId);
                samples.add(sample);
            }
        } catch (MaPSeqDAOException e1) {
            e1.printStackTrace();
        }

        String devIrodsBaseDir = "/genomicsDataGridZone/sequence_data/dev/nec";

        if (samples != null && !samples.isEmpty()) {

            for (Sample sample : samples) {
                
                Set<FileData> sampleFileDatas = sample.getFileDatas();

                File necOutputDirectory = new File(sample.getOutputDirectory(), "NEC");

                File necAlignmentOutputDirectory = new File(sample.getOutputDirectory(), "NECAlignment");
                if (!necAlignmentOutputDirectory.exists()) {
                    necAlignmentOutputDirectory.mkdirs();
                }
                
            }
        }

        return null;
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
