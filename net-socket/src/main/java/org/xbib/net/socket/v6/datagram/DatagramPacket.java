package org.xbib.net.socket.v6.datagram;

import org.xbib.net.socket.v6.Addressable;

import java.net.Inet6Address;
import java.nio.ByteBuffer;

public class DatagramPacket implements Addressable {

    private final ByteBuffer byteBuffer;

    private Addressable addressable;
    
    public DatagramPacket(int size) {
        this(ByteBuffer.allocate(size));
    }
    
    public DatagramPacket(byte[] data) {
        this(ByteBuffer.wrap(data));
    }

    public DatagramPacket(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public void setAddressable(Addressable addressable) {
        this.addressable = addressable;
    }

    public Addressable getAddressable() {
        return addressable;
    }

    public void setAddress(Inet6Address inetAddress, int port) {
        this.addressable = new Addressable() {
            @Override
            public Inet6Address getAddress() {
                return inetAddress;
            }

            @Override
            public int getPort() {
                return port;
            }
        };
    }

    public Inet6Address getAddress() {
        return addressable != null ? addressable.getAddress() : null;
    }

    public int getPort() {
        return addressable != null ? addressable.getPort() : -1;
    }

    public int getLength() {
        return byteBuffer.limit();
    }

    public void setLength(int length) {
        byteBuffer.limit(length);
    }

    public ByteBuffer getContent() {
        return byteBuffer.duplicate();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Address: ");
        if (addressable != null) {
            buf.append(addressable.getAddress());
        }
        buf.append(" Port: ");
        if (addressable != null) {
            buf.append(addressable.getPort());
        }
        buf.append("\nData: ");
        ByteBuffer data = byteBuffer.duplicate();
        buf.append(data.limit());
        buf.append(" Bytes\n");
        int bytesPerRow = 256;
        int limit = data.limit();
        int rows = (limit + bytesPerRow) / bytesPerRow;
        int index = 0;
        for(int i = 0; i < rows && index < limit; i++) {
            for(int j = 0; j < bytesPerRow && index < limit; j++) {
                buf.append(String.format("%02X", data.get(index++)));
            }
            buf.append("\n");
        }
        buf.append("\n");
        return buf.toString();
    }
}
