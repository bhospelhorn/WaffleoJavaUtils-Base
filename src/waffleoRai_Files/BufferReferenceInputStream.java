package waffleoRai_Files;

import java.io.IOException;
import java.io.InputStream;

import waffleoRai_Utils.BufferReference;

public class BufferReferenceInputStream extends InputStream{
	
	private BufferReference reference;
	
	public BufferReferenceInputStream(BufferReference ref) {
		ref = reference;
	}
	
	public int available() {
		return (int)reference.getRemaining();
	}

	public boolean markSupported() {return false;}
	
	public int read() throws IOException {
		return Byte.toUnsignedInt(reference.nextByte());
	}
	
	public void reset() {}
	
	public long skip(long n) {
		return reference.add(n);
	}
	
	public void close() {}

}
