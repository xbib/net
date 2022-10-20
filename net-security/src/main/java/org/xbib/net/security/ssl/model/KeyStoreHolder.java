package org.xbib.net.security.ssl.model;

import java.security.KeyStore;

public final class KeyStoreHolder {

    private final KeyStore keyStore;
    private final char[] keyPassword;

    public KeyStoreHolder(KeyStore keyStore, char[] keyPassword) {
        this.keyStore = keyStore;
        this.keyPassword = keyPassword;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public char[] getKeyPassword() {
        return keyPassword;
    }

}
