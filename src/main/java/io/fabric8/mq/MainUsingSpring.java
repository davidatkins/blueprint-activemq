package io.fabric8.mq;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class MainUsingSpring {

    private static Logger LOG = LoggerFactory.getLogger(MainUsingSpring.class);

    public static void main(String[] args) throws Exception {

        MqPropertySource propertySource = new MqPropertySource();
        Object brokerConfig = propertySource.getProperty("AMQ_BROKER_CONFIG");
        if(brokerConfig == null || brokerConfig.toString().isEmpty()) {
            brokerConfig = "broker.xml";
        }

        LOG.info("Using broker config '{}'",brokerConfig);

        BrokerService broker = BrokerFactory.createBroker(String.format("xbean:%s",brokerConfig));
        broker.start();
        waitUntilStop();
    }

    protected static void waitUntilStop() {
        Object lock = new Object();
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

}
