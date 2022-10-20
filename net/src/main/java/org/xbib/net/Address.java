package org.xbib.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface Address {

    String getHost();

    Integer getPort();

    InetAddress getInetAddress() throws IOException;

    InetSocketAddress getInetSocketAddress() throws IOException;

    URL base();

    boolean isSecure();

    SocketConfig getSocketConfig();
}
