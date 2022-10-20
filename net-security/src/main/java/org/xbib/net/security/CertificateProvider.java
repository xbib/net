package org.xbib.net.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;

public interface CertificateProvider {

    Map.Entry<PrivateKey, Collection<? extends X509Certificate>> provide(InputStream key, String password, InputStream chain)
            throws CertificateException, IOException;

    Map.Entry<PrivateKey, Collection<? extends X509Certificate>> provideSelfSigned(String fqdn)
            throws CertificateException, IOException;
}
