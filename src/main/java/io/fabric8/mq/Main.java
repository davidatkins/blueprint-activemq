/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.mq;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.leveldb.LevelDBStoreFactory;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the orginal method used to start the broker. At somepoint i'll create a pull request to replace it with 'MainUsingSpring'
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final String DEFAULT_BROKER_NAME = "defaultBroker";
    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final String DEFAULT_PORT_TEXT = "61616";
    public static final String DEFAULT_DATA_DIRECTORY = "data";

    private static String brokerName;
    private static String dataDirectory;
    private static String host;
    private static int port;
    private static Integer mqttPort;

    public static void main(String args[]) {
        try {
            try {
                brokerName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_BROKER_NAME");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_BROKER_NAME", DEFAULT_BROKER_NAME) : result;
                        return result;
                    }
                });
                host = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_HOST");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_HOST", DEFAULT_HOST) : result;
                        return result;
                    }
                });
                String portStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_PORT");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_PORT", DEFAULT_PORT_TEXT) : result;
                        return result;
                    }
                });
                if (portStr != null && portStr.length() > 0) {
                    port = Integer.parseInt(portStr);
                }
                String mqttPortStr = mqttPortString();
                if (mqttPortStr != null && mqttPortStr.length() > 0) {
                    mqttPort = Integer.parseInt(mqttPortStr);
                }
                dataDirectory = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_DATA_DIRECTORY");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_DATA_DIRECTORY", DEFAULT_DATA_DIRECTORY) : result;
                        return result;
                    }
                });

            } catch (Throwable e) {
                LOG.warn("Failed to look up System properties for host and port", e);
            }

            if (host == null || host.length() == 0) {
                host = "0.0.0.0";
            }
            if (port <= 0) {
                port = 61616;
            }
            if (brokerName == null) {
                brokerName = DEFAULT_BROKER_NAME;
            }
            if (dataDirectory == null) {
                dataDirectory = "data";
            }
            BrokerService brokerService = new BrokerService();
            brokerService.setBrokerName(brokerName);
            brokerService.setDataDirectory(dataDirectory);

            //we create our own ManagementContext - so ActiveMQ doesn't create a needless JMX Connector
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ManagementContext managementContext = new ManagementContext(server);
            managementContext.setCreateConnector(false);

            brokerService.setManagementContext(managementContext);

            List<BrokerPlugin> list = new ArrayList<BrokerPlugin>();
            list.add(new StatisticsBrokerPlugin());

            //ToDo - uncomment this when we move to ActiveMQ 5.11
            //list.add(new CamelRoutesBrokerPlugin());
            BrokerPlugin[] plugins = new BrokerPlugin[list.size()];
            list.toArray(plugins);
            brokerService.setPlugins(plugins);

            LevelDBStoreFactory persistenceFactory = new LevelDBStoreFactory();
            persistenceFactory.setDirectory(new File(getDataDirectory()));
            persistenceFactory.setSync(false);
            brokerService.setPersistenceFactory(persistenceFactory);

            //set max available memory to the broker

            long maxMemory = Runtime.getRuntime().maxMemory();
            long brokerMemory = (long) (maxMemory * 0.7);

            brokerService.getSystemUsage().getMemoryUsage().setLimit(brokerMemory);
            String connector = "tcp://" + host + ":" + port;
            System.out.println("Starting broker on " + connector);
            brokerService.addConnector(connector);

            // TODO: can't make this optional via xml? Could do this via java? but need access to spring then, and users can't control it
            if(mqttPort != null) {
                String mqttConnector = "mqtt://" + host + ":" + mqttPort;
                System.out.println("Starting MQTT connector on " + mqttConnector);
                brokerService.addConnector(mqttConnector);
            }

            brokerService.start();

            waitUntilStop();
        } catch (Throwable e) {
            LOG.error("Failed to Start Fabric8MQ", e);
        }
    }

    protected static String mqttPortString() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                String result = System.getenv("AMQ_MQTT_PORT");
                result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_MQTT_PORT") : result;
                return result;
            }
        });
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

    public static String getBrokerName() {
        return brokerName;
    }

    public static String getDataDirectory() {
        return dataDirectory;
    }

    public static int getPort() {
        return port;
    }

    public static Integer getMqttPort() {
        return mqttPort;
    }
}