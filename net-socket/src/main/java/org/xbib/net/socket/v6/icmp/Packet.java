package org.xbib.net.socket.v6.icmp;

import org.xbib.net.socket.v6.datagram.DatagramPacket;

import java.net.Inet6Address;
import java.nio.ByteBuffer;

public class Packet {
    
    public static final int CHECKSUM_INDEX = 2;
    
    public enum Type {
        DestinationUnreachable(1),
        TimeExceeded(3),
        EchoRequest(128),
        EchoReply(129),
        Other(-1);

        private final int code;

        Type(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static Type toType(byte typeCode) {
            int code = (typeCode & 0xff);
            for(Type p : Type.values()) {
                if (code == p.getCode()) {
                    return p;
                }
            }
            return Other;
        }
    }

    protected ByteBuffer byteBuffer;
    
    public Packet(ByteBuffer ipPayload) {
        byteBuffer = ipPayload;
    }
    
    public Packet(Packet packet) {
        this(packet.byteBuffer.duplicate());
    }
    
    public Packet(int size) {
        this(ByteBuffer.allocate(size));
    }
    
    public int getPacketLength() {
        return byteBuffer.limit();
    }
    
    public Type getType() {
        return Type.toType(byteBuffer.get(0));
    }
    
    public void setType(Type t) {
        byteBuffer.put(0, ((byte)(t.getCode())));
    }
    
    public int getCode() {
        return 0xff & byteBuffer.get(1);
    }

    public void setCode(int code) {
        byteBuffer.put(1, ((byte)code));
    }
    
    public int getChecksum() {
        return getUnsignedShort(2);
    }
    
    public int computeChecksum() {
        int sum = 0;
        int count = byteBuffer.remaining();
        int index = 0;
        while(count > 1) {
            if (index != CHECKSUM_INDEX) {
                sum += getUnsignedShort(index);
            }
            index += 2;
            count -= 2;
        }
        if (count > 0) {
            sum += makeUnsignedShort(byteBuffer.get((byteBuffer.remaining()-1)), (byte)0);
        }
        int sumLo = sum & 0xffff;
        int sumHi = (sum >> 16) & 0xffff;
        sum = sumLo + sumHi;
        sumLo = sum & 0xffff;
        sumHi = (sum >> 16) & 0xffff;
        sum = sumLo + sumHi;
        return (~sum) & 0xffff;
    }
    
    public void setBytes(int index, byte[] b) {
        ByteBuffer payload = byteBuffer;
        int oldPos = payload.position();
        try {
            payload.position(index);
            payload.put(b);
        } finally {
            payload.position(oldPos);
        }
    }

    public int makeUnsignedShort(byte b1, byte b0) {
        return 0xffff & (((b1 & 0xff) << 8) | ((b0 & 0xff)));
    }

    public int getUnsignedShort(int index) {
        return byteBuffer.getShort(index) & 0xffff;
    }

    public void setUnsignedShort(int index, int us) {
        byteBuffer.putShort(index, ((short)(us & 0xffff)));
    }

    public DatagramPacket toDatagramPacket(Inet6Address destinationAddress) {
        DatagramPacket p = new DatagramPacket(byteBuffer.duplicate());
        p.setAddress(destinationAddress, 0);
        return p;
    }
}
