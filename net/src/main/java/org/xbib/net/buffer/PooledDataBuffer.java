package org.xbib.net.buffer;

/**
 * Extension of {@link DataBuffer} that allows for buffer that share
 * a memory pool. Introduces methods for reference counting.
 */
public interface PooledDataBuffer extends DataBuffer {

	/**
	 * Return {@code true} if this buffer is allocated;
	 * {@code false} if it has been deallocated.
	 */
	boolean isAllocated();

	/**
	 * Increase the reference count for this buffer by one.
	 * @return this buffer
	 */
	PooledDataBuffer retain();

	/**
	 * Associate the given hint with the data buffer for debugging purposes.
	 * @return this buffer
	 */
	PooledDataBuffer touch(Object hint);

}
