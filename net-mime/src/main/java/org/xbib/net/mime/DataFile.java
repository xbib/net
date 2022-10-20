package org.xbib.net.mime;

import java.io.File;
import java.io.IOException;

final class DataFile {
    private WeakDataFile weak;
    private long writePointer;

    DataFile(File file) throws IOException {
        writePointer=0;
        weak = new WeakDataFile(this, file);
    }

    void close() throws IOException {
        weak.close();
    }

    /**
     * Read data from the given file pointer position.
     *
     * @param pointer read position
     * @param buf that needs to be filled
     * @param offset the start offset of the data.
     * @param length of data that needs to be read
     */
    synchronized void read(long pointer, byte[] buf, int offset, int length ) throws IOException {
        weak.read(pointer, buf, offset, length);
    }

    void renameTo(File f) throws IOException {
        weak.renameTo(f);
    }

    /**
     * Write data to the file
     *
     * @param data that needs to written to a file
     * @param offset start offset in the data
     * @param length no bytes to write
     * @return file pointer before the write operation(or at which the
     *         data is written)
     */
    synchronized long writeTo(byte[] data, int offset, int length) throws IOException {
        long temp = writePointer;
        writePointer = weak.writeTo(writePointer, data, offset, length);
        return temp;
    }

}
