package org.xbib.net.security;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SecureSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory sslSocketFactory;

    private static volatile SecureSocketFactory secureSocketFactory;

    private SecureSocketFactory() throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        sslSocketFactory = loadTrustStore();
    }

    public static SocketFactory getInstance() {
        try {
            if (secureSocketFactory == null) {
                secureSocketFactory = new SecureSocketFactory();
            }
            return secureSocketFactory;
        } catch (Exception e) {
            throw new IllegalStateException("Failed create socket factory. Exception: " + e.getClass().getName() + ". Reason: " + e.getMessage(), e);
        }
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return sslSocketFactory.createSocket(socket, host, port, autoClose);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return sslSocketFactory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return sslSocketFactory.createSocket(address, port, localAddress, localPort);
    }

    private SSLSocketFactory loadTrustStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException, CertificateException {
        String keyStoreType = System.getProperty("truststore.type");
        String keyStorePath = System.getProperty("truststore.path");
        String password = System.getProperty("truststore.password");
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        Path path = Paths.get(keyStorePath);
        try (InputStream inputStream = Files.newInputStream(path)) {
            keyStore.load(inputStream, password != null ? password.toCharArray() : null);
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        String sslContextProtocol = System.getProperty("truststore.ssl.protocol");
        SSLContext sslContext = sslContextProtocol != null ? SSLContext.getInstance(sslContextProtocol) : SSLContext.getDefault();
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }
}
