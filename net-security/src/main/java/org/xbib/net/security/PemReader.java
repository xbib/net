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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Base64.getMimeDecoder;
import static java.util.Locale.US;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static org.xbib.net.security.util.DerUtils.decodeSequence;
import static org.xbib.net.security.util.DerUtils.decodeSequenceOptionalElement;
import static org.xbib.net.security.util.DerUtils.encodeBitString;
import static org.xbib.net.security.util.DerUtils.encodeOctetString;
import static org.xbib.net.security.util.DerUtils.encodeOID;
import static org.xbib.net.security.util.DerUtils.encodeSequence;

public final class PemReader {

    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+(?:(.*)\\s+)?PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                                  // Base64 text
                    "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",                       // Footer
            CASE_INSENSITIVE);

    private static final Pattern PUBLIC_KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+(?:(.*)\\s+)?PUBLIC\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                      // Base64 text
                    "-+END\\s+.*PUBLIC\\s+KEY[^-]*-+",            // Footer
            CASE_INSENSITIVE);

    private static final Pattern CERT_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                    // Base64 text
                    "-+END\\s+.*CERTIFICATE[^-]*-+",            // Footer
            CASE_INSENSITIVE);

    // test data must be exactly 20 bytes for DSA
    private static final byte[] TEST_SIGNATURE_DATA = "01234567890123456789".getBytes(US_ASCII);
    private static final Set<String> SUPPORTED_KEY_TYPES = Set.of("RSA", "EC", "DSA");

    private static final byte[] VERSION_0_ENCODED = new byte[]{2, 1, 0};
    private static final byte[] RSA_KEY_OID = encodeOID("1.2.840.113549.1.1.1");
    private static final byte[] DSA_KEY_OID = encodeOID("1.2.840.10040.4.1");
    private static final byte[] EC_KEY_OID = encodeOID("1.2.840.10045.2.1");
    private static final byte[] DER_NULL = new byte[]{5, 0};

    private PemReader() {
    }

    public static boolean isPem(String data) {
        return isPemCertificate(data) ||
                isPemPublicKey(data) ||
                isPemPrivateKey(data);
    }

    private static boolean isPemPrivateKey(String data) {
        return PRIVATE_KEY_PATTERN.matcher(data).find();
    }

    private static boolean isPemPublicKey(String data) {
        return PUBLIC_KEY_PATTERN.matcher(data).find();
    }

    private static boolean isPemCertificate(String data) {
        return CERT_PATTERN.matcher(data).find();
    }

    public static KeyStore loadTrustStore(InputStream inputStream)
            throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        List<X509Certificate> certificateChain = readCertificateChain(inputStream);
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
        return loadKeyStore(certificateChainInputStream, privateKeyInputStream, keyPassword, false);
    }

    public static KeyStore loadKeyStore(InputStream certificateChainInputStream,
                                        InputStream privateKeyInputStream,
                                        String keyPassword, boolean storeKeyWithPassword)
            throws IOException, GeneralSecurityException {
        PrivateKey key = loadPrivateKey(privateKeyInputStream, keyPassword);
        List<X509Certificate> certificateChain = readCertificateChain(certificateChainInputStream);
        if (certificateChain.isEmpty()) {
            throw new CertificateException("Certificate file does not contain any certificates");
        }
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        Certificate[] certificates = certificateChain.toArray(new Certificate[0]);
        boolean foundMatchingCertificate = false;
        for (int i = 0; i < certificates.length; i++) {
            Certificate certificate = certificates[i];
            if (matches(key, certificate)) {
                foundMatchingCertificate = true;
                certificates[i] = certificates[0];
                certificates[0] = certificate;
                break;
            }
        }
        if (!foundMatchingCertificate) {
            throw new KeyStoreException("Private key does not match the public key of any certificate");
        }
        char[] password = (storeKeyWithPassword ? keyPassword : "").toCharArray();
        keyStore.setKeyEntry("key", key, password, certificates);
        return keyStore;
    }

    public static List<X509Certificate> readCertificateChain(InputStream certificateChain)
            throws IOException, GeneralSecurityException {
        return readCertificateChain(new String(certificateChain.readAllBytes(), StandardCharsets.US_ASCII));
    }

    public static List<X509Certificate> readCertificateChain(String certificateChain)
            throws CertificateException {
        Matcher matcher = CERT_PATTERN.matcher(certificateChain);
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

    public static PrivateKey loadPrivateKey(InputStream inputStream, String keyPassword)
            throws IOException, GeneralSecurityException {
        return loadPrivateKey(new String(inputStream.readAllBytes(), US_ASCII), keyPassword);
    }

    public static PrivateKey loadPrivateKey(String privateKey, String keyPassword)
            throws IOException, GeneralSecurityException {
        Matcher matcher = PRIVATE_KEY_PATTERN.matcher(privateKey);
        if (!matcher.find()) {
            throw new KeyStoreException("did not find a private key");
        }
        String keyType = matcher.group(1);
        String base64Key = matcher.group(2);
        if (base64Key.toLowerCase(US).startsWith("proc-type")) {
            throw new InvalidKeySpecException("Password protected PKCS#1 private keys are not supported");
        }
        byte[] encodedKey = base64Decode(base64Key);
        PKCS8EncodedKeySpec encodedKeySpec;
        if (keyType == null) {
            encodedKeySpec = new PKCS8EncodedKeySpec(encodedKey);
        } else if ("ENCRYPTED".equals(keyType)) {
            if (keyPassword == null) {
                throw new KeyStoreException("Private key is encrypted, but no password was provided");
            }
            EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(encodedKey);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
            SecretKey secretKey = keyFactory.generateSecret(new PBEKeySpec(keyPassword.toCharArray()));
            Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
            cipher.init(DECRYPT_MODE, secretKey, encryptedPrivateKeyInfo.getAlgParameters());
            encodedKeySpec = encryptedPrivateKeyInfo.getKeySpec(cipher);
        } else {
            return loadPkcs1PrivateKey(keyType, encodedKey);
        }
        // this code requires a key in PKCS8 format which is not the default openssl format
        // to convert to the PKCS8 format you use : openssl pkcs8 -topk8 ...
        Set<String> algorithms = Set.of("RSA", "EC", "DSA");
        for (String algorithm : algorithms) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
                return keyFactory.generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore) {
            }
        }
        throw new InvalidKeySpecException("Key type must be one of " + algorithms);
    }

    private static PrivateKey loadPkcs1PrivateKey(String pkcs1KeyType, byte[] pkcs1Key)
            throws GeneralSecurityException, IOException {
        byte[] pkcs8Key;
        switch (pkcs1KeyType) {
            case "RSA":
                pkcs8Key = rsaPkcs1ToPkcs8(pkcs1Key);
                break;
            case "DSA":
                pkcs8Key = dsaPkcs1ToPkcs8(pkcs1Key);
                break;
            case "EC":
                pkcs8Key = ecPkcs1ToPkcs8(pkcs1Key);
                break;
            default:
                throw new InvalidKeySpecException(pkcs1KeyType + " private key in PKCS 1 format is not supported");
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(pkcs1KeyType);
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Key));
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeySpecException(format("Invalid %s private key in PKCS 1 format", pkcs1KeyType), e);
        }
    }

    static byte[] rsaPublicKeyPkcs1ToPkcs8(byte[] pkcs1) throws IOException {
        byte[] keyIdentifier = encodeSequence(RSA_KEY_OID, DER_NULL);
        return encodeSequence(keyIdentifier, encodeBitString(0, pkcs1));
    }

    static byte[] rsaPkcs1ToPkcs8(byte[] pkcs1) throws IOException {
        byte[] keyIdentifier = encodeSequence(RSA_KEY_OID, DER_NULL);
        return encodeSequence(VERSION_0_ENCODED, keyIdentifier, encodeOctetString(pkcs1));
    }

    static byte[] dsaPkcs1ToPkcs8(byte[] pkcs1)
            throws InvalidKeySpecException, IOException {
        List<byte[]> elements = decodeSequence(pkcs1);
        if (elements.size() != 6) {
            throw new InvalidKeySpecException("Expected DSA key to have 6 elements");
        }
        byte[] keyIdentifier = encodeSequence(DSA_KEY_OID, encodeSequence(elements.get(1), elements.get(2), elements.get(3)));
        return encodeSequence(VERSION_0_ENCODED, keyIdentifier, encodeOctetString(elements.get(5)));
    }

    static byte[] ecPkcs1ToPkcs8(byte[] pkcs1)
            throws InvalidKeySpecException, IOException {
        List<byte[]> elements = decodeSequence(pkcs1);
        if (elements.size() != 4) {
            throw new InvalidKeySpecException("Expected EC key to have 4 elements");
        }
        byte[] curveOid = decodeSequenceOptionalElement(elements.get(2));
        byte[] keyIdentifier = encodeSequence(EC_KEY_OID, curveOid);
        return encodeSequence(VERSION_0_ENCODED, keyIdentifier, encodeOctetString(encodeSequence(elements.get(0), elements.get(1), elements.get(3))));
    }

    public static PublicKey loadPublicKey(InputStream publicKeyFile)
            throws IOException, GeneralSecurityException {
        return loadPublicKey(new String(publicKeyFile.readAllBytes(), US_ASCII));
    }

    public static PublicKey loadPublicKey(String publicKey)
            throws GeneralSecurityException {
        Matcher matcher = PUBLIC_KEY_PATTERN.matcher(publicKey);
        if (!matcher.find()) {
            throw new KeyStoreException("did not find a public key");
        }
        String keyType = matcher.group(1);
        String base64Key = matcher.group(2);
        byte[] encodedKey = base64Decode(base64Key);

        if (keyType == null) {
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(encodedKey);
            for (String algorithm : SUPPORTED_KEY_TYPES) {
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
                    return keyFactory.generatePublic(encodedKeySpec);
                } catch (InvalidKeySpecException ignore) {
                }
            }
            throw new InvalidKeySpecException("Key type must be one of " + SUPPORTED_KEY_TYPES);
        }

        if (!"RSA".equals(keyType)) {
            throw new InvalidKeySpecException(format("%s public key in PKCS 1 format is not supported", keyType));
        }
        try {
            byte[] pkcs8Key = rsaPublicKeyPkcs1ToPkcs8(encodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance(keyType);
            return keyFactory.generatePublic(new X509EncodedKeySpec(pkcs8Key));
        } catch (InvalidKeySpecException | IOException e) {
            throw new InvalidKeySpecException(format("Invalid %s private key in PKCS 1 format", keyType), e);
        }
    }

    private static boolean matches(PrivateKey privateKey, Certificate certificate) {
        try {
            PublicKey publicKey = certificate.getPublicKey();

            Signature signer = createSignature(privateKey, publicKey);

            signer.initSign(privateKey);
            signer.update(TEST_SIGNATURE_DATA);
            byte[] signature = signer.sign();

            signer.initVerify(publicKey);
            signer.update(TEST_SIGNATURE_DATA);
            return signer.verify(signature);
        } catch (GeneralSecurityException ignored) {
            return false;
        }
    }

    private static Signature createSignature(PrivateKey privateKey, PublicKey publicKey)
            throws GeneralSecurityException {
        if (privateKey instanceof RSAPrivateKey && publicKey instanceof RSAPublicKey) {
            return Signature.getInstance("NONEwithRSA");
        }
        if (privateKey instanceof ECPrivateKey && publicKey instanceof ECPublicKey) {
            return Signature.getInstance("NONEwithECDSA");
        }
        if (privateKey instanceof DSAKey && publicKey instanceof DSAKey) {
            return Signature.getInstance("NONEwithDSA");
        }
        throw new InvalidKeySpecException("Key type must be one of " + SUPPORTED_KEY_TYPES);
    }

    public static byte[] base64Decode(String base64) {
        return getMimeDecoder().decode(base64.getBytes(US_ASCII));
    }
}
