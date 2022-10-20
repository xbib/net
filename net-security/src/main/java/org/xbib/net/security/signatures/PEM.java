package org.xbib.net.security.signatures;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

/**
 * A PEM utility that can be used to read keys from PEM. With this PEM utility,
 * private keys in either PKCS#1 or PKCS#8 PEM encoded format can be read
 * without the need to depend on the Bouncy Castle library.
 * <p>
 * Some background information:
 * <ul>
 * <li>Interestingly, the creation of a CloudFront Key Pair via the AWS console
 * would result in a private key in PKCS#1 PEM format.</li>
 * <li>Unfortunately, the JDK doesn't provide a means to load PEM key encoded in
 * PKCS#1 without adding the Bouncy Castle to the classpath. The JDK can only
 * load PEM key encoded in PKCS#8 encoding.</li>
 * <li>One the other hand, one can use openssl to convert a PEM file from PKCS#1
 * to PKCS#8. Example:
 *
 * <pre>
 * openssl pkcs8 -topk8 -in pk-APKAJM22QV32R3I2XVIQ.pem -inform pem -out pk-APKAJM22QV32R3I2XVIQ_pk8.pem  -outform pem -nocrypt
 * </pre>
 *
 * </li>
 * </ul>
 */
public enum PEM {
    ;
    private static final String BEGIN_MARKER = "-----BEGIN ";

    /**
     * Returns the first private key that is found from the input stream of a
     * PEM file.
     *
     * @param is Inputstream to read a private key from
     * @return the first PrivateKey found in the stream
     * @throws InvalidKeySpecException  if failed to convert the DER bytes into a private key.
     * @throws IllegalArgumentException if no private key is found.
     * @throws IOException              if an IO exception occurs while reading the stream
     */
    public static PrivateKey readPrivateKey(final InputStream is) throws InvalidKeySpecException, IOException {
        final List<PEMObject> objects = readPEMObjects(is);
        for (final PEMObject object : objects) {
            switch (object.getPEMObjectType()) {
                case PRIVATE_KEY_PKCS1:
                    return RSA.privateKeyFromPKCS1(object.getDerBytes());
                case PRIVATE_EC_KEY_PKCS8:
                    return EC.privateKeyFromPKCS8(object.getDerBytes());
                case PRIVATE_KEY_PKCS8:
                    try {
                        return RSA.privateKeyFromPKCS8(object.getDerBytes());
                    } catch (final InvalidKeySpecException e) {
                        return EC.privateKeyFromPKCS8(object.getDerBytes());
                    }
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("Found no private key");
    }

    /**
     * Returns the first public key that is found from the input stream of a PEM
     * file.
     *
     * @param is The Input stream to read
     * @return the first PublicKey found in the stream
     * @throws InvalidKeySpecException  if failed to convert the DER bytes into a public key.
     * @throws IllegalArgumentException if no public key is found.
     * @throws IOException              if an IO exception occurs while reading the stream
     */
    public static PublicKey readPublicKey(final InputStream is) throws InvalidKeySpecException, IOException {
        for (final PEMObject object : readPEMObjects(is)) {
            if (object.getPEMObjectType() == PEMObjectType.PUBLIC_KEY_X509) {
                try {
                    return RSA.publicKeyFrom(object.getDerBytes());
                } catch (final InvalidKeySpecException e) {
                    return EC.publicKeyFrom(object.getDerBytes());
                }
            }
        }
        throw new IllegalArgumentException("Found no public key");
    }

    /**
     * A lower level API used to returns all PEM objects that can be read off
     * from the input stream of a PEM file.
     * <p>
     * This method can be useful if more than one PEM object of different types
     * are embedded in the same PEM file.
     */
    private static List<PEMObject> readPEMObjects(final InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            final List<PEMObject> pemContents = new ArrayList<>();
            boolean readingContent = false;
            String beginMarker = null;
            String endMarker = null;
            StringBuilder sb = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (readingContent) {
                    if (line.contains(endMarker)) {
                        pemContents.add(new PEMObject(beginMarker, Base64.decodeBase64(sb.toString().getBytes(StandardCharsets.US_ASCII))));
                        readingContent = false;
                    } else {
                        sb.append(line.trim());
                    }
                } else {
                    if (line.contains(BEGIN_MARKER)) {
                        readingContent = true;
                        beginMarker = line.trim();
                        endMarker = beginMarker.replace("BEGIN", "END");
                        sb = new StringBuilder();
                    }
                }
            }
            return pemContents;
        }
    }

    /**
     * The type of a specific PEM object in a PEM file.
     * <p>
     * A PEM file can contain one or multiple PEM objects, each with a beginning
     * and ending marker.
     */
    enum PEMObjectType {
        PRIVATE_KEY_PKCS1("-----BEGIN RSA PRIVATE KEY-----"),
        PRIVATE_EC_KEY_PKCS8("-----BEGIN EC PRIVATE KEY-----"), // RFC-5915
        PRIVATE_KEY_PKCS8("-----BEGIN PRIVATE KEY-----"),
        PUBLIC_KEY_X509("-----BEGIN PUBLIC KEY-----"),
        CERTIFICATE_X509("-----BEGIN CERTIFICATE-----");
        private final String beginMarker;

        PEMObjectType(final String beginMarker) {
            this.beginMarker = beginMarker;
        }

        public static PEMObjectType fromBeginMarker(final String beginMarker) {
            for (final PEMObjectType e : PEMObjectType.values()) {
                if (e.getBeginMarker().equals(beginMarker)) {
                    return e;
                }
            }
            return null;
        }

        public String getBeginMarker() {
            return beginMarker;
        }
    }

    /**
     * A PEM object in a PEM file.
     * <p>
     * A PEM file can contain one or multiple PEM objects, each with a beginning
     * and ending marker.
     */
    static class PEMObject {
        private final String beginMarker;
        private final byte[] derBytes;

        public PEMObject(final String beginMarker, final byte[] derBytes) {
            this.beginMarker = beginMarker;
            this.derBytes = derBytes.clone();
        }

        public String getBeginMarker() {
            return beginMarker;
        }

        public byte[] getDerBytes() {
            return derBytes.clone();
        }

        public PEMObjectType getPEMObjectType() {
            return PEMObjectType.fromBeginMarker(beginMarker);
        }
    }
}
