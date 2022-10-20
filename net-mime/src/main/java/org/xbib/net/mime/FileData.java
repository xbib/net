package org.xbib.net.mime;

import java.io.IOException;
import java.nio.ByteBuffer;

final class FileData implements Data {

    private final DataFile file;

    private final long pointer;

    private final int length;

    FileData(DataFile file, ByteBuffer buf) throws IOException {
        this(file, file.writeTo(buf.array(), 0, buf.limit()), buf.limit());
    }

    FileData(DataFile file, long pointer, int length) {
        this.file = file;
        this.pointer = pointer;
        this.length = length;
    }

    @Override
    public byte[] read() throws IOException {
        byte[] buf = new byte[length];
        file.read(pointer, buf, 0, length);
        return buf;
    }

    @Override
    public long writeTo(DataFile file) {
        throw new IllegalStateException();
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public Data createNext(DataHead dataHead, ByteBuffer buf) throws IOException {
        return new FileData(file, buf);
    }
}
