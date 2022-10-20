package org.xbib.net.security.ssl.trustmanager;

import java.security.cert.X509Certificate;
import java.util.Objects;

@FunctionalInterface
public interface ChainAndAuthTypeValidator {

    boolean test(X509Certificate[] certificateChain, String authType);

    default ChainAndAuthTypeValidator and(ChainAndAuthTypeValidator other) {
        Objects.requireNonNull(other);
        return (certificateChain, authType) -> test(certificateChain, authType) && other.test(certificateChain, authType);
    }

    default ChainAndAuthTypeValidator or(ChainAndAuthTypeValidator other) {
        Objects.requireNonNull(other);
        return (certificateChain, authType) -> test(certificateChain, authType) || other.test(certificateChain, authType);
    }

}
