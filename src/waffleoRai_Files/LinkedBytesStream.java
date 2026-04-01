package waffleoRai_Files;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class LinkedBytesStream extends InputStream{
	
	private LinkedList<Byte> list;
	
	private LinkedBytesStream() {}
	
	public static LinkedBytesStream wrap(LinkedList<Byte> buffer) {
		LinkedBytesStream s = new LinkedBytesStream();
		s.list = buffer;
		return s;
	}
	
	public int available() {
		return list.size();
	}

	public void mark(int readlimit) {}
	public boolean markSupported() {return false;}
	
	public int read() throws IOException {
		return Byte.toUnsignedInt(list.pop());
	}
	
	public void reset() {}
	
	public long skip(long n) {
		long amt = 0;
		for(int i = 0; i < n; i++) {
			if(!list.isEmpty()) {
				list.pop();
			}
			else break;
		}
		return amt;
	}
	
	public void close() {
		list.clear();
	}

}
