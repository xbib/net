package org.xbib.net.security.eddsa;

import java.security.Provider;
import java.security.Security;

/**
 * A security {@link Provider} that can be registered via {@link Security#addProvider(Provider)}.
 */
@SuppressWarnings("serial")
public class EdDSASecurityProvider extends Provider {

    public static final String PROVIDER_NAME = "EdDSA";

    public EdDSASecurityProvider() {
        super(PROVIDER_NAME, "0.1", "xbib " + PROVIDER_NAME + " security provider wrapper");
        setup();
    }

    protected void setup() {
        // See https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/HowToImplAProvider.html
        put("KeyFactory." + EdDSAKey.KEY_ALGORITHM, "org.xbib.net.security.eddsa.KeyFactory");
        put("KeyPairGenerator." + EdDSAKey.KEY_ALGORITHM, "org.xbib.net.security.eddsa.KeyPairGenerator");
        put("Signature." + EdDSAEngine.SIGNATURE_ALGORITHM, "org.xbib.net.security.eddsa.EdDSAEngine");

        // OID Mappings
        // See section "Mapping from OID to name".
        // The Key* -> OID mappings correspond to the default algorithm in KeyPairGenerator.
        //
        // From draft-ieft-curdle-pkix-04:
        //   id-Ed25519   OBJECT IDENTIFIER ::= { 1 3 101 112 }
        put("Alg.Alias.KeyFactory.1.3.101.112", EdDSAKey.KEY_ALGORITHM);
        put("Alg.Alias.KeyFactory.OID.1.3.101.112", EdDSAKey.KEY_ALGORITHM);
        put("Alg.Alias.KeyPairGenerator.1.3.101.112", EdDSAKey.KEY_ALGORITHM);
        put("Alg.Alias.KeyPairGenerator.OID.1.3.101.112", EdDSAKey.KEY_ALGORITHM);
        put("Alg.Alias.Signature.1.3.101.112", EdDSAEngine.SIGNATURE_ALGORITHM);
        put("Alg.Alias.Signature.OID.1.3.101.112", EdDSAEngine.SIGNATURE_ALGORITHM);
    }
}
