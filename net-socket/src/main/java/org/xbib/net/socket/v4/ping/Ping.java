package org.xbib.net.socket.v4.ping;

import org.xbib.net.socket.Metric;
import org.xbib.net.socket.NetworkUnreachableException;
import org.xbib.net.socket.v4.SocketFactory;
import org.xbib.net.socket.v4.datagram.DatagramPacket;
import org.xbib.net.socket.v4.datagram.DatagramSocket;
import org.xbib.net.socket.v4.icmp.Packet;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.xbib.net.socket.v4.Constants.IPPROTO_ICMP;

public class Ping implements Runnable, Closeable {

    private static final Logger logger = Logger.getLogger(Ping.class.getName());

    public static final long PING_COOKIE = StandardCharsets.US_ASCII.encode("org.xbib").getLong(0);

    private final DatagramSocket datagramSocket;

    private final List<PingResponseListener> listeners;

    private volatile boolean closed;

    private Thread thread;

    private PingMetric metric;

    public Ping(int id)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this(SocketFactory.createDatagramSocket(IPPROTO_ICMP, id));
    }

    public Ping(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
        this.listeners = new ArrayList<>();
        this.closed = false;
    }

    public Metric getMetric() {
        return metric;
    }

    public boolean isClosed() {
        return closed;
    }

    public void start() {
        thread = new Thread(this, "PingThread:PingListener");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
    }

    @Override
    public void close() throws IOException {
        if (datagramSocket != null) {
            closed = true;
            datagramSocket.close();
        }
    }

    public List<PingResponseListener> getListeners() {
        return listeners;
    }

    public void addPingReplyListener(PingResponseListener listener) {
        listeners.add(listener);
    }

    public void execute(int id, Inet4Address addr)
            throws InterruptedException, NetworkUnreachableException {
        Thread t = new Thread(this);
        t.start();
        execute(id, addr, 1, 10, 1000);
    }

    public void execute(int id,
                        Inet4Address inet4Address,
                        int sequenceNumber,
                        int count,
                        long interval)
            throws InterruptedException, NetworkUnreachableException {
        if (inet4Address == null) {
            return;
        }
        metric = new PingMetric(count, interval);
        addPingReplyListener(metric);
        for(int i = sequenceNumber; i < sequenceNumber + count; i++) {
            PingRequest request = new PingRequest(id, i);
            int rc = request.send(datagramSocket, inet4Address);
            Thread.sleep(interval);
        }
    }

    @Override
    public void run() {
        try {
            DatagramPacket datagram = new DatagramPacket(65535);
            while (!isClosed()) {
                int rc = datagramSocket.receive(datagram);
                long received = System.nanoTime();
                Packet packet = new Packet(getPayload(datagram));
                if (packet.getType() == Packet.Type.EchoReply) {
                    PingResponse pingResponse = new PingResponse(packet, received);
                    if (pingResponse.isValid()) {
                        logger.log(Level.INFO, String.format("%d bytes from %s: tid=%d icmp_seq=%d time=%.3f ms%n",
                                pingResponse.getPacketLength(),
                                datagram.getAddress().getHostAddress(),
                                pingResponse.getIdentifier(),
                                pingResponse.getSequenceNumber(),
                                pingResponse.elapsedTime(TimeUnit.MILLISECONDS)));
                        for (PingResponseListener listener : getListeners()) {
                            listener.onPingResponse(datagram.getAddress(), pingResponse);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private ByteBuffer getPayload(final DatagramPacket datagram) {
        return new org.xbib.net.socket.v4.ip.Packet(datagram.getContent()).getPayload();
    }
}
