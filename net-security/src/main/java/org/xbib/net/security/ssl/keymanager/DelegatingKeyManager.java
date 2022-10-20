package org.xbib.net.security.ssl.keymanager;

import org.xbib.net.security.ssl.util.ValidationUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
abstract class DelegatingKeyManager<T extends X509KeyManager> extends X509ExtendedKeyManager {

    private static final String NO_KEY_MANAGER_EXCEPTION_MESSAGE = "No valid KeyManager has been provided. KeyManager must be present, but was absent.";

    T keyManager;

    DelegatingKeyManager(T keyManager) {
        this.keyManager = ValidationUtils.requireNotNull(keyManager, NO_KEY_MANAGER_EXCEPTION_MESSAGE);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return keyManager.chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return keyManager.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return keyManager.getPrivateKey(alias);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return keyManager.getCertificateChain(alias);
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return keyManager.getClientAliases(keyType, issuers);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return keyManager.getServerAliases(keyType, issuers);
    }

    @Override
    public abstract String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine);

    @Override
    public abstract String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine);

}
