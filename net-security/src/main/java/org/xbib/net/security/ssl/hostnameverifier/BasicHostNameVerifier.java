package org.xbib.net.security.ssl.hostnameverifier;

import org.xbib.net.security.ssl.util.HostnameVerifierUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link HostnameVerifierUtils HostnameVerifierUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 */
public final class BasicHostNameVerifier implements HostnameVerifier {

    private static final HostnameVerifier INSTANCE = new BasicHostNameVerifier();

    private BasicHostNameVerifier() {}

    @Override
    public boolean verify(String host, SSLSession sslSession) {
        return host.equalsIgnoreCase(sslSession.getPeerHost());
    }

    public static HostnameVerifier getInstance() {
        return INSTANCE;
    }

}
