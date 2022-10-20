package org.xbib.net;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 *  Path normalizer.
 */
public class PathNormalizer {

    private static final char SEPARATOR_CHAR = '/';

    private static final String SEPARATOR_STRING = "/";

    private PathNormalizer() {
    }

    public static String normalize(String p) {
        String path = p;
        if (path == null || "".equals(path) || SEPARATOR_STRING.equals(path)) {
            return SEPARATOR_STRING;
        }
        path = path.replaceAll("/+", SEPARATOR_STRING);
        int leadingSlashes = 0;
        while (leadingSlashes < path.length() && path.charAt(leadingSlashes) == SEPARATOR_CHAR) {
            ++leadingSlashes;
        }
        boolean isDir = (path.charAt(path.length() - 1) == SEPARATOR_CHAR);
        StringTokenizer st = new StringTokenizer(path, SEPARATOR_STRING);
        LinkedList<String> list = new LinkedList<>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ("..".equals(token)) {
                if (!list.isEmpty() && !"..".equals(list.getLast())) {
                    list.removeLast();
                    if (!st.hasMoreTokens()) {
                        isDir = true;
                    }
                }
            } else if (!".".equals(token) && !"".equals(token)) {
                list.add(token);
            }
        }
        StringBuilder sb = new StringBuilder();
        while (leadingSlashes-- > 0) {
            sb.append(SEPARATOR_CHAR);
        }
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(SEPARATOR_CHAR);
            }
        }
        if (isDir && sb.length() > 0 && sb.charAt(sb.length() - 1) != SEPARATOR_CHAR) {
            sb.append(SEPARATOR_CHAR);
        }
        return sb.toString();
    }
}
