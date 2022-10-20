package org.xbib.net.security.ssl.socket;

import org.xbib.net.security.ssl.util.SSLSocketUtils;
import org.xbib.net.security.ssl.util.ValidationUtils;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link SSLSocketUtils SSLSocketUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 */
public final class CompositeSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory sslSocketFactory;
    private final SSLParameters sslParameters;

    public CompositeSSLSocketFactory(SSLSocketFactory sslSocketFactory, SSLParameters sslParameters) {
        this.sslSocketFactory = ValidationUtils.requireNotNull(sslSocketFactory, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("SSLSocketFactory"));
        this.sslParameters = ValidationUtils.requireNotNull(sslParameters, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("SSLParameters"));
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslParameters.getCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslParameters.getCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        Socket socket = sslSocketFactory.createSocket();
        return withSslParameters(socket);
    }

    @Override
    public Socket createSocket(Socket socket, InputStream inputStream, boolean autoClosable) throws IOException {
        Socket newSocket = sslSocketFactory.createSocket(socket, inputStream, autoClosable);
        return withSslParameters(newSocket);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClosable) throws IOException {
        Socket newSocket = sslSocketFactory.createSocket(socket, host, port, autoClosable);
        return withSslParameters(newSocket);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Socket socket = sslSocketFactory.createSocket(host, port);
        return withSslParameters(socket);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
        Socket socket = sslSocketFactory.createSocket(host, port, localAddress, localPort);
        return withSslParameters(socket);
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        Socket socket = sslSocketFactory.createSocket(address, port);
        return withSslParameters(socket);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = sslSocketFactory.createSocket(address, port, localAddress, localPort);
        return withSslParameters(socket);
    }

    private Socket withSslParameters(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            sslSocket.setSSLParameters(sslParameters);
        }
        return socket;
    }

}
