package org.xbib.net.socket.v6.ping;

import org.xbib.net.socket.NetworkUnreachableException;
import org.xbib.net.socket.v6.datagram.DatagramPacket;
import org.xbib.net.socket.v6.datagram.DatagramSocket;
import org.xbib.net.socket.v6.icmp.EchoPacket;
import org.xbib.net.socket.v6.icmp.Packet;

import java.net.Inet6Address;
import java.nio.ByteBuffer;

class PingRequest extends EchoPacket {
    
    public PingRequest() {
        super(64);
        setType(Packet.Type.EchoRequest);
        setCode(0);
    }
    
    public PingRequest(int id, int seqNum) {
        super(64);
        setType(Type.EchoRequest);
        setCode(0);
        setIdentifier(id);
        setSequenceNumber(seqNum);
        ByteBuffer buf = getContentBuffer();
        for(int b = 0; b < 56; b++) {
            buf.put((byte)b);
        }
    }

    @Override
    public DatagramPacket toDatagramPacket(Inet6Address destinationAddress) {
        ByteBuffer contentBuffer = getContentBuffer();
        contentBuffer.putLong(Ping.PING_COOKIE);
        contentBuffer.putLong(System.nanoTime());
        return super.toDatagramPacket(destinationAddress);
    }

    public int send(DatagramSocket socket, Inet6Address addr) throws NetworkUnreachableException {
        return socket.send(toDatagramPacket(addr));
    }
}
