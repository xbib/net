package org.xbib.net.security;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class SSLUtil {

    private SSLUtil() {
    }

    public static void createSslContextFactory(File cert, File privateKey) {
        try {
            byte[][] certBytes = parseDERFromPEM(Files.readAllBytes(cert.toPath()),
                    "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
            byte[][] keyBytes = parseDERFromPEM(Files.readAllBytes(privateKey.toPath()),
                    "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
            X509Certificate[] xCerts = new X509Certificate[certBytes.length];
            RSAPrivateKey key = generatePrivateKeyFromDER(keyBytes[0]);
            for (int i = 0; i < certBytes.length; i++) {
                xCerts[i] = generateCertificateFromDER(certBytes[i]);
            }
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(null);
            keystore.setCertificateEntry("cert-alias", xCerts[0]);
            keystore.setKeyEntry("key-alias", key, null, xCerts);
            /*SslContextFactory sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStore(keystore);
            sslContextFactory.setKeyStorePassword(TEMP_PW);
            return sslContextFactory;*/
        } catch (IOException | KeyStoreException | InvalidKeySpecException
                | NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Parses the given PEM certificate (chain) to DER certificates.
     *
     * @param pem            The PEM certificate (chain) to convert.
     * @param beginDelimiter The begin delimiter of the certificate.
     * @param endDelimiter   The end delimiter of the certificate.
     * @return An array containing all certificates as binary (byte[]) data.
     */
    private static byte[][] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] elements = data.split(beginDelimiter);
        List<String> newTokens = new ArrayList<>();
        for (int i = 1; i < elements.length; i++) {
            newTokens.add(elements[i].split(endDelimiter)[0]);
        }
        byte[][] ders = new byte[2][];
        for (int i = 0; i < newTokens.size(); i++) {
            String string = newTokens.get(i);
            ders[i] = Base64.getMimeDecoder().decode(string);
        }
        return ders;
    }

    /**
     * Generates a {@link RSAPrivateKey} from the given DER key.
     *
     * @param keyBytes The private key as binary (byte[]) data.
     * @return An {@link RSAPrivateKey} instance representing the key.
     * @throws InvalidKeySpecException  The key is inappropriate.
     * @throws NoSuchAlgorithmException No provider supports RSA.
     */
    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    /**
     * Generates an {@link X509Certificate} from the given DER certificate.
     *
     * @param certBytes The certificate as binary (byte[]) data.
     * @return An {@link X509Certificate} instance representing the certificate.
     * @throws CertificateException No provider supports X.509.
     */
    private static X509Certificate generateCertificateFromDER(byte[] certBytes)
            throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(certBytes));
    }

}