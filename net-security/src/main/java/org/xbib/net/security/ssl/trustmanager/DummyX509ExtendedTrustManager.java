package org.xbib.net.security.ssl.trustmanager;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
public final class DummyX509ExtendedTrustManager extends X509ExtendedTrustManager {

    private static final X509ExtendedTrustManager INSTANCE = new DummyX509ExtendedTrustManager();
    private static final X509Certificate[] EMPTY_CERTIFICATES = new X509Certificate[0];
    private static final String MISSING_IMPLEMENTATION = "No X509ExtendedTrustManager implementation available";

    private DummyX509ExtendedTrustManager() {}

    public static X509ExtendedTrustManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        throw new CertificateException(MISSING_IMPLEMENTATION);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType, Socket socket) throws CertificateException {
        throw new CertificateException(MISSING_IMPLEMENTATION);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType, SSLEngine sslEngine) throws CertificateException {
        throw new CertificateException(MISSING_IMPLEMENTATION);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        throw new CertificateException(MISSING_IMPLEMENTATION);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType, Socket socket) throws CertificateException {
        throw new CertificateException(MISSING_IMPLEMENTATION);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType, SSLEngine sslEngine) throws CertificateException {
        throw new CertificateException(MISSING_IMPLEMENTATION);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return EMPTY_CERTIFICATES;
    }

}
