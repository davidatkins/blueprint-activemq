package io.fabric8.mq;

import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class MqPropertySource extends PropertySource {

    public MqPropertySource() {
        super("MqPropertySource");
    }

    @Override
    public Object getProperty(final String name) {

        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                String result = System.getenv(name);
                if(StringUtils.isEmpty(result)) {
                    result = System.getProperty(String.format("org.apache.activemq.%s",name));
                }
                return result;
            }
        });

    }

}