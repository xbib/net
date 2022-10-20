package org.xbib.net.security;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Map;

public class DefaultCertificateProvider implements CertificateProvider {

    public DefaultCertificateProvider() {
    }

    @Override
    public Map.Entry<PrivateKey, Collection<? extends X509Certificate>> provide(InputStream keyInputStream, String password, InputStream chainInputStream)
            throws CertificateException, IOException {
        try {
            PrivateKeyReader privateKeyReader = new PrivateKeyReader();
            PrivateKey privateKey = privateKeyReader.readPrivateKey(keyInputStream, password);
            CertificateReader certificateReader = new CertificateReader();
            Collection<? extends X509Certificate> chain = certificateReader.readChain(chainInputStream);
            return Map.entry(privateKey, chain);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeySpecException |
                NoSuchPaddingException | InvalidKeyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Map.Entry<PrivateKey, Collection<? extends X509Certificate>> provideSelfSigned(String fqdn) throws CertificateException {
        throw new CertificateException();
    }
}
