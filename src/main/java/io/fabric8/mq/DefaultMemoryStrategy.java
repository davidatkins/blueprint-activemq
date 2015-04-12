package io.fabric8.mq;

public class DefaultMemoryStrategy {

    public long getBrokerMemory() {
        long maxMemory = Runtime.getRuntime().maxMemory();
       return (long) (maxMemory * 0.7);
    }

}
