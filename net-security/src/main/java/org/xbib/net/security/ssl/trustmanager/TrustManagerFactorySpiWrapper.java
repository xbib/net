package org.xbib.net.security.ssl.trustmanager;

import org.xbib.net.security.ssl.util.ValidationUtils;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import java.security.KeyStore;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
class TrustManagerFactorySpiWrapper extends TrustManagerFactorySpi {

    private final TrustManager[] trustManagers;

    TrustManagerFactorySpiWrapper(TrustManager trustManager) {
        ValidationUtils.requireNotNull(trustManager, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("TrustManager"));
        this.trustManagers = new TrustManager[]{trustManager};
    }

    @Override
    protected void engineInit(KeyStore keyStore) {
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return trustManagers;
    }

}
