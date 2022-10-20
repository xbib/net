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
public final class EnhanceableX509ExtendedTrustManager extends DelegatingX509ExtendedTrustManager {

    private final ChainAndAuthTypeValidator chainAndAuthTypeValidator;
    private final ChainAndAuthTypeWithSocketValidator chainAndAuthTypeWithSocketValidator;
    private final ChainAndAuthTypeWithSSLEngineValidator chainAndAuthTypeWithSSLEngineValidator;

    public EnhanceableX509ExtendedTrustManager(
            X509ExtendedTrustManager trustManager,
            ChainAndAuthTypeValidator chainAndAuthTypeValidator,
            ChainAndAuthTypeWithSocketValidator chainAndAuthTypeWithSocketValidator,
            ChainAndAuthTypeWithSSLEngineValidator chainAndAuthTypeWithSSLEngineValidator) {

        super(trustManager);
        this.chainAndAuthTypeValidator = chainAndAuthTypeValidator;
        this.chainAndAuthTypeWithSocketValidator = chainAndAuthTypeWithSocketValidator;
        this.chainAndAuthTypeWithSSLEngineValidator = chainAndAuthTypeWithSSLEngineValidator;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chainAndAuthTypeValidator != null && chainAndAuthTypeValidator.test(chain, authType)) {
            return;
        }
        super.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (chainAndAuthTypeWithSocketValidator != null && chainAndAuthTypeWithSocketValidator.test(chain, authType, socket)) {
            return;
        }
        super.checkClientTrusted(chain, authType, socket);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine sslEngine) throws CertificateException {
        if (chainAndAuthTypeWithSSLEngineValidator != null && chainAndAuthTypeWithSSLEngineValidator.test(chain, authType, sslEngine)) {
            return;
        }
        super.checkClientTrusted(chain, authType, sslEngine);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chainAndAuthTypeValidator != null && chainAndAuthTypeValidator.test(chain, authType)) {
            return;
        }
        super.checkServerTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (chainAndAuthTypeWithSocketValidator != null && chainAndAuthTypeWithSocketValidator.test(chain, authType, socket)) {
            return;
        }
        super.checkServerTrusted(chain, authType, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine sslEngine) throws CertificateException {
        if (chainAndAuthTypeWithSSLEngineValidator != null && chainAndAuthTypeWithSSLEngineValidator.test(chain, authType, sslEngine)) {
            return;
        }
        super.checkServerTrusted(chain, authType, sslEngine);
    }

}
