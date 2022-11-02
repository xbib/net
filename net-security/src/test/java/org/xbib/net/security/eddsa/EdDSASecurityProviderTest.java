package org.xbib.net.security.eddsa;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class EdDSASecurityProviderTest {

    @Test
    public void canGetInstancesWhenProviderIsPresent() throws Exception {
        Security.addProvider(new EdDSASecurityProvider());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EdDSA", "EdDSA");
        KeyFactory keyFac = KeyFactory.getInstance("EdDSA", "EdDSA");
        Signature sgr = Signature.getInstance("NONEwithEdDSA", "EdDSA");
        Security.removeProvider("EdDSA");
    }

    @Test
    public void cannotGetInstancesWhenProviderIsNotPresent() throws Exception {
        Assertions.assertThrowsExactly(NoSuchProviderException.class, () -> {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EdDSA", "EdDSA");
        });
    }
}
