package org.xbib.net.socket.v6.datagram;

import org.xbib.net.socket.NetworkUnreachableException;

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;

public interface DatagramSocket extends Closeable {

    int setFragmentation(boolean frag) throws IOException;

    int setTrafficClass(int trafficClass) throws IOException;

    int receive(DatagramPacket datagramPacket) throws UnknownHostException;

    int send(DatagramPacket datagramPacket) throws NetworkUnreachableException;
}
