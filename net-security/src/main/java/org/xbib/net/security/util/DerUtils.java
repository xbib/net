package org.xbib.net.security.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * ASN.1 DER encoder methods necessary to process PEM files and to write a certificate signing request.
 * NOTE: this API is only present for the two mentioned use cases, and is subject to change without warning.
 */
public final class DerUtils {
    public static final int SEQUENCE_TAG = 0x30;
    public static final int BOOLEAN_TAG = 0x01;
    public static final int INTEGER_TAG = 0x02;
    public static final int BIT_STRING_TAG = 0x03;
    public static final int OCTET_STRING_TAG = 0x04;
    public static final int NULL_TAG = 0x05;
    public static final int OBJECT_IDENTIFIER_TAG = 0x06;
    public static final int UTC_TIME_TAG = 0x17;
    private static final DateTimeFormatter UTC_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyMMddHHmmssX").withZone(ZoneOffset.UTC);

    private DerUtils() {
    }

    /**
     * Encodes a sequence of encoded values.
     */
    public static byte[] encodeSequence(byte[]... encodedValues) throws IOException {
        return encodeConstructed(SEQUENCE_TAG, encodedValues);
    }

    /**
     * Decodes a sequence of encoded values.
     */
    public static List<byte[]> decodeSequence(byte[] sequence) {
        int index = 0;
        //checkArgument(sequence[0] == SEQUENCE_TAG, "Expected sequence tag");
        index++;
        int sequenceDataLength = decodeLength(sequence, index);
        index += encodedLengthSize(sequenceDataLength);
        //checkArgument(sequenceDataLength + index == sequence.length, "Invalid sequence");
        List<byte[]> elements = new ArrayList<>();
        while (index < sequence.length) {
            int elementStart = index;
            index++;
            int length = decodeLength(sequence, index);
            index += encodedLengthSize(length);
            byte[] data = Arrays.copyOfRange(sequence, elementStart, index + length);
            elements.add(data);
            index += length;
        }
        return elements;
    }

    /**
     * Decodes a optional element of a sequence.
     */
    public static byte[] decodeSequenceOptionalElement(byte[] element) {
        int index = 0;
        //checkArgument((element[0] & 0xE0) == 0xA0, "Expected optional sequence element tag");
        index++;
        int length = decodeLength(element, index);
        index += encodedLengthSize(length);
        //checkArgument(length + index == element.length, "Invalid optional sequence element");
        return Arrays.copyOfRange(element, index, index + length);
    }

    /**
     * Encodes a bit string padded with the specified number of bits.
     * The encoding is a byte containing the padBits followed by the value bytes.
     */
    public static byte[] encodeBitString(int padBits, byte[] value) throws IOException {
        //checkArgument(padBits >= 0 && padBits < 8, "Invalid pad bits");
        byte[] lengthEncoded = encodeLength(value.length + 1);
        ByteArrayOutputStream out = new ByteArrayOutputStream(2 + lengthEncoded.length + value.length);
        out.write(BIT_STRING_TAG);
        out.write(lengthEncoded);
        out.write(padBits);
        out.write(value);
        return out.toByteArray();
    }

    /**
     * Encodes an integer.
     */
    public static byte[] encodeBooleanTrue() {
        return new byte[]{BOOLEAN_TAG, 0x01, (byte) 0xFF};
    }

    /**
     * Encodes an integer.
     */
    public static byte[] encodeInteger(long value) throws IOException {
        return encodeInteger(BigInteger.valueOf(value));
    }

    /**
     * Encodes an integer.
     */
    public static byte[] encodeInteger(BigInteger value) throws IOException {
        return encodeTag(INTEGER_TAG, value.toByteArray());
    }

    /**
     * Encodes an octet string.
     */
    public static byte[] encodeOctetString(byte[] value) throws IOException {
        return encodeTag(OCTET_STRING_TAG, value);
    }

    /**
     * Encodes an octet string.
     */
    public static byte[] encodeUtcTime(String value) throws IOException {
        return encodeTag(UTC_TIME_TAG, value.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Encodes an octet string.
     */
    public static byte[] encodeUtcTime(Instant value) throws IOException {
        String utcTime = UTC_TIME_FORMATTER.format(value);
        return encodeTag(UTC_TIME_TAG, utcTime.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Encodes the length of a DER value.  The encoding of a 7bit value is simply the value.  Values needing more than 7bits
     * are encoded as a lead byte with the high bit set and containing the number of value bytes.  Then the following bytes
     * encode the length using the least number of bytes possible.
     */
    public static byte[] encodeLength(int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        int numberOfBits = 32 - Integer.numberOfLeadingZeros(length);
        int numberOfBytes = (numberOfBits + 7) / 8;
        byte[] encoded = new byte[1 + numberOfBytes];
        encoded[0] = (byte) (numberOfBytes | 0x80);
        for (int i = 0; i < numberOfBytes; i++) {
            int byteToEncode = (numberOfBytes - i);
            int shiftSize = (byteToEncode - 1) * 8;
            encoded[i + 1] = (byte) (length >>> shiftSize);
        }
        return encoded;
    }

    private static int encodedLengthSize(int length) {
        if (length < 128) {
            return 1;
        }
        int numberOfBits = 32 - Integer.numberOfLeadingZeros(length);
        int numberOfBytes = (numberOfBits + 7) / 8;
        return numberOfBytes + 1;
    }

    static int decodeLength(byte[] buffer, int offset) {
        int firstByte = buffer[offset] & 0xFF;
        //checkArgument(firstByte != 0x80, "Indefinite lengths not supported in DER");
        //checkArgument(firstByte != 0xFF, "Invalid length first byte 0xFF");
        if (firstByte < 128) {
            return firstByte;
        }
        int numberOfBytes = firstByte & 0x7F;
        //checkArgument(numberOfBytes <= 4);
        int length = 0;
        for (int i = 0; i < numberOfBytes; i++) {
            length = (length << 8) | (buffer[offset + 1 + i] & 0xFF);
        }
        return length;
    }

    public static byte[] encodeOID(String oid) {
        requireNonNull(oid, "oid is null");
        Iterator<Object> it = new StringTokenizer(oid, ".").asIterator();
        Stream<Object> targetStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(it,
                Spliterator.ORDERED), false);
        List<Integer> parts = targetStream.map(Object::toString)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        //checkArgument(parts.size() >= 2, "at least 2 parts are required");
        try {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(parts.get(0) * 40 + parts.get(1));
            for (Integer part : parts.subList(2, parts.size())) {
                writeOidPart(body, part);
            }
            byte[] length = encodeLength(body.size());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(OBJECT_IDENTIFIER_TAG);
            out.write(length);
            body.writeTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            // this won't happen with byte array output streams
            throw new UncheckedIOException(e);
        }
    }

    public static int[] decodeOID(byte[] buffer) throws IOException {
        final byte ASN_BIT8 = (byte) 0x80;
        /*
         * ASN.1 objid ::= 0x06 asnlength subidentifier {subidentifier}*
         * subidentifier ::= {leadingbyte}* lastbyte
         * leadingbyte ::= 1 7bitvalue
         * lastbyte ::= 0 7bitvalue
         */
        int subidentifier;
        int length;
        // get the type
        int offset = 0;
        byte type = buffer[offset++];
        if (type != 0x06) {
            throw new IOException("Wrong type. Not an OID: ");
        }
        length = decodeLength(buffer, offset++);
        int[] oid = new int[length + 2];
        /* Handle invalid object identifier encodings of the form 06 00 robustly */
        if (length == 0) {
            oid[0] = oid[1] = 0;
        }
        int pos = 1;
        while (length > 0) {
            subidentifier = 0;
            int b;
            do {    /* shift and add in low order 7 bits */
                b = buffer[offset++];
                subidentifier = (subidentifier << 7) + (b & ~ASN_BIT8);
                length--;
            } while ((length > 0) && ((b & ASN_BIT8) != 0));    /* last byte has high bit clear */
            oid[pos++] = subidentifier;
        }
        /*
         * The first two subidentifiers are encoded into the first component
         * with the value (X * 40) + Y, where:
         * X is the value of the first subidentifier.
         * Y is the value of the second subidentifier.
         */
        subidentifier = oid[1];
        if (subidentifier == 0x2B) {
            oid[0] = 1;
            oid[1] = 3;
        } else if (subidentifier >= 0 && subidentifier < 80) {
            if (subidentifier < 40) {
                oid[0] = 0;
                oid[1] = subidentifier;
            } else {
                oid[0] = 1;
                oid[1] = subidentifier - 40;
            }
        } else {
            oid[0] = 2;
            oid[1] = subidentifier - 80;
        }
        if (pos < 2) {
            pos = 2;
        }
        int[] value = new int[pos];
        System.arraycopy(oid, 0, value, 0, pos);
        return value;
    }

    public static String oidToString(int[] oid) {
        StringBuilder sb = new StringBuilder();
        for (int i : oid) {
            if (sb.length() > 0) {
                sb.append(".");
            }
            sb.append(i);
        }
        return sb.toString();
    }

    /**
     * Encode an OID number part.  The encoding is a big endian variant.
     */
    private static void writeOidPart(ByteArrayOutputStream out, final int number) {
        if (number < 128) {
            out.write((byte) number);
            return;
        }

        int numberOfBits = Integer.SIZE - Integer.numberOfLeadingZeros(number);
        int numberOfParts = (numberOfBits + 6) / 7;
        for (int i = 0; i < numberOfParts - 1; i++) {
            int partToEncode = (numberOfParts - i);
            int shiftSize = (partToEncode - 1) * 7;
            int part = (number >>> shiftSize) & 0x7F | 0x80;
            out.write(part);
        }
        out.write(number & 0x7f);
    }

    public static byte[] encodeNull() {
        return new byte[]{NULL_TAG, 0x00};
    }

    public static byte[] encodeTag(int tag, byte[] body) throws IOException {
        //checkArgument(tag >= 0 && tag < 32, "Invalid tag: %s", tag);
        requireNonNull(body, "body is null");
        return encodeTagInternal(tag, body);
    }

    public static byte[] encodeContextSpecificTag(int tag, byte[] body) throws IOException {
        //checkArgument(tag >= 0 && tag < 32, "Invalid tag: %s", tag);
        requireNonNull(body, "body is null");
        int privateTag = tag | 0x80;
        return encodeTagInternal(privateTag, body);
    }

    private static byte[] encodeTagInternal(int tag, byte[] body) throws IOException {
        //checkArgument(tag >= 0 && tag < 256, "Invalid tag: %s", tag);
        byte[] lengthEncoded = encodeLength(body.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1 + lengthEncoded.length + body.length);
        out.write(tag);
        out.write(lengthEncoded);
        out.write(body);
        return out.toByteArray();
    }

    public static byte[] encodeContextSpecificSequence(int tag, byte[]... encodedValues) throws IOException {
        //checkArgument(tag >= 0 && tag < 32, "Invalid tag: %s", tag);
        requireNonNull(encodedValues, "body is null");
        int privateTag = tag | 0xA0;

        return encodeConstructed(privateTag, encodedValues);
    }

    private static byte[] encodeConstructed(int privateTag, byte[]... encodedValues) throws IOException {
        int length = 0;
        for (byte[] encodedValue : encodedValues) {
            length += encodedValue.length;
        }
        byte[] lengthEncoded = encodeLength(length);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1 + lengthEncoded.length + length);
        out.write(privateTag);
        out.write(lengthEncoded);
        for (byte[] entry : encodedValues) {
            out.write(entry);
        }
        return out.toByteArray();
    }
}
