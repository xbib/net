package org.xbib.net.security.eddsa.math.ed25519;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbib.net.security.eddsa.math.AbstractFieldElementTest;
import org.xbib.net.security.eddsa.math.Field;
import org.xbib.net.security.eddsa.math.FieldElement;
import org.xbib.net.security.eddsa.math.MathUtils;
import org.hamcrest.core.IsEqual;

import java.math.BigInteger;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests rely on the BigInteger class.
 */
public class Ed25519FieldElementTest extends AbstractFieldElementTest {

    protected FieldElement getRandomFieldElement() {
        return MathUtils.getRandomFieldElement();
    }

    protected BigInteger toBigInteger(FieldElement f) {
        return MathUtils.toBigInteger(f);
    }

    protected BigInteger getQ() {
        return MathUtils.getQ();
    }

    protected Field getField() {
        return MathUtils.getField();
    }

    @Test
    public void canConstructFieldElementFromArrayWithCorrectLength() {
        new Ed25519FieldElement(MathUtils.getField(), new int[10]);
    }

    @Test
    public void cannotConstructFieldElementFromArrayWithIncorrectLength() {
        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
            new Ed25519FieldElement(MathUtils.getField(), new int[9]);
        });
    }

    @Test
    public void cannotConstructFieldElementWithoutField() {
        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
            new Ed25519FieldElement(null, new int[9]);
        });
    }

    protected FieldElement getZeroFieldElement() {
        return new Ed25519FieldElement(MathUtils.getField(), new int[10]);
    }

    protected FieldElement getNonZeroFieldElement() {
        final int[] t = new int[10];
        t[0] = 5;
        return new Ed25519FieldElement(MathUtils.getField(), t);
    }

    @Test
    public void toStringReturnsCorrectRepresentation() {
        final byte[] bytes = new byte[32];
        for (int i=0; i<32; i++) {
            bytes[i] = (byte)(i+1);
        }
        final FieldElement f = MathUtils.getField().getEncoding().decode(bytes);

        final String fAsString = f.toString();
        final StringBuilder builder = new StringBuilder();
        builder.append("[Ed25519FieldElement val=");
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        builder.append("]");

        assertThat(fAsString, IsEqual.equalTo(builder.toString()));
    }

}
