package org.xbib.net.security.ssl.trustmanager;

import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Objects;

@FunctionalInterface
public interface ChainAndAuthTypeWithSocketValidator {

    boolean test(X509Certificate[] certificateChain, String authType, Socket socket);

    default ChainAndAuthTypeWithSocketValidator and(ChainAndAuthTypeWithSocketValidator other) {
        Objects.requireNonNull(other);
        return (certificateChain, authType, socket) -> test(certificateChain, authType, socket) && other.test(certificateChain, authType, socket);
    }

    default ChainAndAuthTypeWithSocketValidator or(ChainAndAuthTypeWithSocketValidator other) {
        Objects.requireNonNull(other);
        return (certificateChain, authType, socket) -> test(certificateChain, authType, socket) || other.test(certificateChain, authType, socket);
    }

}
