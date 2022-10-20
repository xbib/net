package org.xbib.net.security.ssl.trustmanager;

import org.xbib.net.security.ssl.util.TrustManagerUtils;
import org.xbib.net.security.ssl.util.ValidationUtils;

import javax.net.ssl.X509ExtendedTrustManager;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link TrustManagerUtils TrustManagerUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 */
public class HotSwappableX509ExtendedTrustManager extends DelegatingX509ExtendedTrustManager {

    public HotSwappableX509ExtendedTrustManager(X509ExtendedTrustManager trustManager) {
        super(trustManager);
    }

    public void setTrustManager(X509ExtendedTrustManager trustManager) {
        this.trustManager = ValidationUtils.requireNotNull(trustManager, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("TrustManager"));
    }

}
