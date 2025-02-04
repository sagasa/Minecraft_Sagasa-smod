package hide.model.util;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
	private ByteBuffer buffer;

	public ByteBufferInputStream(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int read() {
		if (!buffer.hasRemaining()) {
			return -1;
		}
		return buffer.get();
	}

	@Override
	public int read(byte[] bytes, int off, int len) {
		if (!buffer.hasRemaining()) {
			return -1;
		}

		len = Math.min(len, buffer.remaining());
		buffer.get(bytes, off, len);
		return len;
	}
}