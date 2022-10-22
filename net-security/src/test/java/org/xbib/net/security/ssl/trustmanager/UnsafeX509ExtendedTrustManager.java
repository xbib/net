package org.xbib.net.security.ssl.trustmanager;

import org.xbib.net.security.ssl.util.TrustManagerUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.X509Certificate;

/**
 * An insecure {@link UnsafeX509ExtendedTrustManager TrustManager} that trusts all X.509 certificates without any verification.
 * <p>
 * <strong>NOTE:</strong>
 * Never use this {@link UnsafeX509ExtendedTrustManager} in production.
 * It is purely for testing purposes, and thus it is very insecure.
 * </p>
 * <br>
 * Suppressed warning: java:S4830 - "Server certificates should be verified during SSL/TLS connections"
 *                                  This TrustManager doesn't validate certificates and should not be used at production.
 *                                  It is just meant to be used for testing purposes and it is designed not to verify server certificates.
 *
 * <p>
 * <br>
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link TrustManagerUtils TrustManagerUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 * </p>
 */
@SuppressWarnings("java:S4830")
public final class UnsafeX509ExtendedTrustManager extends X509ExtendedTrustManager {

    private static final X509ExtendedTrustManager INSTANCE = new UnsafeX509ExtendedTrustManager();
    private static final X509Certificate[] EMPTY_CERTIFICATES = new X509Certificate[0];

    private UnsafeX509ExtendedTrustManager() {}

    public static X509ExtendedTrustManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) {
        // ignore certificate validation
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType, Socket socket) {
        // ignore certificate validation
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType, SSLEngine sslEngine) {
        // ignore certificate validation
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) {
        // ignore certificate validation
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType, Socket socket) {
        // ignore certificate validation
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType, SSLEngine sslEngine) {
        // ignore certificate validation
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return EMPTY_CERTIFICATES;
    }

}
