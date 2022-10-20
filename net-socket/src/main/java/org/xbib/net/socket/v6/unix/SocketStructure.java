package org.xbib.net.socket.v6.unix;

import com.sun.jna.Structure;
import org.xbib.net.socket.v6.Addressable;

import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static org.xbib.net.socket.v6.Constants.AF_INET6;

public class SocketStructure extends Structure implements Addressable {

    public short sin6_family;

    public byte[] sin6_port;

    public int sin6_flowinfo;

    public byte[] sin6_addr;

    public int sin6_scope_id;

    public SocketStructure() {
        this(null, 0);
    }

    public SocketStructure(int port) {
        this(null, port);
    }

    public SocketStructure(Inet6Address address, int port) {
        this(AF_INET6, address, port);
    }

    public SocketStructure(int family, Inet6Address address, int port) {
        this.sin6_family = (short) (0xffff & family);
        this.sin6_flowinfo = 0;
        this.sin6_scope_id = 0;
        setAddress(address);
        setPort(port);
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("sin6_family", "sin6_port", "sin6_flowinfo", "sin6_addr", "sin6_scope_id");
    }

    @Override
    public Inet6Address getAddress() {
        try {
            return (Inet6Address) Inet6Address.getByAddress(sin6_addr);
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    public void setAddress(Inet6Address address) {
        if (address != null) {
            byte[] addr = address.getAddress();
            assertLen("address", addr, 16);
            this.sin6_addr = addr;
        } else {
            this.sin6_addr = new byte[] { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };
        }
    }

    @Override
    public int getPort() {
        int port = 0;
        for (int i = 0; i < 2; i++) {
            port = ((port << 8) | (sin6_port[i] & 0xff));
        }
        return port;
    }

    public void setPort(int port) {
        if (port >= 0) {
            byte[] p = new byte[]{(byte) (0xff & (port >> 8)), (byte) (0xff & port)};
            assertLen("port", p, 2);
            this.sin6_port = p;
        }
    }


    private void assertLen(String field, byte[] addr, int len) {
        if (addr.length != len) {
            throw new IllegalArgumentException(field + " length must be " + len + " bytes but was " + addr.length + " bytes.");
        }
    }
}
