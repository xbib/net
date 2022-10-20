package org.xbib.net.security.util;

import java.util.Objects;

public class HexUtil {

    private HexUtil() {
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(Character.forDigit((b & 240) >> 4, 16)).append(Character.forDigit((b & 15), 16));
        }
        return sb.toString();
    }

    public static byte[] fromHex(String hex) {
        Objects.requireNonNull(hex);
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) fromHex(hex.charAt(i), hex.charAt(i + 1));
        }
        return data;
    }

    public static int fromHex(int b1, int b2) {
        int i1 = Character.digit(b1, 16);
        if (i1 == -1) {
            throw new IllegalArgumentException("invalid character in hexadecimal: " + b1);
        }
        int i2 = Character.digit(b2, 16);
        if (i2 == -1) {
            throw new IllegalArgumentException("invalid character in hexadecimal: " + b2);
        }
        return (i1 << 4) + i2;
    }

}
