package org.xbib.net.security.eddsa.spec;

import org.xbib.net.security.eddsa.math.Curve;
import org.xbib.net.security.eddsa.math.GroupElement;
import org.xbib.net.security.eddsa.math.ScalarOps;

/**
 * EdDSA Curve specification that can also be referred to by name.
 */
public class EdDSANamedCurveSpec extends EdDSAParameterSpec {

    private final String name;

    public EdDSANamedCurveSpec(String name, Curve curve,
                               String hashAlgo, ScalarOps sc, GroupElement B) {
        super(curve, hashAlgo, sc, B);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
