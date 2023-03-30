package org.xbib.net.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class DataBufferUtil {

    private DataBufferUtil() {
    }

    public static boolean release(DataBuffer dataBuffer) {
        if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer) {
            if (pooledDataBuffer.isAllocated()) {
                pooledDataBuffer.release();
                return true;
            }
        }
        return false;
    }

    /**
     * Retain the given data buffer, if it is a {@link PooledDataBuffer}.
     * @param dataBuffer the data buffer to retain
     * @return the retained buffer
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataBuffer> T retain(T dataBuffer) {
        if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer) {
            return (T) pooledDataBuffer.retain();
        }
        else {
            return dataBuffer;
        }
    }

    public static DataBuffer readBuffer(DataBufferFactory factory,
                                        ReadableByteChannel channel,
                                        long size) throws IOException {
        boolean release = true;
        DataBuffer dataBuffer = factory.allocateBuffer((int) size);
        try {
            int read;
            ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, dataBuffer.capacity());
            if ((read = channel.read(byteBuffer)) >= 0) {
                dataBuffer.writePosition(read);
                release = false;
                return dataBuffer;
            }
            else {
                return null;
            }
        } finally {
            if (release) {
                release(dataBuffer);
            }
        }
    }
}
