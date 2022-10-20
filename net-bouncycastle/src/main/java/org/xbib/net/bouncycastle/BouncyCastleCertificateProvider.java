package org.xbib.net.bouncycastle;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.OperatorCreationException;
import org.xbib.net.security.CertificateProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BouncyCastleCertificateProvider implements CertificateProvider {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static final Provider BOUNCYCASTLE = new BouncyCastleProvider();

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BOUNCYCASTLE);
        }
    }

    public BouncyCastleCertificateProvider() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map.Entry<PrivateKey, Collection<? extends X509Certificate>> provide(InputStream key, String password, InputStream chain)
            throws CertificateException, IOException {
        PEMParser pemParser = new PEMParser(new InputStreamReader(key, StandardCharsets.US_ASCII));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                .setProvider(BOUNCYCASTLE);
        Object object = pemParser.readObject();
        KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
        PrivateKey privateKey = kp.getPrivate();
        return Map.entry(privateKey, new CertificateFactory().engineGenerateCertificates(chain));
    }

    @Override
    public Map.Entry<PrivateKey, Collection<? extends X509Certificate>> provideSelfSigned(String fullQualifiedDomainName) throws CertificateException, IOException {
        try {
            SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
            selfSignedCertificate.generate(fullQualifiedDomainName, secureRandom, 2048);
            return Map.entry(selfSignedCertificate.getPrivateKey(), List.of(selfSignedCertificate.getCertificate()));
        } catch (NoSuchProviderException | NoSuchAlgorithmException | OperatorCreationException e) {
            throw new IOException(e);
        }
    }
}
