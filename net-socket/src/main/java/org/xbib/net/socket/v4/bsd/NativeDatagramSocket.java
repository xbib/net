package org.xbib.net.socket.v4.bsd;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.xbib.net.socket.v4.datagram.DatagramPacket;
import org.xbib.net.socket.v4.datagram.DatagramSocket;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.xbib.net.socket.v4.Constants.AF_INET;
import static org.xbib.net.socket.v4.Constants.IPPROTO_IP;
import static org.xbib.net.socket.v4.Constants.IP_MTU_DISCOVER;

public class NativeDatagramSocket implements DatagramSocket, AutoCloseable {

    static {
        Native.register((String) null);
    }

    private static final int IP_TOS = 3;

    private final int socket;

    private volatile boolean closed;

    public NativeDatagramSocket(int type, int protocol, int port) {
        this.socket = socket(AF_INET, type, protocol);
        if (socket < 0) {
            throw new IllegalStateException("socket < 0");
        }
        SocketStructure socketStructure = new SocketStructure(port);
        bind(socket, socketStructure, socketStructure.size());
        closed = false;
    }

    public native int bind(int socket, SocketStructure address, int address_len) throws LastErrorException;

    public native int socket(int family, int type, int protocol) throws LastErrorException;

    public native int setsockopt(int socket, int level, int option_name, Pointer value, int len);

    public native int sendto(int socket, Buffer buffer, int buflen, int flags, SocketStructure address, int len) throws LastErrorException;

    public native int recvfrom(int socket, Buffer buffer, int buflen, int flags, SocketStructure address, int[] len) throws LastErrorException;

    public native int close(int socket) throws LastErrorException;

    public native String strerror(int errnum);

    @Override
    public int setTrafficClass(int tc) throws IOException {
        IntByReference ptr = new IntByReference(tc);
        try {
            return setsockopt(socket, IPPROTO_IP, IP_TOS, ptr.getPointer(), Native.POINTER_SIZE);
        } catch (LastErrorException e) {
            throw new IOException("setsockopt: " + strerror(e.getErrorCode()));
        }
    }

    @Override
    public int setFragmentation(boolean frag) throws IOException {
        return allowFragmentation(IPPROTO_IP, IP_MTU_DISCOVER, frag);
    }

    private int allowFragmentation(int level, int option_name, boolean frag) throws IOException {
        if (closed) {
            return -1;
        }
        IntByReference dontfragment = new IntByReference(frag ? 0 : 1);
        try {
            return setsockopt(socket, level, option_name, dontfragment.getPointer(), Native.POINTER_SIZE);
        } catch (LastErrorException e) {
            throw new IOException("setsockopt: " + strerror(e.getErrorCode()));
        }
    }

    @Override
    public int receive(DatagramPacket datagramPacket) {
        if (closed) {
            return -1;
        }
        try {
            SocketStructure socketStructure = new SocketStructure();
            int[] szRef = new int[]{socketStructure.size()};
            ByteBuffer buf = datagramPacket.getContent();
            int n = recvfrom(socket, buf, buf.capacity(), 0, socketStructure, szRef);
            datagramPacket.setLength(n);
            datagramPacket.setAddressable(socketStructure);
            return n;
        } catch (LastErrorException e) {
            if (e.getMessage().contains("[9]")) {
                // bad file descriptor
                return -1;
            }
            throw e;
        }
    }

    @Override
    public int send(DatagramPacket datagramPacket) {
        if (closed) {
            return -1;
        }
        ByteBuffer buf = datagramPacket.getContent();
        SocketStructure socketStructure = new SocketStructure(datagramPacket.getAddress(), datagramPacket.getPort());
        return sendto(socket, buf, buf.remaining(), 0, socketStructure, socketStructure.size());
    }

    @Override
    public void close() {
        closed = true;
        close(socket);
    }
}
