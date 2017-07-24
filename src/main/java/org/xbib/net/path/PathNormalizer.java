package org.xbib.net.path;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 */
public class PathNormalizer {

    private static final char separator = '/';

    private PathNormalizer() {
    }

    /*public static String normalizePath(String path) {
        return normalizePath(path, false);
    }

    public static String normalizePath(String path, boolean keepSeparator) {
        if (path == null || path.equals("") || path.equals("/")) {
            return "/";
        }
        path = path.replaceAll("/+", "/");
        int size = path.length();
        if (size == 0) {
            return path;
        }
        int prefix = getPrefixLength(path);
        if (prefix < 0) {
            return "";
        }
        char[] ch = new char[size + 2];
        path.getChars(0, path.length(), ch, 0);
        boolean firstIsDirectory = true;
        if (ch[0] != separator) {
            firstIsDirectory = false;
        }
        boolean lastIsDirectory = true;
        if (ch[size - 1] != separator) {
            lastIsDirectory = false;
        }
        for (int i = prefix + 1; i < size; i++) {
            if (ch[i] == separator && ch[i - 1] == separator) {
                System.arraycopy(ch, i, ch, i - 1, size - i);
                size--;
                i--;
            }
        }
        for (int i = prefix + 1; i < size; i++) {
            if (ch[i] == separator && ch[i - 1] == '.'
                    && (i == prefix + 1 || ch[i - 2] == separator)) {
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                System.arraycopy(ch, i + 1, ch, i - 1, size - i);
                size -=2;
                i--;
            }
        }
        int i =  prefix + 2;
        while (i < size) {
            if (ch[i] == separator && ch[i - 1] == '.' && ch[i - 2] == '.'
                    && (i == prefix + 2 || ch[i - 3] == separator)) {
                if (i == prefix + 2) {
                    return "";
                }
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                int j;
                boolean b = false;
                for (j = i - 4 ; j >= prefix; j--) {
                    if (ch[j] == separator) {
                        System.arraycopy(ch, i + 1, ch, j + 1, size - i);
                        size -= (i - j);
                        i = j + 1;
                        b = true;
                        break;
                    }
                }
                if (b) {
                    continue;
                }
                System.arraycopy(ch, i + 1, ch, prefix, size - i);
                size -= (i + 1 - prefix);
                i = prefix + 1;
            }
            i++;
        }
        if (size <= 0) {
            return "";
        }
        String s = new String(ch, 0, size);
        if (size <= prefix) {
            return s;
        }
        if (!keepSeparator) {
            if (firstIsDirectory && lastIsDirectory) {
                return s.substring(1, s.length() - 1);
            } else if (firstIsDirectory) {
                return s.substring(1);
            } else if (lastIsDirectory) {
                return s.substring(0, s.length() - 1);
            }
        }
        return s;
    }*/

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

    private static int getPrefixLength(String filename) {
        if (filename == null) {
            return -1;
        }
        int len = filename.length();
        if (len == 0) {
            return 0;
        }
        char ch0 = filename.charAt(0);
        if (ch0 == ':') {
            return -1;
        }
        if (len == 1) {
            if (ch0 == '~') {
                return 2;
            }
            return ch0 == separator ? 1 : 0;
        } else {
            if (ch0 == '~') {
                int pos = filename.indexOf(separator, 1);
                return pos == -1 ? len + 1 : pos + 1;
            }
            char ch1 = filename.charAt(1);
            if (ch1 == ':') {
                ch0 = Character.toUpperCase(ch0);
                if (ch0 >= ('A') && ch0 <= ('Z')) {
                    if (len == 2 || filename.charAt(2) != separator) {
                        return 2;
                    }
                    return 3;
                }
                return -1;
            } else if (ch0 == separator && ch1 == separator) {
                int pos = filename.indexOf(separator, 2);
                if (pos == -1  || pos == 2) {
                    return -1;
                }
                return pos + 1;
            } else {
                return ch0 == separator ? 1 : 0;
            }
        }
    }
}
