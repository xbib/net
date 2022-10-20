package org.xbib.net.security.ssl.trustmanager;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
public class CertificateCapturingX509ExtendedTrustManager extends DelegatingX509ExtendedTrustManager {

    private final List<X509Certificate> certificatesCollector;

    public CertificateCapturingX509ExtendedTrustManager(X509ExtendedTrustManager trustManager, List<X509Certificate> certificatesCollector) {
        super(trustManager);
        this.certificatesCollector = certificatesCollector;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        certificatesCollector.addAll(Arrays.asList(chain));
        super.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        certificatesCollector.addAll(Arrays.asList(chain));
        super.checkServerTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        certificatesCollector.addAll(Arrays.asList(chain));
        super.checkClientTrusted(chain, authType, socket);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine sslEngine) throws CertificateException {
        certificatesCollector.addAll(Arrays.asList(chain));
        super.checkClientTrusted(chain, authType, sslEngine);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        certificatesCollector.addAll(Arrays.asList(chain));
        super.checkServerTrusted(chain, authType, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine sslEngine) throws CertificateException {
        certificatesCollector.addAll(Arrays.asList(chain));
        super.checkServerTrusted(chain, authType, sslEngine);
    }

}
