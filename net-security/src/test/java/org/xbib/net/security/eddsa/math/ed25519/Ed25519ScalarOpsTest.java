package org.xbib.net.security.eddsa.math.ed25519;

import org.junit.jupiter.api.Test;
import org.xbib.net.security.eddsa.Utils;
import org.xbib.net.security.eddsa.math.MathUtils;
import org.hamcrest.core.IsEqual;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Additional tests by the NEM project team.
 *
 */
public class Ed25519ScalarOpsTest {

    private static final Ed25519ScalarOps scalarOps = new Ed25519ScalarOps();

    /**
     * Test method for {@link org.xbib.net.security.eddsa.math.bigint.BigIntegerScalarOps#reduce(byte[])}.
     */
    @Test
    public void testReduce() {
        // Example from test case 1
        byte[] r = Utils.hexToBytes("b6b19cd8e0426f5983fa112d89a143aa97dab8bc5deb8d5b6253c928b65272f4044098c2a990039cde5b6a4818df0bfb6e40dc5dee54248032962323e701352d");
        assertThat(scalarOps.reduce(r), is(equalTo(Utils.hexToBytes("f38907308c893deaf244787db4af53682249107418afc2edc58f75ac58a07404"))));
    }

    @Test
    public void reduceReturnsExpectedResult() {
        for (int i=0; i<1000; i++) {
            final byte[] bytes = MathUtils.getRandomByteArray(64);
            final byte[] reduced1 = scalarOps.reduce(bytes);
            final byte[] reduced2 = MathUtils.reduceModGroupOrder(bytes);
            assertThat(MathUtils.toBigInteger(reduced1).compareTo(MathUtils.getGroupOrder()), IsEqual.equalTo(-1));
            assertThat(MathUtils.toBigInteger(reduced1).compareTo(new BigInteger("-1")), IsEqual.equalTo(1));
            assertThat(reduced1, IsEqual.equalTo(reduced2));
        }
    }

    /**
     * Test method for {@link org.xbib.net.security.eddsa.math.bigint.BigIntegerScalarOps#multiplyAndAdd(byte[], byte[], byte[])}.
     */
    @Test
    public void testMultiplyAndAdd() {
        byte[] h = Utils.hexToBytes("86eabc8e4c96193d290504e7c600df6cf8d8256131ec2c138a3e7e162e525404");
        byte[] a = Utils.hexToBytes("307c83864f2833cb427a2ef1c00a013cfdff2768d980c0a3a520f006904de94f");
        byte[] r = Utils.hexToBytes("f38907308c893deaf244787db4af53682249107418afc2edc58f75ac58a07404");
        byte[] S = Utils.hexToBytes("5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");
        assertThat(scalarOps.multiplyAndAdd(h, a, r), is(equalTo(S)));
    }

    @Test
    public void multiplyAndAddReturnsExpectedResult() {
        for (int i=0; i<1000; i++) {
            final byte[] bytes1 = MathUtils.getRandomByteArray(32);
            final byte[] bytes2 = MathUtils.getRandomByteArray(32);
            final byte[] bytes3 = MathUtils.getRandomByteArray(32);

            final byte[] result1 = scalarOps.multiplyAndAdd(bytes1, bytes2, bytes3);
            final byte[] result2 = MathUtils.multiplyAndAddModGroupOrder(bytes1, bytes2, bytes3);

            assertThat(MathUtils.toBigInteger(result1).compareTo(MathUtils.getGroupOrder()), IsEqual.equalTo(-1));
            assertThat(MathUtils.toBigInteger(result1).compareTo(new BigInteger("-1")), IsEqual.equalTo(1));
            assertThat(result1, IsEqual.equalTo(result2));
        }
    }
}
