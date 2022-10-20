package org.xbib.net.util;

import java.security.SecureRandom;

public class RandomUtil {

    private static final SecureRandom secureRandom = new SecureRandom();

    private RandomUtil() {
    }

    public static String randomString(int length) {
        byte[] b = new byte[length];
        secureRandom.nextBytes(b);
        return encodeHex(b);
    }

    public static byte[] randomBytes(int length) {
        byte[] b = new byte[length];
        secureRandom.nextBytes(b);
        return b;
    }

    private static String encodeHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(Character.forDigit((b & 240) >> 4, 16)).append(Character.forDigit((b & 15), 16));
        }
        return sb.toString();
    }
}
