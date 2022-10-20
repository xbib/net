package org.xbib.net.security.ssl.util;

import org.xbib.net.security.ssl.hostnameverifier.BasicHostNameVerifier;
import org.xbib.net.security.ssl.hostnameverifier.UnsafeHostNameVerifier;

import javax.net.ssl.HostnameVerifier;

public final class HostnameVerifierUtils {

    private HostnameVerifierUtils() {}

    public static HostnameVerifier createBasic() {
        return BasicHostNameVerifier.getInstance();
    }

    public static HostnameVerifier createUnsafe() {
        return UnsafeHostNameVerifier.getInstance();
    }

}
