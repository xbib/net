package org.xbib.net.security.eddsa;

import org.xbib.net.security.eddsa.spec.EdDSAParameterSpec;

/**
 * Common interface for all EdDSA keys.
 */
public interface EdDSAKey {
    /**
     * The reported key algorithm for all EdDSA keys
     */
    String KEY_ALGORITHM = "EdDSA";

    /**
     * @return a parameter specification representing the EdDSA domain
     * parameters for the key.
     */
    EdDSAParameterSpec getParams();
}
