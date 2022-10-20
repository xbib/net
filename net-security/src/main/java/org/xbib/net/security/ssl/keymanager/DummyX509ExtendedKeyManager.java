package org.xbib.net.security.ssl.keymanager;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
public final class DummyX509ExtendedKeyManager extends X509ExtendedKeyManager {

    private static final X509ExtendedKeyManager INSTANCE = new DummyX509ExtendedKeyManager();

    private DummyX509ExtendedKeyManager() {}

    public static X509ExtendedKeyManager getInstance() {
        return INSTANCE;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return null;
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return null;
    }

    @Override
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine sslEngine) {
        return null;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return null;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return null;
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine sslEngine) {
        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return null;
    }

}
