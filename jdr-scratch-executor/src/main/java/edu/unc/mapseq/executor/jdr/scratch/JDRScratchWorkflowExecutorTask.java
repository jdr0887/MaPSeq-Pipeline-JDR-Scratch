package edu.unc.mapseq.executor.jdr.scratch;

import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.mapseq.dao.MaPSeqDAOException;
import edu.unc.mapseq.dao.WorkflowDAO;
import edu.unc.mapseq.dao.WorkflowRunAttemptDAO;
import edu.unc.mapseq.dao.model.Workflow;
import edu.unc.mapseq.dao.model.WorkflowRunAttempt;
import edu.unc.mapseq.workflow.WorkflowBeanService;
import edu.unc.mapseq.workflow.WorkflowExecutor;
import edu.unc.mapseq.workflow.WorkflowTPE;
import edu.unc.mapseq.workflow.jdr.scratch.JDRScratchWorkflow;

public class JDRScratchWorkflowExecutorTask extends TimerTask {

    private final Logger logger = LoggerFactory.getLogger(JDRScratchWorkflowExecutorTask.class);

    private final WorkflowTPE threadPoolExecutor = new WorkflowTPE();

    private WorkflowBeanService workflowBeanService;

    public JDRScratchWorkflowExecutorTask() {
        super();
    }

    @Override
    public void run() {
        logger.debug("ENTERING run()");

        threadPoolExecutor.setCorePoolSize(workflowBeanService.getCorePoolSize());
        threadPoolExecutor.setMaximumPoolSize(workflowBeanService.getMaxPoolSize());

        logger.info(String.format("CorePoolSize: %d, MaxPoolSize: %d", threadPoolExecutor.getCorePoolSize(),
                threadPoolExecutor.getMaximumPoolSize()));

        logger.info(String.format("ActiveCount: %d, TaskCount: %d, CompletedTaskCount: %d",
                threadPoolExecutor.getActiveCount(), threadPoolExecutor.getTaskCount(),
                threadPoolExecutor.getCompletedTaskCount()));

        WorkflowDAO workflowDAO = getWorkflowBeanService().getMaPSeqDAOBean().getWorkflowDAO();
        WorkflowRunAttemptDAO workflowRunAttemptDAO = getWorkflowBeanService().getMaPSeqDAOBean()
                .getWorkflowRunAttemptDAO();

        try {

            List<Workflow> workflowList = workflowDAO.findByName("JDRScratch");
            if (workflowList == null || (workflowList != null && workflowList.isEmpty())) {
                logger.error("No Workflow Found: {}", "TestVCF");
                return;
            }
            Workflow workflow = workflowList.get(0);
            List<WorkflowRunAttempt> attempts = workflowRunAttemptDAO.findEnqueued(workflow.getId());

            if (attempts != null && !attempts.isEmpty()) {

                logger.info("dequeuing {} WorkflowRunAttempts", attempts.size());
                for (WorkflowRunAttempt attempt : attempts) {

                    JDRScratchWorkflow w = new JDRScratchWorkflow();
                    attempt.setVersion(w.getVersion());
                    attempt.setDequeued(new Date());
                    workflowRunAttemptDAO.save(attempt);

                    w.setWorkflowBeanService(workflowBeanService);
                    w.setWorkflowRunAttempt(attempt);
                    threadPoolExecutor.submit(new WorkflowExecutor(w));

                }

            }

        } catch (MaPSeqDAOException e) {
            e.printStackTrace();
        }

    }

    public WorkflowBeanService getWorkflowBeanService() {
        return workflowBeanService;
    }

    public void setWorkflowBeanService(WorkflowBeanService workflowBeanService) {
        this.workflowBeanService = workflowBeanService;
    }

}
