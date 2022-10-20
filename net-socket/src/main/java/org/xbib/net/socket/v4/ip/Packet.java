package org.xbib.net.socket.v4.ip;

import org.xbib.net.socket.v4.datagram.DatagramPacket;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Packet {
    
    public enum Protocol {
        ICMP(1),
        TCP(6),
        UDP(17),
        V6_OVER_V4(41);
        private final int m_code;
        Protocol(int code) {
            m_code = code;
        }
        
        public int getCode() {
            return m_code;
        }
        
        public static Protocol toProtocol(int code) {
            for(Protocol p : Protocol.values()) {
                if (code == p.getCode()) {
                    return p;
                }
            }
            throw new IllegalArgumentException(String.format("Unabled to find Protocol with code %d", code));
        }
        
    }
    
    private final ByteBuffer m_buffer;
    
    public Packet(Packet p) {
        this(p.m_buffer.duplicate());
    }
    
    public Packet(ByteBuffer buffer) {
        m_buffer = buffer;
    }
    
    public Packet(byte[] data, int offset, int length) {
        this(ByteBuffer.wrap(data, offset, length).slice());
    }

    public Packet(DatagramPacket datagram) {
        this(datagram.getContent());
    }
    
    public int getVersion() {
        return ((m_buffer.get(0) & 0xf0) >> 4);
    }
    
    public int getHeaderLength() {
        return (m_buffer.get(0) & 0xf) << 2; // shift effectively does * 4 (4 bytes per 32 bit word)
    }
    
    private InetAddress getAddrAtOffset(int offset) {
        byte[] addr = new byte[4];
        int oldPos = m_buffer.position();
        try {
            m_buffer.position(offset);
            m_buffer.get(addr);
        } finally {
            m_buffer.position(oldPos);
        }
        InetAddress result = null;
        try {
            result = InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            // this can't happen
        }
        return result;
    }
    
    public int getTimeToLive() {
        return 0xff & m_buffer.get(8);
    }
    
    public Protocol getProtocol() {
        return Protocol.toProtocol(m_buffer.get(9));
    }
    
    public InetAddress getSourceAddress() {
        return getAddrAtOffset(12);
    }
    
    public InetAddress getDestinationAddress() {
        return getAddrAtOffset(16);
    }
    
    public ByteBuffer getPayload() {
        ByteBuffer data = m_buffer.duplicate();
        data.position(getHeaderLength());
        return data.slice();
    }
    public int getPayloadLength() {
        return getPayload().remaining();
    }
    
}
