package org.xbib.net.security.ssl.keymanager;

import org.xbib.net.security.ssl.util.KeyManagerUtils;
import org.xbib.net.security.ssl.util.ValidationUtils;

import javax.net.ssl.X509ExtendedKeyManager;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link KeyManagerUtils KeyManagerUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 */
public final class HotSwappableX509ExtendedKeyManager extends DelegatingX509ExtendedKeyManager {

    public HotSwappableX509ExtendedKeyManager(X509ExtendedKeyManager keyManager) {
        super(keyManager);
    }

    public void setKeyManager(X509ExtendedKeyManager keyManager) {
        this.keyManager = ValidationUtils.requireNotNull(keyManager, ValidationUtils.GENERIC_EXCEPTION_MESSAGE.apply("KeyManager"));
    }

}
