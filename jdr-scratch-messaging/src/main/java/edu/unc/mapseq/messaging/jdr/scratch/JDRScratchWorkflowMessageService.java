package edu.unc.mapseq.messaging.jdr.scratch;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDRScratchWorkflowMessageService {

    private final Logger logger = LoggerFactory.getLogger(JDRScratchWorkflowMessageService.class);

    private Connection connection;

    private Session session;

    private ConnectionFactory connectionFactory;

    private JDRScratchWorkflowMessageListener messageListener;

    private String destinationName;

    public JDRScratchWorkflowMessageService() {
        super();
    }

    public void start() throws Exception {
        logger.debug("ENTERING start()");
        this.connection = connectionFactory.createConnection();
        this.session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = this.session.createQueue(this.destinationName);
        MessageConsumer consumer = this.session.createConsumer(destination);
        consumer.setMessageListener(getMessageListener());
        this.connection.start();
    }

    public void stop() throws Exception {
        logger.debug("ENTERING stop()");
        if (this.session != null) {
            this.session.close();
        }
        if (this.connection != null) {
            this.connection.stop();
            this.connection.close();
        }
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public JDRScratchWorkflowMessageListener getMessageListener() {
        return messageListener;
    }

    public void setMessageListener(JDRScratchWorkflowMessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

}
