package org.xbib.net.buffer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Default implementation of the {@code DataBufferFactory} interface. Allows for
 * specification of the default initial capacity at construction time, as well
 * as whether heap-based or direct buffers are to be preferred.
 */
public class DefaultDataBufferFactory implements DataBufferFactory {

	/**
	 * The default capacity when none is specified.
	 * @see #DefaultDataBufferFactory()
	 * @see #DefaultDataBufferFactory(boolean)
	 */
	public static final int DEFAULT_INITIAL_CAPACITY = 256;

	/**
	 * Shared instance based on the default constructor.
	 */
	private static final DefaultDataBufferFactory INSTANCE = new DefaultDataBufferFactory();

	private final boolean preferDirect;

	private final int defaultInitialCapacity;

	/**
	 * Creates a new {@code DefaultDataBufferFactory} with default settings.
	 */
	public DefaultDataBufferFactory() {
		this(false);
	}

	/**
	 * Creates a new {@code DefaultDataBufferFactory}, indicating whether direct
	 * buffers should be created by {@link #allocateBuffer()} and
	 * {@link #allocateBuffer(int)}.
	 * @param preferDirect {@code true} if direct buffers are to be preferred;
	 * {@code false} otherwise
	 */
	public DefaultDataBufferFactory(boolean preferDirect) {
		this(preferDirect, DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Creates a new {@code DefaultDataBufferFactory}, indicating whether direct
	 * buffers should be created by {@link #allocateBuffer()} and
	 * {@link #allocateBuffer(int)}, and what the capacity is to be used for
	 * {@link #allocateBuffer()}.
	 * @param preferDirect {@code true} if direct buffers are to be preferred;
	 * {@code false} otherwise
	 */
	public DefaultDataBufferFactory(boolean preferDirect, int defaultInitialCapacity) {
		if (defaultInitialCapacity <= 0) {
			throw new IllegalArgumentException("'defaultInitialCapacity' should be larger than 0");
		}
		this.preferDirect = preferDirect;
		this.defaultInitialCapacity = defaultInitialCapacity;
	}

	public static DataBufferFactory getInstance() {
		return INSTANCE;
	}

	@Override
	public DefaultDataBuffer allocateBuffer() {
		return allocateBuffer(this.defaultInitialCapacity);
	}

	@Override
	public DefaultDataBuffer allocateBuffer(int initialCapacity) {
		ByteBuffer byteBuffer = (this.preferDirect ?
				ByteBuffer.allocateDirect(initialCapacity) :
				ByteBuffer.allocate(initialCapacity));
		return DefaultDataBuffer.fromEmptyByteBuffer(this, byteBuffer);
	}

	@Override
	public DefaultDataBuffer wrap(ByteBuffer byteBuffer) {
		return DefaultDataBuffer.fromFilledByteBuffer(this, byteBuffer.slice());
	}

	@Override
	public DefaultDataBuffer wrap(byte[] bytes) {
		return DefaultDataBuffer.fromFilledByteBuffer(this, ByteBuffer.wrap(bytes));
	}

	/**
	 * This implementation creates a single {@link DefaultDataBuffer}
	 * to contain the data in {@code dataBuffers}.
	 */
	@Override
	public DefaultDataBuffer join(List<? extends DataBuffer> dataBuffers) {
		if (dataBuffers == null || dataBuffers.isEmpty()) {
			throw new IllegalArgumentException("DataBuffer List must not be empty");
		}
		int capacity = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
		DefaultDataBuffer result = allocateBuffer(capacity);
		dataBuffers.forEach(result::write);
		dataBuffers.forEach(DataBufferUtil::release);
		return result;
	}

	@Override
	public String toString() {
		return "DefaultDataBufferFactory (preferDirect=" + this.preferDirect + ")";
	}
}
