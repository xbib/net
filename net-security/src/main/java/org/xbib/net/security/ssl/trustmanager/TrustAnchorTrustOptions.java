package org.xbib.net.security.ssl.trustmanager;

import javax.net.ssl.CertPathTrustManagerParameters;
import java.security.cert.TrustAnchor;
import java.util.Set;

@FunctionalInterface
public interface TrustAnchorTrustOptions<R extends CertPathTrustManagerParameters> extends TrustOptions<Set<TrustAnchor>, R> {

    @Override
    R apply(Set<TrustAnchor> trustAnchors) throws Exception;

}
