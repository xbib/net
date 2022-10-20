package org.xbib.net.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

public class DerParser {

    public final static int UNIVERSAL = 0x00;
    public final static int APPLICATION = 0x40;
    public final static int CONTEXT = 0x80;
    public final static int PRIVATE = 0xC0;

    public final static int CONSTRUCTED = 0x20;

    public final static int ANY = 0x00;
    public final static int BOOLEAN = 0x01;
    public final static int INTEGER = 0x02;
    public final static int BIT_STRING = 0x03;
    public final static int OCTET_STRING = 0x04;
    public final static int NULL = 0x05;
    public final static int OBJECT_IDENTIFIER = 0x06;
    public final static int REAL = 0x09;
    public final static int ENUMERATED = 0x0a;
    public final static int RELATIVE_OID = 0x0d;

    public final static int SEQUENCE = 0x10;
    public final static int SET = 0x11;

    public final static int NUMERIC_STRING = 0x12;
    public final static int PRINTABLE_STRING = 0x13;
    public final static int T61_STRING = 0x14;
    public final static int VIDEOTEX_STRING = 0x15;
    public final static int IA5_STRING = 0x16;
    public final static int GRAPHIC_STRING = 0x19;
    public final static int ISO646_STRING = 0x1A;
    public final static int GENERAL_STRING = 0x1B;

    public final static int UTF8_STRING = 0x0C;
    public final static int UNIVERSAL_STRING = 0x1C;
    public final static int BMP_STRING = 0x1E;

    public final static int UTC_TIME = 0x17;
    public final static int GENERALIZED_TIME = 0x18;

    private final InputStream inputStream;

    public DerParser(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
    }

    public DerParser(byte[] bytes) throws IOException {
        this(new ByteArrayInputStream(bytes));
    }

    public Asn1Object read() throws IOException {
        int tag = inputStream.read();
        if (tag == -1) {
            throw new IOException("Invalid DER: stream too short, missing tag"); //$NON-NLS-1$
        }
        int length = getLength();
        byte[] value = new byte[length];
        int n = inputStream.read(value);
        if (n < length) {
            throw new IOException("Invalid DER: stream too short, missing value"); //$NON-NLS-1$
        }
        return new Asn1Object(tag, length, value);
    }

    /**
     * Decode the length of the field. Can only support length
     * encoding up to 4 octets.
     * <p>
     * <p/>In BER/DER encoding, length can be encoded in 2 forms,
     * <ul>
     * <li>Short form. One octet. Bit 8 has value "0" and bits 7-1
     * give the length.
     * <li>Long form. Two to 127 octets (only 4 is supported here).
     * Bit 8 of first octet has value "1" and bits 7-1 give the
     * number of additional length octets. Second and following
     * octets give the length, base 256, most significant digit first.
     * </ul>
     *
     * @return The length as integer
     * @throws IOException if reading fails
     */
    private int getLength() throws IOException {
        int i = inputStream.read();
        if (i == -1) {
            throw new IOException("Invalid DER: length missing");
        }
        if ((i & ~0x7F) == 0) {
            return i;
        }
        int num = i & 0x7F;
        if (i >= 0xFF || num > 4) {
            throw new IOException("Invalid DER: length field too big (" + i + ")");
        }
        byte[] bytes = new byte[num];
        int n = inputStream.read(bytes);
        if (n < num) {
            throw new IOException("Invalid DER: length too short");
        }
        return new BigInteger(1, bytes).intValue();
    }
}
