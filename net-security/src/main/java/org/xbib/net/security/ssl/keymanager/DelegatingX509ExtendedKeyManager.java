package org.xbib.net.security.ssl.keymanager;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.security.Principal;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
class DelegatingX509ExtendedKeyManager extends DelegatingKeyManager<X509ExtendedKeyManager> {

    public DelegatingX509ExtendedKeyManager(X509ExtendedKeyManager keyManager) {
        super(keyManager);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine sslEngine) {
        return keyManager.chooseEngineClientAlias(keyTypes, issuers, sslEngine);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine sslEngine) {
        return keyManager.chooseEngineServerAlias(keyType, issuers, sslEngine);
    }

}
