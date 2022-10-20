package org.xbib.net.security;

import org.xbib.net.security.util.DistinguishedNameParser;

import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class CertificateReader {

    private static final String BEGIN_MARKER = "-----BEGIN CERTIFICATE-----";

    private static final String END_MARKER = "-----END CERTIFICATE-----";

    private static final PrivateKeyReader privateKeyReader = new PrivateKeyReader();

    public CertificateReader() {
    }

    public PrivateKey providePrivateKey(InputStream pem, String password)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException {
        return privateKeyReader.readPrivateKey(pem, password);
    }

    public X509Certificate readCertificate(String pem) throws CertificateException, IOException {
        return readCertificate(new ByteArrayInputStream(readMaterial(pem, BEGIN_MARKER, END_MARKER)));
    }

    public X509Certificate readCertificate(InputStream pem) throws CertificateException, IOException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(pem);
    }

    public X509Certificate readCertificate(byte[] der) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
    }

    public String getCertificateInPemFormat(X509Certificate certificate) throws CertificateEncodingException {
        return derToPem(certificate.getEncoded(), BEGIN_MARKER, END_MARKER);
    }

    public byte[] getCertificateInDerFormat(X509Certificate certificate) throws CertificateEncodingException {
        return certificate.getEncoded();
    }

    public BigInteger getModulus(X509Certificate certificate) {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) certificate.getPublicKey();
        return rsaPublicKey.getModulus();
    }

    public String getSha1Fingerprint(X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(getCertificateInDerFormat(certificate));
        return toHex(sha1.digest());
    }

    public String getSha256Fingerprint(X509Certificate certificate) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(getCertificateInDerFormat(certificate));
        return toHex(sha256.digest());
    }

    @SuppressWarnings("unchecked")
    public Collection<? extends X509Certificate> readChain(InputStream keyCertChainInputStream)
            throws CertificateException {
        return (Collection<? extends X509Certificate>) CertificateFactory.getInstance("X509")
                .generateCertificates(keyCertChainInputStream);
    }

    public static String getServerName(X509Certificate certificate) {
        if (certificate == null) {
            return null;
        }
        return new DistinguishedNameParser(certificate.getSubjectX500Principal())
                .findMostSpecific("CN");
    }

    public static List<String> getAlternativeServerNames(X509Certificate certificate)
            throws CertificateParsingException {
        List<String> list = new ArrayList<>();
        Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
        if (altNames != null) {
            for (List<?> altName : altNames) {
                Integer type = (Integer) altName.get(0);
                if (type == 2) { // Type = DNS
                    String string = altName.get(1).toString();
                    list.add(string);
                }
            }
        }
        return list;
    }

    public static List<X509Certificate> parseCertificateChain(String pemChain)
            throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        List<X509Certificate> list = new ArrayList<>();
        String[] pemCerts = pemChain.split(END_MARKER);
        for (String pemCert : pemCerts) {
            CertificateReader certificateReader = new CertificateReader();
            X509Certificate certificate = certificateReader.readCertificate(pemCert + END_MARKER);
            list.add(certificate);
        }
        return orderCertificateChain(list);
    }

    public static List<X509Certificate> orderCertificateChain(Collection<? extends X509Certificate> chain)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        List<X509Certificate> unorderedCertificateList = new ArrayList<>(chain);
        List<X509Certificate> orderedCertificateList = new ArrayList<>();
        X509Certificate topCertificate = findTopCertificate(unorderedCertificateList);
        orderedCertificateList.add(topCertificate);
        unorderedCertificateList.remove(topCertificate);
        int count = unorderedCertificateList.size();
        for (int i = 0; i < count; i++) {
            X509Certificate nextCertificate = findNextCertificate(orderedCertificateList.get(0), unorderedCertificateList);
            orderedCertificateList.add(0, nextCertificate);
            unorderedCertificateList.remove(nextCertificate);
        }
        return orderedCertificateList;
    }

    private static X509Certificate findNextCertificate(X509Certificate certificate, Collection<? extends X509Certificate> certificateList)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        for (X509Certificate currentCert : certificateList) {
            try {
                currentCert.verify(certificate.getPublicKey());
                return currentCert;
            } catch (SignatureException e) {
                // skip
            }
        }
        throw new CertificateException("chain doesn't contain a certificate that was signed by " + certificate);
    }

    private static X509Certificate findTopCertificate(Collection<? extends X509Certificate> certificateList)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        for (X509Certificate currentCert : certificateList) {
            boolean signerFound = false;
            for (X509Certificate certificate : certificateList) {
                if (currentCert != certificate) {
                    try {
                        currentCert.verify(certificate.getPublicKey());
                        signerFound = true;
                        break;
                    } catch (SignatureException e) {
                        //
                    }
                }
            }
            if (!signerFound) {
                return currentCert;
            }
        }
        throw new CertificateException("could not find the top certificate of the chain");
    }

    private byte[] readMaterial(String base64WithMarkers, String beginMarker, String endMarker) throws IOException {
        String line;
        StringBuilder sb = new StringBuilder();
        Scanner scanner = new Scanner(base64WithMarkers);
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (line.contains(beginMarker)) {
                continue;
            }
            if (line.contains(endMarker)) {
                return Base64.getMimeDecoder().decode(sb.toString());
            }
            sb.append(line.trim());
        }
        throw new IOException("Invalid PEM file: No end marker");
    }

    private static String derToPem(byte[] der, String beginMarker, String endMarker) {
        String base64 = Base64.getEncoder().encodeToString(der);
        String[] lines = base64.split("(?<=\\G.{64})");
        StringBuilder result = new StringBuilder(beginMarker + "\n");
        for (String line : lines) {
            result.append(line).append("\n");
        }
        result.append(endMarker);
        return result.toString();
    }

    private static String toHex(byte[] bytes) {
        return String.format("%0" + (bytes.length << 1) + "X", new BigInteger(1, bytes));
    }
}
