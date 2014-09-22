package edu.unc.mapseq.executor.jdr.scratch;

import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDRScratchWorkflowExecutorService {

    private final Logger logger = LoggerFactory.getLogger(JDRScratchWorkflowExecutorService.class);

    private final Timer mainTimer = new Timer();

    private JDRScratchWorkflowExecutorTask task;

    private Long period = Long.valueOf(5);

    public JDRScratchWorkflowExecutorService() {
        super();
    }

    public void start() throws Exception {
        logger.info("ENTERING start()");
        long delay = 1 * 60 * 1000; // 1 minute
        mainTimer.scheduleAtFixedRate(task, delay, period * 60 * 1000);
    }

    public void stop() throws Exception {
        logger.info("ENTERING stop()");
        mainTimer.purge();
        mainTimer.cancel();
    }

    public JDRScratchWorkflowExecutorTask getTask() {
        return task;
    }

    public void setTask(JDRScratchWorkflowExecutorTask task) {
        this.task = task;
    }

    public Long getPeriod() {
        return period;
    }

    public void setPeriod(Long period) {
        this.period = period;
    }

}
