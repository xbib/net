package org.xbib.net.security.ssl.trustmanager;

import javax.net.ssl.CertPathTrustManagerParameters;
import java.security.KeyStore;

@FunctionalInterface
public interface TrustStoreTrustOptions<R extends CertPathTrustManagerParameters> extends TrustOptions<KeyStore, R> {

    @Override
    R apply(KeyStore trustStore) throws Exception;

}
