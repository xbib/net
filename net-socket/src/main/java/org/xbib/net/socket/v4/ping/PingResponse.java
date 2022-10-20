package org.xbib.net.socket.v4.ping;

import org.xbib.net.socket.v4.icmp.EchoPacket;
import org.xbib.net.socket.v4.icmp.Packet;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class PingResponse extends EchoPacket {

    private final long receivedTimeNanos;

    public PingResponse(Packet packet, long receivedTimeNanos) {
        super(packet);
        this.receivedTimeNanos = receivedTimeNanos;
    }
    
    public boolean isValid() {
        ByteBuffer content = getContentBuffer();
        return content.limit() >= 16 && Ping.PING_COOKIE == content.getLong(0);
    }

    public long getSentTimeNanos() {
        return getContentBuffer().getLong(8);
    }
    
    public long getReceivedTimeNanos() {
        return receivedTimeNanos;
    }
    
    public double elapsedTime(TimeUnit unit) {
        double nanosPerUnit = TimeUnit.NANOSECONDS.convert(1, unit);
        return getElapsedTimeNanos() / nanosPerUnit;
    }

    public long getElapsedTimeNanos() {
        return getReceivedTimeNanos() - getSentTimeNanos();
    }
}
