package org.xbib.net.path;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 *  Path normalizer.
 */
public class PathNormalizer {

    private static final char separator = '/';

    private PathNormalizer() {
    }

    public static String normalize(String path) {
        if (path == null || "".equals(path) || "/".equals(path)) {
            return "/";
        }
        path = path.replaceAll("/+", "/");
        int leadingSlashes = 0;
        while (leadingSlashes < path.length() && path.charAt(leadingSlashes) == '/') {
            ++leadingSlashes;
        }
        boolean isDir = (path.charAt(path.length() - 1) == '/');
        StringTokenizer st = new StringTokenizer(path, "/");
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
            sb.append('/');
        }
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append('/');
            }
        }
        if (isDir && sb.length() > 0 && sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }
}
