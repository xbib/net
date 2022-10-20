package org.xbib.net.security.ssl.trustmanager;

import javax.net.ssl.SSLEngine;
import java.security.cert.X509Certificate;
import java.util.Objects;

@FunctionalInterface
public interface ChainAndAuthTypeWithSSLEngineValidator {

    boolean test(X509Certificate[] certificateChain, String authType, SSLEngine sslEngine);

    default ChainAndAuthTypeWithSSLEngineValidator and(ChainAndAuthTypeWithSSLEngineValidator other) {
        Objects.requireNonNull(other);
        return (certificateChain, authType, sslEngine) -> test(certificateChain, authType, sslEngine) && other.test(certificateChain, authType, sslEngine);
    }

    default ChainAndAuthTypeWithSSLEngineValidator or(ChainAndAuthTypeWithSSLEngineValidator other) {
        Objects.requireNonNull(other);
        return (certificateChain, authType, sslEngine) -> test(certificateChain, authType, sslEngine) || other.test(certificateChain, authType, sslEngine);
    }

}
