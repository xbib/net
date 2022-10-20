package org.xbib.net.util;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {

	private final ByteBuffer byteBuffer;

	public ByteBufferInputStream(ByteBuffer byteBuffer) {
		byteBuffer.mark();
		this.byteBuffer = byteBuffer;
	}

	@Override
	public int read() {
		if (!byteBuffer.hasRemaining()) {
			return -1;
		}
		return byteBuffer.get() & 0xFF;
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		if (length == 0) {
			return 0;
		}
		int count = Math.min(byteBuffer.remaining(), length);
		if (count == 0) {
			return -1;
		}
		byteBuffer.get(bytes, offset, count);
		return count;
	}

	@Override
	public int available() {
		return byteBuffer.remaining();
	}

	@Override
	public long skip(long n) {
		if (n < 0L) {
			return 0L;
		}
		int skipped = Math.min((int) n, byteBuffer.remaining());
		byteBuffer.position(byteBuffer.position() + skipped);
		return skipped;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public void mark(int readAheadLimit) {
		byteBuffer.mark();
	}

	@Override
	public void reset() {
		byteBuffer.reset();
	}

	@Override
	public void close() {
		byteBuffer.position(byteBuffer.limit());
	}
}
