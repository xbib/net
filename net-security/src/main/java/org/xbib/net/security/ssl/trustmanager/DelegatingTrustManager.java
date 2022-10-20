package org.xbib.net.security.ssl.trustmanager;

import org.xbib.net.security.ssl.util.ValidationUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
abstract class DelegatingTrustManager<T extends X509TrustManager> extends X509ExtendedTrustManager {

    T trustManager;

    DelegatingTrustManager(T trustManager) {
        this.trustManager = ValidationUtils.requireNotNull(trustManager, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("TrustManager"));
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        trustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public abstract void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException;

    @Override
    public abstract void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException;

    @Override
    public abstract void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException;

    @Override
    public abstract void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException;

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509Certificate[] acceptedIssuers = trustManager.getAcceptedIssuers();
        return Arrays.copyOf(acceptedIssuers, acceptedIssuers.length);
    }

}
