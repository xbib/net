package org.xbib.net.socket.v4.unix;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.xbib.net.socket.NetworkUnreachableException;
import org.xbib.net.socket.v4.datagram.DatagramPacket;
import org.xbib.net.socket.v4.datagram.DatagramSocket;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.xbib.net.socket.v4.Constants.AF_INET;
import static org.xbib.net.socket.v4.Constants.IPPROTO_IP;
import static org.xbib.net.socket.v4.Constants.IP_MTU_DISCOVER;
import static org.xbib.net.socket.v4.Constants.IP_TOS;

public class NativeDatagramSocket implements DatagramSocket, AutoCloseable {

    static {
        Native.register((String) null);
    }

    private static final Logger logger = Logger.getLogger(NativeDatagramSocket.class.getName());

    private final int socket;

    private volatile boolean closed;

    public NativeDatagramSocket(int type, int protocol, int port) {
        try {
            this.socket = socket(AF_INET, type, protocol);
            if (socket < 0) {
                throw new IllegalStateException("socket < 0");
            }
            SocketStructure socketStructure = new SocketStructure(port);
            bind(socket, socketStructure, socketStructure.size());
            closed = false;
        } catch (LastErrorException e) {
            logger.log(Level.SEVERE, e.getMessage() + ": check if sysctl -w net.ipv4.ping_group_range=\"0 65535\" and check for selinux security:\n" +
                        "allow unconfined_t node_t:icmp_socket node_bind;\n" +
                        "allow unconfined_t port_t:icmp_socket name_bind;\n");
            throw e;
        }
    }

    public native int bind(int socket, SocketStructure address, int address_len) throws LastErrorException;

    public native int socket(int domain, int type, int protocol) throws LastErrorException;

    public native int setsockopt(int socket, int level, int option_name, Pointer value, int option_len);

    public native int sendto(int socket, Buffer buffer, int buflen, int flags, SocketStructure dest_addr, int dest_addr_len) throws LastErrorException;

    public native int recvfrom(int socket, Buffer buffer, int buflen, int flags, SocketStructure in_addr, int[] in_addr_len) throws LastErrorException;

    public native int close(int socket) throws LastErrorException;

    public native String strerror(int errnum);

    @Override
    public int setTrafficClass(final int tc) throws IOException {
        if (closed) {
            return -1;
        }
        IntByReference ptr = new IntByReference(tc);
        try {
            return setsockopt(socket, IPPROTO_IP, IP_TOS, ptr.getPointer(), Native.POINTER_SIZE);
        } catch (final LastErrorException e) {
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
        } catch (final LastErrorException e) {
            throw new IOException("setsockopt: " + strerror(e.getErrorCode()));
        }
    }

    @Override
    public int receive(DatagramPacket datagramPacket) {
        if (closed) {
            return -1;
        }
        SocketStructure in_addr = new SocketStructure();
        int[] szRef = new int[]{in_addr.size()};
        ByteBuffer buf = datagramPacket.getContent();
        int n = recvfrom(socket, buf, buf.capacity(), 0, in_addr, szRef);
        datagramPacket.setLength(n);
        datagramPacket.setAddressable(in_addr);
        return n;
    }

    @Override
    public int send(DatagramPacket datagramPacket) throws NetworkUnreachableException {
        if (closed) {
            return -1;
        }
        SocketStructure destAddr = new SocketStructure(datagramPacket.getAddress(), datagramPacket.getPort());
        ByteBuffer buf = datagramPacket.getContent();
        try {
            return sendto(socket, buf, buf.remaining(), 0, destAddr, destAddr.size());
        } catch (LastErrorException e) {
            if (e.getMessage().contains("[101]")) {
                throw new NetworkUnreachableException();
            }
            throw e;
        }
    }

    @Override
    public void close() {
        closed = true;
        close(socket);
    }
}
