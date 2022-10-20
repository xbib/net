package org.xbib.net.security.ssl.trustmanager;

import org.xbib.net.security.ssl.util.TrustManagerUtils;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.Provider;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link TrustManagerUtils TrustManagerUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 */
public final class TrustManagerFactoryWrapper extends TrustManagerFactory {

    private static final String TRUST_MANAGER_FACTORY_ALGORITHM = "no-algorithm";

    private static final Provider PROVIDER = new Provider("", "1.0", "") {};

    public TrustManagerFactoryWrapper(TrustManager trustManager) {
        super(new TrustManagerFactorySpiWrapper(trustManager), PROVIDER, TRUST_MANAGER_FACTORY_ALGORITHM);
    }
}
