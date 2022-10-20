package org.xbib.net.socket.v6.ping;

import org.xbib.net.socket.Metric;

import java.net.Inet6Address;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PingMetric extends Metric implements PingResponseListener {
    
    private final CountDownLatch countDownLatch;

    private final int count;

    private final long interval;
    
    public PingMetric(int count, long interval) {
        this.countDownLatch = new CountDownLatch(count);
        this.count = count;
        this.interval = interval;
    }

    @Override
    public void onPingResponse(Inet6Address address, PingResponse reply) {
        try {
            update(reply.getElapsedTimeNanos());
        } finally {
            countDownLatch.countDown();
        }
    }

    public void await() throws InterruptedException {
        countDownLatch.await(interval * count + 1000, TimeUnit.MILLISECONDS);
    }
}
