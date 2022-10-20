package org.xbib.net.security.eddsa;

import org.xbib.net.security.eddsa.spec.EdDSAGenParameterSpec;
import org.xbib.net.security.eddsa.spec.EdDSANamedCurveSpec;
import org.xbib.net.security.eddsa.spec.EdDSANamedCurveTable;
import org.xbib.net.security.eddsa.spec.EdDSAParameterSpec;
import org.xbib.net.security.eddsa.spec.EdDSAPrivateKeySpec;
import org.xbib.net.security.eddsa.spec.EdDSAPublicKeySpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Hashtable;

/**
 * Default keysize is 256 (Ed25519).
 */
public final class KeyPairGenerator extends KeyPairGeneratorSpi {

    private static final int DEFAULT_KEYSIZE = 256;

    private static final Hashtable<Integer, AlgorithmParameterSpec> edParameters;

    static {
        edParameters = new Hashtable<>();
        edParameters.put(256, new EdDSAGenParameterSpec("Ed25519"));
    }

    private EdDSAParameterSpec edParams;

    private SecureRandom random;

    private boolean initialized;

    public KeyPairGenerator() {
    }

    public void initialize(int keysize, SecureRandom random) {
        AlgorithmParameterSpec edParams = edParameters.get(keysize);
        if (edParams == null)
            throw new InvalidParameterException("unknown key type.");
        try {
            initialize(edParams, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidParameterException("key type not configurable.");
        }
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        if (params instanceof EdDSAParameterSpec) {
            edParams = (EdDSAParameterSpec) params;
        } else if (params instanceof EdDSAGenParameterSpec) {
            edParams = createNamedCurveSpec(((EdDSAGenParameterSpec) params).getName());
        } else
            throw new InvalidAlgorithmParameterException("parameter object not a EdDSAParameterSpec");

        this.random = random;
        initialized = true;
    }

    public KeyPair generateKeyPair() {
        if (!initialized)
            initialize(DEFAULT_KEYSIZE, new SecureRandom());

        byte[] seed = new byte[edParams.getCurve().getField().getb() / 8];
        random.nextBytes(seed);

        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(seed, edParams);
        EdDSAPublicKeySpec pubKey = new EdDSAPublicKeySpec(privKey.getA(), edParams);

        return new KeyPair(new EdDSAPublicKey(pubKey), new EdDSAPrivateKey(privKey));
    }

    /**
     * Create an EdDSANamedCurveSpec from the provided curve name. The current
     * implementation fetches the pre-created curve spec from a table.
     *
     * @param curveName the EdDSA named curve.
     * @return the specification for the named curve.
     * @throws InvalidAlgorithmParameterException if the named curve is unknown.
     */
    protected EdDSANamedCurveSpec createNamedCurveSpec(String curveName) throws InvalidAlgorithmParameterException {
        EdDSANamedCurveSpec spec = EdDSANamedCurveTable.getByName(curveName);
        if (spec == null) {
            throw new InvalidAlgorithmParameterException("unknown curve name: " + curveName);
        }
        return spec;
    }
}
