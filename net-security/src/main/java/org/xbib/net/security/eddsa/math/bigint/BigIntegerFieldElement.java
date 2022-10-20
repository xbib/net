package org.xbib.net.security.eddsa.math.bigint;

import org.xbib.net.security.eddsa.math.Field;
import org.xbib.net.security.eddsa.math.FieldElement;

import java.math.BigInteger;

/**
 * A particular element of the field \Z/(2^255-19).
 */
public class BigIntegerFieldElement extends FieldElement {
    /**
     * Variable is package private for encoding.
     */
    final BigInteger bi;

    public BigIntegerFieldElement(Field f, BigInteger bi) {
        super(f);
        this.bi = bi;
    }

    public boolean isNonZero() {
        return !bi.equals(BigInteger.ZERO);
    }

    public FieldElement add(FieldElement val) {
        return new BigIntegerFieldElement(f, bi.add(((BigIntegerFieldElement) val).bi)).mod(f.getQ());
    }

    @Override
    public FieldElement addOne() {
        return new BigIntegerFieldElement(f, bi.add(BigInteger.ONE)).mod(f.getQ());
    }

    public FieldElement subtract(FieldElement val) {
        return new BigIntegerFieldElement(f, bi.subtract(((BigIntegerFieldElement) val).bi)).mod(f.getQ());
    }

    @Override
    public FieldElement subtractOne() {
        return new BigIntegerFieldElement(f, bi.subtract(BigInteger.ONE)).mod(f.getQ());
    }

    public FieldElement negate() {
        return f.getQ().subtract(this);
    }

    @Override
    public FieldElement divide(FieldElement val) {
        return divide(((BigIntegerFieldElement) val).bi);
    }

    public FieldElement divide(BigInteger val) {
        return new BigIntegerFieldElement(f, bi.divide(val)).mod(f.getQ());
    }

    public FieldElement multiply(FieldElement val) {
        return new BigIntegerFieldElement(f, bi.multiply(((BigIntegerFieldElement) val).bi)).mod(f.getQ());
    }

    public FieldElement square() {
        return multiply(this);
    }

    public FieldElement squareAndDouble() {
        FieldElement sq = square();
        return sq.add(sq);
    }

    public FieldElement invert() {
        // Euler's theorem
        //return modPow(f.getQm2(), f.getQ());
        return new BigIntegerFieldElement(f, bi.modInverse(((BigIntegerFieldElement) f.getQ()).bi));
    }

    public FieldElement mod(FieldElement m) {
        return new BigIntegerFieldElement(f, bi.mod(((BigIntegerFieldElement) m).bi));
    }

    public FieldElement modPow(FieldElement e, FieldElement m) {
        return new BigIntegerFieldElement(f, bi.modPow(((BigIntegerFieldElement) e).bi, ((BigIntegerFieldElement) m).bi));
    }

    public FieldElement pow(FieldElement e) {
        return modPow(e, f.getQ());
    }

    public FieldElement pow22523() {
        return pow(f.getQm5d8());
    }

    @Override
    public FieldElement cmov(FieldElement val, int b) {
        // Not constant-time, but it doesn't really matter because none of the underlying BigInteger operations
        // are either, so there's not much point in trying hard here ...
        return b == 0 ? this : val;
    }

    @Override
    public int hashCode() {
        return bi.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BigIntegerFieldElement))
            return false;
        BigIntegerFieldElement fe = (BigIntegerFieldElement) obj;
        return bi.equals(fe.bi);
    }

    @Override
    public String toString() {
        return "[BigIntegerFieldElement val=" + bi + "]";
    }
}
