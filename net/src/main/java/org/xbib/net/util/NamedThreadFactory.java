package org.xbib.net.util;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {

    private final String name;

    private long counter = 0;

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, name + "-" + (counter++));
        thread.setDaemon(true);
        return thread;
    }
}
