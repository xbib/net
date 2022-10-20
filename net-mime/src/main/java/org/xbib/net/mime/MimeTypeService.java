package org.xbib.net.mime;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MimeTypeService {

    public static final String DEFAULT_TYPE = "application/octet-stream";

    private final Map<String, MimeTypeEntry> extensions;

    public MimeTypeService() {
        this.extensions = new HashMap<>();
        try {
            List<MimeTypeFile> mimeTypeFiles = new ArrayList<>();
            String s = "META-INF/mime.types";
            boolean found = false;
            ClassLoader classLoader = getClass().getClassLoader();
            Enumeration<URL> e = classLoader.getResources(s);
            while (e != null && e.hasMoreElements()) {
                URL url = e.nextElement();
                if (url != null) {
                    mimeTypeFiles.add(new MimeTypeFile(url));
                    found = true;
                }
            }
            if (!found) {
                MimeTypeFile mtf = loadResource(classLoader, "/" + s);
                if (mtf != null) {
                    mimeTypeFiles.add(mtf);
                }
            }
            MimeTypeFile defaultMimeTypeFile = loadResource(classLoader, "/META-INF/mimetypes.default");
            if (defaultMimeTypeFile != null) {
                mimeTypeFiles.add(defaultMimeTypeFile);
            }
            for (MimeTypeFile mimeTypeFile : mimeTypeFiles) {
                extensions.putAll(mimeTypeFile.getExtensions());
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public String getContentType(String nameWithSuffix) {
        if (nameWithSuffix == null) {
            return null;
        }
        int pos = nameWithSuffix.lastIndexOf('.');
        if (pos < 0) {
            return DEFAULT_TYPE;
        } else {
            String ext = nameWithSuffix.substring(pos + 1);
            if (ext.length() == 0) {
                return DEFAULT_TYPE;
            } else {
                return extensions.containsKey(ext) ? extensions.get(ext).getType() : DEFAULT_TYPE;
            }
        }
    }

    private static MimeTypeFile loadResource(ClassLoader classLoader, String name) throws IOException {
        URL url = classLoader.getResource(name);
        return url != null ? new MimeTypeFile(url) : null;
    }
}
