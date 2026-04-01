package waffleoRai_Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream{
	
	private ByteBuffer buffer;
	
	public ByteBufferInputStream(ByteBuffer buff) {
		buffer = buff;
	}
	
	public int available() {
		return buffer.remaining();
	}

	public void mark(int readlimit) {
		//TODO Implement readlimit ig
		buffer.mark();
	}
	
	public boolean markSupported() {return true;}
	
	public int read() throws IOException {
		return Byte.toUnsignedInt(buffer.get());
	}
	
	public void reset() {
		buffer.reset();
	}
	
	public long skip(long n) {
		long amt = 0;
		for(int i = 0; i < n; i++) {
			if(buffer.hasRemaining()) {
				amt++;
				buffer.get();
			}
			else break;
		}
		return amt;
	}
	
	public void close() {
		buffer.clear();
	}

}
