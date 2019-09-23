package org.xbib.net;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;

/**
 * Decodes percent-encoded strings.
 *
 * This class is not thread-safe.
 */
public class PercentDecoder {

    /**
     * Written to with decoded chars by decoder.
     */
    private final CharBuffer decodedCharBuf;

    private final CharsetDecoder decoder;

    /**
     * The decoded string for the current input.
     */
    private final StringBuilder outputBuf;

    /**
     * The bytes represented by the current sequence of %-triples. Resized as needed.
     */
    private ByteBuffer encodedBuf;

    public PercentDecoder() {
        this(StandardCharsets.UTF_8.newDecoder()
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .onMalformedInput(CodingErrorAction.REPORT));
    }

    /**
     * Construct a new PercentDecoder with default buffer sizes.
     *
     * @param charsetDecoder Charset to decode bytes into chars with
     * @see PercentDecoder#PercentDecoder(CharsetDecoder, int, int)
     */
    public PercentDecoder(CharsetDecoder charsetDecoder) {
        this(charsetDecoder, 16, 16);
    }

    /**
     * @param charsetDecoder            Charset to decode bytes into chars with
     * @param initialEncodedByteBufSize Initial size of buffer that holds encoded bytes
     * @param decodedCharBufSize        Size of buffer that encoded bytes are decoded into
     */
    public PercentDecoder(CharsetDecoder charsetDecoder, int initialEncodedByteBufSize, int decodedCharBufSize) {
        this.outputBuf = new StringBuilder();
        this.encodedBuf = ByteBuffer.allocate(initialEncodedByteBufSize);
        this.decodedCharBuf = CharBuffer.allocate(decodedCharBufSize);
        this.decoder = charsetDecoder;
    }

    /**
     * Decode a percent-encoded character sequuence to a string.
     *
     * @param input Input with %-encoded representation of characters in this instance's configured character set, e.g.
     *              "%20" for a space character
     * @return Corresponding string with %-encoded data decoded and converted to their corresponding characters
     * @throws MalformedInputException      if decoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if decoder is configured to report errors and an unmappable character is
     *                                      detected
     */
    public String decode(CharSequence input) throws MalformedInputException, UnmappableCharacterException {
        if (input == null) {
            return null;
        }
        outputBuf.setLength(0);
        outputBuf.ensureCapacity((input.length() / 8));
        encodedBuf.clear();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c != '%') {
                handleEncodedBytes();
                outputBuf.append(c);
                continue;
            }
            if (i + 2 >= input.length()) {
                throw new MalformedInputException(i);
            }
            if (encodedBuf.remaining() == 0) {
                ByteBuffer largerBuf = ByteBuffer.allocate(encodedBuf.capacity() * 2);
                encodedBuf.flip();
                largerBuf.put(encodedBuf);
                encodedBuf = largerBuf;
            }
            int c1 = input.charAt(++i);
            int c2 = input.charAt(++i);
            byte b1 = (byte) decode((char) c1);
            byte b2 = (byte) decode((char) c2);
            /*if (b1 == -1 || b2 == -1) {
                throw new MalformedInputException(i);
            }*/
            byte b = (byte) ((b1 & 0xf) << 4 | (b2 & 0xf));
            encodedBuf.put(b);
        }
        handleEncodedBytes();
        return outputBuf.toString();
    }

    private static int decode(char c) {
        return (c >= '0' && c <= '9') ? c - '0' :
                (c >= 'A' && c <= 'F') ? c - 'A' + 10 :
                        (c >= 'a' && c <= 'f') ? c - 'a' + 10 : -1;
    }

    /**
     * Decode any buffered encoded bytes and write them to the output buf.
     */
    private void handleEncodedBytes() throws MalformedInputException, UnmappableCharacterException {
        if (encodedBuf.position() == 0) {
            return;
        }
        decoder.reset();
        CoderResult coderResult = CoderResult.OVERFLOW;
        encodedBuf.flip();
        while (coderResult == CoderResult.OVERFLOW && encodedBuf.hasRemaining()) {
            decodedCharBuf.clear();
            coderResult = decoder.decode(encodedBuf, decodedCharBuf, false);
            throwIfError(coderResult);
            decodedCharBuf.flip();
            outputBuf.append(decodedCharBuf);
        }
        decodedCharBuf.clear();
        coderResult = decoder.decode(encodedBuf, decodedCharBuf, true);
        throwIfError(coderResult);
        if (encodedBuf.hasRemaining()) {
            throw new IllegalStateException("final decode didn't error, but didn't consume remaining input bytes");
        }
        if (coderResult != CoderResult.UNDERFLOW) {
            throw new IllegalStateException("expected underflow, but instead final decode returned " + coderResult);
        }
        decodedCharBuf.flip();
        outputBuf.append(decodedCharBuf);
        encodedBuf.clear();
        flush();
    }

    /**
     * Must only be called when the input encoded bytes buffer is empty.
     */
    private void flush() throws MalformedInputException, UnmappableCharacterException {
        CoderResult coderResult;
        decodedCharBuf.clear();
        coderResult = decoder.flush(decodedCharBuf);
        decodedCharBuf.flip();
        outputBuf.append(decodedCharBuf);
        throwIfError(coderResult);
        if (coderResult != CoderResult.UNDERFLOW) {
            throw new IllegalStateException("decoder flush resulted in " + coderResult);
        }
    }

    /**
     * If the coder result is considered an error (i.e. not overflow or underflow), throw the corresponding
     * CharacterCodingException.
     *
     * @param coderResult result to check
     * @throws MalformedInputException      if result represents malformed input
     * @throws UnmappableCharacterException if result represents an unmappable character
     */
    private static void throwIfError(CoderResult coderResult) throws MalformedInputException, UnmappableCharacterException {
        if (coderResult.isMalformed()) {
            throw new MalformedInputException(coderResult.length());
        }
        if (coderResult.isUnmappable()) {
            throw new UnmappableCharacterException(coderResult.length());
        }
    }
}
