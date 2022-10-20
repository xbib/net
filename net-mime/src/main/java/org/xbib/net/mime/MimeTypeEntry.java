package org.xbib.net.mime;

public class MimeTypeEntry {

    private final String type;

    private final String extension;

    public MimeTypeEntry(String type, String extension) {
        this.type = type;
        this.extension = extension;
    }

    public String getType() {
        return type;
    }

    public String getExtension() {
        return extension;
    }
}
