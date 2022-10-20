package org.xbib.net.security.ssl.util;

import static java.util.Objects.isNull;

public final class StringUtils {

    private StringUtils() {}

    public static boolean isBlank(CharSequence charSequence) {
        int length = isNull(charSequence) ? 0 : charSequence.length();
        if (length != 0) {
            for (int i = 0; i < length; ++i) {
                if (!Character.isWhitespace(charSequence.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence charSequence) {
        return !isBlank(charSequence);
    }
}
