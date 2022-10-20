package org.xbib.net.socket.v6.bsd;

import static org.xbib.net.socket.v6.Constants.AF_INET6;
import com.sun.jna.Structure;
import org.xbib.net.socket.v6.Addressable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class SocketStructure extends Structure implements Addressable {

    public byte sin6_len;

    public byte sin6_family;

    public byte[] sin6_port;

    public byte[] sin6_flowinfo;

    public byte[] sin6_addr;

    public byte[] sin6_scope_id;

    public SocketStructure() {
        this(0);
    }

    public SocketStructure(int port) {
        this(null, port);
    }

    public SocketStructure(Inet6Address address, int port) {
        this(AF_INET6, address, port);
    }

    public SocketStructure(int family, Inet6Address address, int port) {
        sin6_family = (byte) (0xff & family);
        sin6_scope_id = new byte[4];
        sin6_len = (byte) (0xff & 16);
        sin6_flowinfo = new byte[4];
        setAddress(address);
        setPort(port);
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("sin6_len", "sin6_family", "sin6_port", "sin6_flowinfo", "sin6_addr", "sin6_scope_id");
    }

    public Inet6Address getAddress() {
        try {
            return (Inet6Address) InetAddress.getByAddress(sin6_addr);
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    public void setAddress(Inet6Address address) {
        if (address != null) {
            byte[] addr = address.getAddress();
            assertLen("address", addr, 16);
            sin6_addr = addr;
        } else {
            sin6_addr = new byte[] { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };
        }
    }

    public int getPort() {
        int port = 0;
        for (int i = 0; i < 2; i++) {
            port = ((port << 8) | (sin6_port[i] & 0xff));
        }
        return port;
    }

    public void setPort(int port) {
        byte[] p = new byte[]{(byte) (0xff & (port >> 8)), (byte) (0xff & port)};
        assertLen("port", p, 2);
        sin6_port = p;
    }

    private void assertLen(String field, byte[] addr, int len) {
        if (addr.length != len) {
            throw new IllegalArgumentException(field + " length must be " + len + " bytes but was " + addr.length + " bytes.");
        }
    }

}
