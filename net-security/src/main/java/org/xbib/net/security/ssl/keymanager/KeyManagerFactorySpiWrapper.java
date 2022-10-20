package org.xbib.net.security.ssl.keymanager;

import org.xbib.net.security.ssl.util.ValidationUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import java.security.KeyStore;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
class KeyManagerFactorySpiWrapper extends KeyManagerFactorySpi {

    private static final String NO_KEY_MANAGER_EXCEPTION_MESSAGE = "No valid KeyManager has been provided. KeyManager must be present, but was absent.";

    private final KeyManager[] keyManagers;

    KeyManagerFactorySpiWrapper(KeyManager keyManager) {
        ValidationUtils.requireNotNull(keyManager, NO_KEY_MANAGER_EXCEPTION_MESSAGE);
        this.keyManagers = new KeyManager[]{keyManager};
    }

    @Override
    protected void engineInit(KeyStore keyStore, char[] keyStorePassword) {
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
    }

    @Override
    protected KeyManager[] engineGetKeyManagers() {
        return keyManagers;
    }

}
