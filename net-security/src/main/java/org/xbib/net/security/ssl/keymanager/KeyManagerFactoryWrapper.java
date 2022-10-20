package org.xbib.net.security.ssl.keymanager;

import org.xbib.net.security.ssl.util.KeyManagerUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.security.Provider;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link KeyManagerUtils KeyManagerUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 */
public final class KeyManagerFactoryWrapper extends KeyManagerFactory {

    private static final String KEY_MANAGER_FACTORY_ALGORITHM = "no-algorithm";

    private static final Provider PROVIDER = new Provider("", "1.0", "") {};

    public KeyManagerFactoryWrapper(KeyManager keyManager) {
        super(new KeyManagerFactorySpiWrapper(keyManager), PROVIDER, KEY_MANAGER_FACTORY_ALGORITHM);
    }

}
