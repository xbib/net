package org.xbib.net.security.ed25519;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import org.junit.jupiter.api.Test;

public class Ed25519KeyTest {

    @Test
    public void generate() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(kp.getPrivate());
        sig.update("Hello Jörg".getBytes(StandardCharsets.UTF_8));
        byte[] s = sig.sign();
    }
}
