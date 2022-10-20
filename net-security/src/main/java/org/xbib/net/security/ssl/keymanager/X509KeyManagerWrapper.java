package org.xbib.net.security.ssl.keymanager;

import org.xbib.net.security.ssl.util.KeyManagerUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509KeyManager;
import java.security.Principal;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link KeyManagerUtils KeyManagerUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 */
public final class X509KeyManagerWrapper extends DelegatingKeyManager<X509KeyManager> {

    public X509KeyManagerWrapper(X509KeyManager keyManager) {
        super(keyManager);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine sslEngine) {
        return keyManager.chooseClientAlias(keyTypes, issuers, null);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine sslEngine) {
        return keyManager.chooseServerAlias(keyType, issuers, null);
    }

}
