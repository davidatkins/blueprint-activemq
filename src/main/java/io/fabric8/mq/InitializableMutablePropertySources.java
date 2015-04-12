package io.fabric8.mq;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.Collection;

public class InitializableMutablePropertySources extends MutablePropertySources {

    public InitializableMutablePropertySources(Collection<PropertySource> propertySources) {
        super();
        if (propertySources != null) {
            for (final PropertySource propertySource : propertySources) {
                addLast(propertySource);
            }
        }
    }

}
