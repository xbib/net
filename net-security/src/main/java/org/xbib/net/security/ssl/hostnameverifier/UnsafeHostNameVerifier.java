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
public final class UnsafeHostNameVerifier implements HostnameVerifier {

    private static final HostnameVerifier INSTANCE = new UnsafeHostNameVerifier();

    private UnsafeHostNameVerifier() {}

    @Override
    public boolean verify(String host, SSLSession sslSession) {
        return true;
    }

    public static HostnameVerifier getInstance() {
        return INSTANCE;
    }

}
