package org.xbib.net.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataBufferUtil {

    private static final Logger logger = Logger.getLogger(DataBufferUtil.class.getName());

    private DataBufferUtil() {
    }

    public static boolean release(DataBuffer dataBuffer) {
        if (dataBuffer instanceof PooledDataBuffer) {
            PooledDataBuffer pooledDataBuffer = (PooledDataBuffer) dataBuffer;
            if (pooledDataBuffer.isAllocated()) {
                try {
                    return pooledDataBuffer.release();
                }
                catch (IllegalStateException ex) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "failed to release PooledDataBuffer " + dataBuffer, ex);
                    }
                    return false;
                }
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
        if (dataBuffer instanceof PooledDataBuffer) {
            PooledDataBuffer pooledDataBuffer = (PooledDataBuffer) dataBuffer;
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
