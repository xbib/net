package org.xbib.net.socket.v6.icmp;

import java.nio.ByteBuffer;

public class EchoPacket extends Packet {
    
    public EchoPacket(int size) {
        super(size);
    }

    public EchoPacket(Packet icmpPacket) {
        super(icmpPacket);
    }
    
    public ByteBuffer getContentBuffer() {
        ByteBuffer content = byteBuffer.duplicate();
        content.position(8);
        return content.slice();
    }
    
    public byte[] toBytes() {
        return getContentBuffer().array();
    }
    
    public int getIdentifier() {
        return getUnsignedShort(4);
    }
    
    public void setIdentifier(int id) {
        setUnsignedShort(4, id);
    }
    
    public int getSequenceNumber() {
        return getUnsignedShort(6);
    }
    
    public void setSequenceNumber(int sn) {
        setUnsignedShort(6, sn);
    }

}
