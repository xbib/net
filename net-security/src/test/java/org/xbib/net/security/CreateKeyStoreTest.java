package org.xbib.net.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.xbib.net.security.util.HexUtil;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CreateKeyStoreTest {

    @Test
    public void list() {
        Arrays.stream(Security.getProviders())
                .flatMap(p -> p.entrySet().stream())
                .map(e -> (String) e.getKey())
                .filter(e -> e.startsWith("KeyStore."))
                .filter(e -> !e.endsWith("ImplementedIn"))
                .map(e -> e.substring("KeyStore.".length()))
                .sorted()
                .forEach(System.out::println);
    }

    @Test
    public void testCreation() throws Exception {
        String keyStorePassword = "secure";

        InputStream keyInputStream = getClass().getResourceAsStream("/rsa.key");
        if (keyInputStream == null) {
            return;
        }
        InputStream chainInputStream = getClass().getResourceAsStream("/rsa.crt");
        if (chainInputStream == null) {
            return;
        }
        String privateKeyPassword = null;

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, keyStorePassword.toCharArray());

        // this works with "Sun", not "BC" ("PKCS12 does not support non-PrivateKeys")
        byte[] encodedKey = HexUtil.fromHex("227d95a88d02cb89823b91f8e6a6d34435c8e391a5576acd0bd4d64464a6e020");
        SecretKey secretKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyStorePassword.toCharArray());
        keyStore.setEntry("secret", secretKeyEntry, protectionParameter);

        Collection<? extends X509Certificate> certChain = null;
        PrivateKey privateKey = null;
        ServiceLoader<CertificateProvider> certificateProviders = ServiceLoader.load(CertificateProvider.class);
        boolean found = false;
        for (CertificateProvider provider : certificateProviders) {
            Map.Entry<PrivateKey, Collection<? extends X509Certificate>> entry =
                    provider.provide(keyInputStream, privateKeyPassword, chainInputStream);
            if (entry != null) {
                privateKey = entry.getKey();
                certChain = entry.getValue();
                found = true;
                break;
            }
        }
        if (!found) {
            throw new CertificateException("no certificate found");
        }
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(privateKey,
                CertificateReader.orderCertificateChain(certChain).toArray(new Certificate[0]));
        protectionParameter = new KeyStore.PasswordProtection(keyStorePassword.toCharArray());
        keyStore.setEntry("key", privateKeyEntry, protectionParameter);

        for (X509Certificate certificate : readTrustStore()) {
            KeyStore.TrustedCertificateEntry trustedCertificateEntry = new KeyStore.TrustedCertificateEntry(certificate);
            String alias = certificate.getSubjectX500Principal().getName();
            System.out.println("alias = " + alias);
            keyStore.setEntry(alias, trustedCertificateEntry, null);
        }

        keyStore.store(new FileOutputStream("build/keystore.pkcs12"), keyStorePassword.toCharArray());
    }

    @Test
    public void testRead() throws Exception {
        String keyStorePassword = "secure";
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        Path path = Paths.get("build/keystore.pkcs12");
        if (!Files.exists(path)) {
            return; // ignore this test
        }
        keyStore.load(Files.newInputStream(path), keyStorePassword.toCharArray());
        Enumeration<String> e = keyStore.aliases();
        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            if ("secret".equals(alias)) {
                SecretKey secretKey = (SecretKey) keyStore.getKey(alias, keyStorePassword.toCharArray());
                assertNotNull(secretKey);
                System.out.println("secret = " + HexUtil.toHex(secretKey.getEncoded()));
            }
            if ("key".equals(alias)) {
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyStorePassword.toCharArray());
                assertNotNull(privateKey);
                System.out.println("algo=" + privateKey.getAlgorithm());
                System.out.println("format=" + privateKey.getFormat());
                X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
                assertNotNull(x509Certificate);
                Principal subject = x509Certificate.getSubjectX500Principal();
                String[] subjectArray = subject.toString().split(",");
                for (String s : subjectArray) {
                    String[] str = s.trim().split("=");
                    String key = str[0];
                    String value = str[1];
                    System.out.println(key + " - " + value);
                }
            } else {
                Certificate certificate = keyStore.getCertificate(alias);
                if (certificate instanceof X509Certificate) {
                    X509Certificate x509Certificate = (X509Certificate) certificate;
                    Principal subject = x509Certificate.getSubjectX500Principal();
                    System.out.println("found certificate: " + subject.toString());
                }
            }
        }
    }

    private List<X509Certificate> readTrustStore() throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        List<TrustManager> trustManagers = Arrays.asList(trustManagerFactory.getTrustManagers());
        List<X509Certificate> list = trustManagers.stream()
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .map(trustManager -> Arrays.asList(trustManager.getAcceptedIssuers()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        System.out.println("found " + list.size() + " certificates in trust manager");
        return list;
    }
}
