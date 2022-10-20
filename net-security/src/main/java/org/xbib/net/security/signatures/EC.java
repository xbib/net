package org.xbib.net.security.signatures;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utility for Elliptic Curve keys.
 */
public class EC {

    private static final String EC = "EC";

    private static final String SUNEC = "SunEC"; // Sun's ECC provider

    private EC() {
    }

    /**
     * Returns a private key constructed from the given DER bytes in PKCS#8 format.
     *
     * @param pkcs8 DER bytes in PKCS#8 format
     * @return Private key
     * @throws InvalidKeySpecException if the DER bytes cannot be converted to a private key
     */
    public static PrivateKey privateKeyFromPKCS8(final byte[] pkcs8) throws InvalidKeySpecException {
        try {
            final EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pkcs8);
            final KeyFactory keyFactory = KeyFactory.getInstance(EC, SUNEC);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (final NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a public key constructed from the given DER bytes.
     *
     * @param derBytes DER bytes
     * @return Public key
     * @throws InvalidKeySpecException if the DER bytes cannot be converted to a public key
     */
    public static PublicKey publicKeyFrom(final byte[] derBytes) throws InvalidKeySpecException {
        try {
            final KeyFactory keyFactory = KeyFactory.getInstance(EC, SUNEC);
            final EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(derBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (final NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new IllegalStateException(e);
        }
    }
}
