package org.xbib.net.security;

import org.xbib.net.security.eddsa.EdDSAPrivateKey;
import org.xbib.net.security.eddsa.spec.EdDSANamedCurveTable;
import org.xbib.net.security.eddsa.spec.EdDSAPrivateKeySpec;
import org.xbib.net.security.util.Asn1Object;
import org.xbib.net.security.util.DerParser;
import org.xbib.net.security.util.DerUtils;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.ECField;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class for reading RSA private key from PEM formatted text.
 * It can read PEM files with PKCS#8 or PKCS#1 encodings.
 * It doesn't support encrypted PEM files.
 */
public class PrivateKeyReader {

    private static final byte[] BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] END_PRIVATE_KEY = "-----END PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] BEGIN_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] END_RSA_PRIVATE_KEY = "-----END RSA PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] BEGIN_DSA_PRIVATE_KEY = "-----BEGIN DSA PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] END_DSA_PRIVATE_KEY = "-----END DSA PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] BEGIN_EC_PRIVATE_KEY = "-----BEGIN EC PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] END_EC_PRIVATE_KEY = "-----END EC PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] BEGIN_OPENSSH_PRIVATE_KEY = "-----BEGIN OPENSSH PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] END_OPENSSH_PRIVATE_KEY = "-----END OPENSSH PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII);

    public PrivateKeyReader() {
    }

    public PrivateKey readPrivateKey(InputStream inputStream, String password)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException {
        Objects.requireNonNull(inputStream);
        byte[] key = inputStream.readAllBytes();
        if (indexOf(key, BEGIN_PRIVATE_KEY,0, key.length) >= 0) {
            byte[] keyBytes = extract(key, BEGIN_PRIVATE_KEY, END_PRIVATE_KEY);
            EncodedKeySpec keySpec = generateKeySpec(keyBytes, password != null ? password.toCharArray() : null);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        }
        if (indexOf(key, BEGIN_RSA_PRIVATE_KEY,0, key.length) >= 0) {
            byte[] keyBytes = extract(key, BEGIN_RSA_PRIVATE_KEY, END_RSA_PRIVATE_KEY);
            RSAPrivateCrtKeySpec keySpec = getRSAKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        }
        if (indexOf(key, BEGIN_DSA_PRIVATE_KEY,0, key.length) >= 0) {
            byte[] keyBytes = extract(key, BEGIN_DSA_PRIVATE_KEY, END_DSA_PRIVATE_KEY);
            DSAPrivateKeySpec keySpec = getDSAKeySpec(keyBytes);
            return KeyFactory.getInstance("DSA").generatePrivate(keySpec);
        }
        if (indexOf(key, BEGIN_EC_PRIVATE_KEY,0, key.length) >= 0) {
            byte[] keyBytes = extract(key, BEGIN_EC_PRIVATE_KEY, END_EC_PRIVATE_KEY);
            ECPrivateKeySpec keySpec = getECKeySpec(keyBytes);
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
        }
        if (indexOf(key, BEGIN_OPENSSH_PRIVATE_KEY,0, key.length) >= 0) {
            byte[] keyBytes = extract(key, BEGIN_OPENSSH_PRIVATE_KEY, END_OPENSSH_PRIVATE_KEY);
            byte[] sk = Arrays.copyOfRange(keyBytes, 0, 32);
            return new EdDSAPrivateKey(new EdDSAPrivateKeySpec(sk, EdDSANamedCurveTable.getByName("Ed25519")));
        }
        throw new IOException("invalid PEM");
    }

    /**
     * Convert PKCS#1 encoded private key into RSAPrivateCrtKeySpec.
     * The ASN.1 syntax for the private key with CRT is
     * <pre>
     * --
     * -- Representation of RSA private key with information for the CRT algorithm.
     * --
     * RSAPrivateKey ::= SEQUENCE {
     *   version           Version,
     *   modulus           INTEGER,  -- n
     *   publicExponent    INTEGER,  -- e
     *   privateExponent   INTEGER,  -- d
     *   prime1            INTEGER,  -- p
     *   prime2            INTEGER,  -- q
     *   exponent1         INTEGER,  -- d mod (p-1)
     *   exponent2         INTEGER,  -- d mod (q-1)
     *   coefficient       INTEGER,  -- (inverse of q) mod p
     *   otherPrimeInfos   OtherPrimeInfos OPTIONAL
     * }
     * </pre>
     *
     * @param keyBytes PKCS#1 encoded key
     * @return KeySpec
     * @throws IOException if failure
     */
    private static RSAPrivateCrtKeySpec getRSAKeySpec(byte[] keyBytes) throws IOException {
        DerParser parser = new DerParser(keyBytes);
        Asn1Object sequence = parser.read();
        if (sequence.getType() != DerParser.SEQUENCE) {
            throw new IOException("invalid DER: not a sequence");
        }
        parser = sequence.getParser();
        parser.read(); // skip version
        BigInteger modulus = parser.read().getInteger();
        BigInteger publicExp = parser.read().getInteger();
        BigInteger privateExp = parser.read().getInteger();
        BigInteger prime1 = parser.read().getInteger();
        BigInteger prime2 = parser.read().getInteger();
        BigInteger exp1 = parser.read().getInteger();
        BigInteger exp2 = parser.read().getInteger();
        BigInteger crtCoef = parser.read().getInteger();
        return new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
    }

    /**
     * Read DSA key in PKCS#1 spec.
     *
     * @param keyBytes PKCS#1 encoded key
     * @return DSA private key spec
     * @throws IOException if ASN.1 parsing fails
     */
    private DSAPrivateKeySpec getDSAKeySpec(byte[] keyBytes) throws IOException {
        DerParser parser = new DerParser(keyBytes);
        Asn1Object sequence = parser.read();
        if (sequence.getType() != DerParser.SEQUENCE) {
            throw new IOException("invalid DER: not a sequence");
        }
        parser = sequence.getParser();
        parser.read(); // skip version
        BigInteger p = parser.read().getInteger();
        BigInteger q = parser.read().getInteger();
        BigInteger g = parser.read().getInteger();
        BigInteger pub = parser.read().getInteger();
        BigInteger prv = parser.read().getInteger();
        return new DSAPrivateKeySpec(prv, p, q, g);
    }

    /**
     * Read EC private key in PKCS#1 format.
     *
     * ECPrivateKey ::= SEQUENCE {
     *          version       INTEGER { ecPrivkeyVer1(1) },
     *          privateKey    OCTET STRING,
     *          parameters    [0] EXPLICIT ECDomainParameters OPTIONAL,
     *          publicKey     [1] EXPLICIT BIT STRING OPTIONAL
     *   }
     *
     *  openssl asn1parse -i -in net-security/src/test/resources/ec.key
     *     0:d=0  hl=2 l= 119 cons: SEQUENCE
     *     2:d=1  hl=2 l=   1 prim:  INTEGER           :01
     *     5:d=1  hl=2 l=  32 prim:  OCTET STRING      [HEX DUMP]:7D9A378C22E17F85643D6D8B4EC14931220329FF5D03D20F4E15095BE40890F7
     *    39:d=1  hl=2 l=  10 cons:  cont [ 0 ]
     *    41:d=2  hl=2 l=   8 prim:   OBJECT            :prime256v1
     *    51:d=1  hl=2 l=  68 cons:  cont [ 1 ]
     *    53:d=2  hl=2 l=  66 prim:   BIT STRING
     *
     *  OID 1.2.840.10045.3.1.7 = "prime256v1"
     *
     * @param keyBytes PKCS#1 encoded key
     * @return the EC private key spec
     * @throws IOException if ASN.1 parsing fails
     */
    private ECPrivateKeySpec getECKeySpec(byte[] keyBytes) throws IOException {
        DerParser parser = new DerParser(keyBytes);
        Asn1Object sequence = parser.read();
        if (sequence.getType() != DerParser.SEQUENCE) {
            throw new IOException("invalid DER: not a sequence");
        }
        parser = sequence.getParser();
        parser.read(); // skip version
        byte[] privateKey = parser.read().getValue();
        Asn1Object asn1Object = parser.read();
        if (asn1Object.getType() != DerParser.ANY) {
            throw new IOException("invalid DER: not any: " + asn1Object.getType());
        }
        int[] oid = DerUtils.decodeOID(asn1Object.getValue());
        BigInteger bigInteger = new BigInteger(1, privateKey);
        String oidString = DerUtils.oidToString(oid);
        if (SECP256R1.getObjectId().equals(oidString)) {
            return new ECPrivateKeySpec(bigInteger, SECP256R1);
        } else if (SECP384R1.getObjectId().equals(oidString)) {
            return new ECPrivateKeySpec(bigInteger, SECP384R1);
        } else if (SECP521R1.getObjectId().equals(oidString)) {
            return new ECPrivateKeySpec(bigInteger, SECP521R1);
        } else {
            throw new IOException("invalid DER: unknown algo: " + oidString);
        }
    }

    private static final Curve SECP256R1 = initializeCurve(
            "secp256r1 [NIST P-256, X9.62 prime256v1]",
            "1.2.840.10045.3.1.7",
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF",
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC",
            "5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B",
            "6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296",
            "4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5",
            "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551",
            1
    );

    private static final Curve SECP384R1 = initializeCurve(
            "secp384r1 [NIST P-384]",
            "1.3.132.0.34",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFF",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFC",
            "B3312FA7E23EE7E4988E056BE3F82D19181D9C6EFE8141120314088F5013875AC656398D8A2ED19D2A85C8EDD3EC2AEF",
            "AA87CA22BE8B05378EB1C71EF320AD746E1D3B628BA79B9859F741E082542A385502F25DBF55296C3A545E3872760AB7",
            "3617DE4A96262C6F5D9E98BF9292DC29F8F41DBD289A147CE9DA3113B5F0B8C00A60B1CE1D7E819D7A431D7C90EA0E5F",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7634D81F4372DDF581A0DB248B0A77AECEC196ACCC52973",
            1
    );

    private static final Curve SECP521R1 = initializeCurve(
            "secp521r1 [NIST P-521]",
            "1.3.132.0.35",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC",
            "0051953EB9618E1C9A1F929A21A0B68540EEA2DA725B99B315F3B8B489918EF109E156193951EC7E937B1652C0BD3BB1BF073573DF883D2C34F1EF451FD46B503F00",
            "00C6858E06B70404E9CD9E3ECB662395B4429C648139053FB521F828AF606B4D3DBAA14B5E77EFE75928FE1DC127A2FFA8DE3348B3C1856A429BF97E7E31C2E5BD66",
            "011839296A789A3BC0045C8A5FB42C7D1BD998F54449579B446817AFBD17273E662C97EE72995EF42640C550B9013FAD0761353C7086A272C24088BE94769FD16650",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFA51868783BF2F966B7FCC0148F709A5D03BB5C9B8899C47AEBB6FB71E91386409",
            1
    );

    private static Curve initializeCurve(String name, String oid,
                                         String sfield, String a, String b,
                                         String x, String y, String n, int h) {
        BigInteger p = bigInt(sfield);
        ECField field = new ECFieldFp(p);
        EllipticCurve curve = new EllipticCurve(field, bigInt(a),bigInt(b));
        ECPoint g = new ECPoint(bigInt(x), bigInt(y));
        return new Curve(name, oid, curve, g, bigInt(n), h);
    }

    static final class Curve extends ECParameterSpec {
        private final String name;
        private final String oid;

        Curve(String name, String oid, EllipticCurve curve,
              ECPoint g, BigInteger n, int h) {
            super(curve, g, n, h);
            this.name = name;
            this.oid = oid;
        }
        private String getName() {
            return name;
        }
        private String getObjectId() {
            return oid;
        }
    }

    private static BigInteger bigInt(String s) {
        return new BigInteger(s, 16);
    }

    private static PKCS8EncodedKeySpec generateKeySpec(byte[] key, char[] password)
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        if (password == null) {
            return new PKCS8EncodedKeySpec(key);
        }
        EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);
        Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());
        return encryptedPrivateKeyInfo.getKeySpec(cipher);
    }

    private static int indexOf(byte[] array, byte[] target, int start, int end) {
        if (target.length == 0) {
            return 0;
        }
        outer:
        for (int i = start; i < end - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte[] extract(byte[] array, byte[] b1, byte[] b2) {
        int i1 = indexOf(array, b1, 0, array.length);
        if (i1 < 0) {
            throw new IllegalArgumentException("unable to extract: not found");
        }
        int i2 = indexOf(array, b2, 0, array.length);
        if (i2 < 0) {
            throw new IllegalArgumentException("unable to extract: not found");
        }
        int start = i1 + b1.length;
        byte[] b = new byte[i2 - start];
        System.arraycopy(array, start, b, 0, b.length);
        return Base64.getMimeDecoder().decode(b);
    }

    private static final String[] KEY_TYPES = {
            "RSA", "DSA", "EC"
    };

    private static final Pattern KEY_PATTERN =
            Pattern.compile("-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" +
                    "([a-z0-9+/=\\r\\n]+)" + "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", Pattern.CASE_INSENSITIVE);

    public static PrivateKey toPrivateKey(InputStream keyInputStream, String keyPassword)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, KeyException, IOException {
        if (keyInputStream == null) {
            return null;
        }
        return getPrivateKey(readPrivateKey(keyInputStream), keyPassword);
    }

    public static PrivateKey getPrivateKey(byte[] key, String keyPassword)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, KeyException, IOException {
        PKCS8EncodedKeySpec encodedKeySpec =
                generateKeySpec(key, keyPassword == null ? null : keyPassword.toCharArray());
        for (String keyType : KEY_TYPES) {
            try {
                return KeyFactory.getInstance(keyType).generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException e) {
                // ignore
            }
        }
        throw new InvalidKeySpecException("Neither RSA, DSA nor EC worked");
    }

    private static byte[] readPrivateKey(InputStream inputStream) throws KeyException, IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII))) {
            String string = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
            Matcher m = KEY_PATTERN.matcher(string);
            if (!m.find()) {
                throw new KeyException("could not find a PKCS #8 private key in input stream");
            }
            return Base64.getMimeDecoder().decode(m.group(1));
        }
    }
}
