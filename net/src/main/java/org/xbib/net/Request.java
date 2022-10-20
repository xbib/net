package org.xbib.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public interface Request {

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

    URL getBaseURL();

    ByteBuffer getBody();

    CharBuffer getBodyAsChars(Charset charset);

    <R extends Request> R as(Class<R> cl);
}
