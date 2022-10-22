package org.xbib.net.security.ssl.util;

import org.xbib.net.security.ssl.socket.CompositeSSLServerSocketFactory;
import org.xbib.net.security.ssl.socket.CompositeSSLSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

public final class SSLSocketUtils {

    private SSLSocketUtils() {}

    public static SSLSocketFactory createSslSocketFactory(SSLContext sslContext, SSLParameters sslParameters) {
        return new CompositeSSLSocketFactory(sslContext.getSocketFactory(), sslParameters);
    }

    public static SSLSocketFactory createSslSocketFactory(SSLSocketFactory sslSocketFactory, SSLParameters sslParameters) {
        return new CompositeSSLSocketFactory(sslSocketFactory, sslParameters);
    }

    public static SSLServerSocketFactory createSslServerSocketFactory(SSLContext sslContext, SSLParameters sslParameters) {
        return new CompositeSSLServerSocketFactory(sslContext.getServerSocketFactory(), sslParameters);
    }

    public static SSLServerSocketFactory createSslServerSocketFactory(SSLServerSocketFactory sslServerSocketFactory, SSLParameters sslParameters) {
        return new CompositeSSLServerSocketFactory(sslServerSocketFactory, sslParameters);
    }
}
