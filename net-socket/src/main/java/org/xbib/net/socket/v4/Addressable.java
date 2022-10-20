package org.xbib.net.socket.v4;

import java.net.Inet4Address;

public interface Addressable {

    Inet4Address getAddress();

    int getPort();
}
