package org.xbib.net;

import java.nio.file.Path;
import java.time.Instant;

public interface Resource {

    Path getPath();

    String getName();

    String getBaseName();

    String getSuffix();

    String getResourcePath();

    URL getURL();

    Instant getLastModified();

    long getLength();

    boolean isExists();

    boolean isDirectory();

    String getMimeType();

    String getIndexFileName();

    boolean isExistsIndexFile();
}
