package org.xbib.net.mime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class MimeTypeFile {

    private final Map<String, MimeTypeEntry> extensions;

    private String prev;

    public MimeTypeFile(URL url) throws IOException {
        this.extensions = new HashMap<>();
        parse(url);
    }

    public Map<String, MimeTypeEntry> getExtensions() {
        return extensions;
    }

    public MimeTypeEntry getMimeTypeEntry(String extension) {
        return extensions.get(extension);
    }

    public String getMimeTypeString(String extension) {
        MimeTypeEntry entry = getMimeTypeEntry(extension);
        return entry != null ? entry.getType() : null;
    }

    private void parse(URL url) throws IOException {
        try (InputStream inputStream = url.openStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            bufferedReader.lines().forEach(line -> {
                if (prev == null) {
                    prev = line;
                } else {
                    prev = prev + line;
                }
                int end = prev.length();
                if (prev.length() > 0 && prev.charAt(end - 1) == 92) {
                    prev = prev.substring(0, end - 1);
                } else{
                    parseEntry(prev);
                    prev = null;
                }
            });
            if (prev != null) {
                parseEntry(prev);
            }
        }
    }

    private void parseEntry(String line) {
        String mimetype = null;
        String ext;
        String entry;
        line = line.trim();
        StringTokenizer strtok = new StringTokenizer(line);
        if (line.isEmpty()) {
            return;
        }
        if (line.startsWith("#")) {
            return;
        }
        if (line.indexOf(61) > 0) {
            while (strtok.hasMoreTokens()) {
                String num_tok = strtok.nextToken();
                entry = null;
                if (strtok.hasMoreTokens() && "=".equals(strtok.nextToken()) && strtok.hasMoreTokens()) {
                    entry = strtok.nextToken();
                }
                if (entry == null) {
                    return;
                }
                if ("type".equals(num_tok)) {
                    mimetype = entry;
                } else if ("exts".equals(num_tok)) {
                    StringTokenizer st = new StringTokenizer(entry, ",");
                    while (st.hasMoreTokens()) {
                        ext = st.nextToken();
                        MimeTypeEntry entry1 = new MimeTypeEntry(mimetype, ext);
                        extensions.put(ext, entry1);
                    }
                }
            }
        } else {
            if (strtok.countTokens() == 0) {
                return;
            }
            mimetype = strtok.nextToken();
            while (strtok.hasMoreTokens()) {
                ext = strtok.nextToken();
                MimeTypeEntry entry2 = new MimeTypeEntry(mimetype, ext);
                extensions.put(ext, entry2);
            }
        }
    }
}
