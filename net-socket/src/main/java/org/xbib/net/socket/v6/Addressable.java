package org.xbib.net.socket.v6;

import java.net.Inet6Address;

public interface Addressable {

    Inet6Address getAddress();

    int getPort();
}
