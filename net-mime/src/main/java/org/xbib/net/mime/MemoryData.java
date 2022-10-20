package org.xbib.net.mime;

import java.nio.ByteBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps the Part's partial content data in memory.
 */
final class MemoryData implements Data {

    private static final Logger LOGGER = Logger.getLogger(MemoryData.class.getName());

    private final byte[] data;

    private final int len;

    private final boolean isOnlyMemory;

    private final long threshold;

    private final File tempDir;

    MemoryData(ByteBuffer byteBuffer) {
        this(byteBuffer, false, 1048576L, new File(System.getProperty("java.io.tmpdir")));
    }

    MemoryData(ByteBuffer buf, boolean isOnlyMemory, long threshold, File tempDir) {
        data = buf.array(); // TODO
        len = buf.limit();
        this.isOnlyMemory = isOnlyMemory;
        this.threshold = threshold;
        this.tempDir = tempDir;
    }

    @Override
    public int size() {
        return len;
    }

    @Override
    public byte[] read() {
        return data;
    }

    @Override
    public long writeTo(DataFile file) throws IOException {
        return file.writeTo(data, 0, len);
    }

    @Override
    public Data createNext(DataHead dataHead, ByteBuffer buf) throws IOException {
        if (!isOnlyMemory && dataHead.inMemory >= threshold) {
                //String prefix = config.getTempFilePrefix();
                //String suffix = config.getTempFileSuffix();
                File tempFile = createTempFile("MIME", ".mime", tempDir);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Created temp file = {0}", tempFile);
                }
                dataHead.dataFile = new DataFile(tempFile);
            if (dataHead.head != null) {
                for (Chunk c = dataHead.head; c != null; c = c.next) {
                    long pointer = c.data.writeTo(dataHead.dataFile);
                    c.data = new FileData(dataHead.dataFile, pointer, len);
                }
            }
            return new FileData(dataHead.dataFile, buf);
        } else {
            return new MemoryData(buf, isOnlyMemory, threshold, tempDir);
        }
    }

    private static File createTempFile(String prefix, String suffix, File dir) throws IOException {
        if (dir != null) {
            Path path = dir.toPath();
            return Files.createTempFile(path, prefix, suffix).toFile();
        } else {
            return Files.createTempFile(prefix, suffix).toFile();
        }
    }
}
