package org.xbib.net.security;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javax.crypto.Cipher.DECRYPT_MODE;

public final class PemReader2 {

    private static final Pattern CERT_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                    // Base64 text
                    "-+END\\s+.*CERTIFICATE[^-]*-+",            // Footer
            CASE_INSENSITIVE);

    private static final Pattern KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                       // Base64 text
                    "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",            // Footer
            CASE_INSENSITIVE);

    private PemReader2() {
    }

    public static KeyStore loadTrustStore(InputStream certificateChainInputStream)
            throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        List<X509Certificate> certificateChain = readCertificateChain(certificateChainInputStream);
        for (X509Certificate certificate : certificateChain) {
            X500Principal principal = certificate.getSubjectX500Principal();
            keyStore.setCertificateEntry(principal.getName("RFC2253"), certificate);
        }
        return keyStore;
    }

    public static KeyStore loadKeyStore(InputStream certificateChainInputStream,
                                        InputStream privateKeyInputStream,
                                        String keyPassword)
            throws IOException, GeneralSecurityException {
        PKCS8EncodedKeySpec encodedKeySpec = readPrivateKey(privateKeyInputStream, keyPassword);
        PrivateKey key;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            key = keyFactory.generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException ignore) {
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            key = keyFactory.generatePrivate(encodedKeySpec);
        }
        List<X509Certificate> certificateChain = readCertificateChain(certificateChainInputStream);
        if (certificateChain.isEmpty()) {
            throw new CertificateException("Certificate file does not contain any certificates: " + certificateChainInputStream);
        }

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        char[] password = (keyPassword != null ? keyPassword : "").toCharArray();
        keyStore.setKeyEntry("key", key, password, certificateChain.toArray(Certificate[]::new));
        return keyStore;
    }

    private static List<X509Certificate> readCertificateChain(InputStream certificateChainInputStream)
            throws IOException, GeneralSecurityException {
        String contents = new String(certificateChainInputStream.readAllBytes(), US_ASCII);
        Matcher matcher = CERT_PATTERN.matcher(contents);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certificates = new ArrayList<>();
        int start = 0;
        while (matcher.find(start)) {
            byte[] buffer = base64Decode(matcher.group(1));
            certificates.add((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(buffer)));
            start = matcher.end();
        }

        return certificates;
    }

    private static PKCS8EncodedKeySpec readPrivateKey(InputStream inputStream, String keyPassword)
            throws IOException, GeneralSecurityException {
        String content = new String(inputStream.readAllBytes(), US_ASCII);
        Matcher matcher = KEY_PATTERN.matcher(content);
        if (!matcher.find()) {
            throw new KeyStoreException("found no private key");
        }
        byte[] encodedKey = base64Decode(matcher.group(1));
        if (keyPassword == null) {
            return new PKCS8EncodedKeySpec(encodedKey);
        }
        EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(encodedKey);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
        SecretKey secretKey = keyFactory.generateSecret(new PBEKeySpec(keyPassword.toCharArray()));
        Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
        cipher.init(DECRYPT_MODE, secretKey, encryptedPrivateKeyInfo.getAlgParameters());
        return encryptedPrivateKeyInfo.getKeySpec(cipher);
    }

    private static byte[] base64Decode(String base64) {
        return Base64.getMimeDecoder().decode(base64.getBytes(US_ASCII));
    }
}
