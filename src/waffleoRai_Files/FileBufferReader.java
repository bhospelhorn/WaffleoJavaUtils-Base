package waffleoRai_Files;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import waffleoRai_Utils.FileBuffer;

public class FileBufferReader extends Reader{
	
	private Charset encoding;
	
	private FileBuffer src;
	
	private boolean isClosed;
	private long mark;
	private int markLim;
	private int sinceMark;
	
	public FileBufferReader(FileBuffer buffer){
		src = buffer;
		markLim = -1;
		encoding = Charset.forName("ASCII");
	}
	
	public void mark(int readAheadLimit){
		mark = src.getCurrentPosition();
		markLim = readAheadLimit;
	}
	
	public boolean markSupported(){
		return true;
	}
	
	public int read() throws IOException {
		if(src.bytesRemaining() <= 0) return -1;
		
		int b = Byte.toUnsignedInt(src.nextByte());
		if(markLim >= 0){
			sinceMark++;
			if(sinceMark >= markLim){
				mark = 0;
				sinceMark = 0;
				markLim = -1;
			}
		}
		//System.err.println("Byte: " + String.format("%02x", b));
		return b;
	}
	
	public int read(char[] cbuf) throws IOException {
		if(cbuf == null) return -1;
		return read(cbuf, 0, cbuf.length);
	}

	public int read(char[] cbuf, int off, int len) throws IOException {
		if(cbuf == null || off < 0 || len < 1 || off >= cbuf.length) return -1;
		
		//Read into a ByteBuffer
		//boolean end = false;
		ByteBuffer bb = ByteBuffer.allocate(len);
		for(int i = 0; i < len; i++){
			int b = read();
			if(b != -1) bb.put((byte)b);
			else{
				//end = true;
				break;
			}
		}
		
		CharBuffer cb = encoding.decode(bb);
		int clen = cb.length();
		for(int i = 0; i < len; i++){
			if(i>=clen) break;
			cbuf[off+i] = cb.get();
		}
		
		return clen;
	}
	
	/*public int read(CharBuffer target){
		//TODO
		
		return 0;
	}*/
	
	public boolean ready(){
		if(isClosed) return false;
		//if(src.bytesRemaining() <= 0)return false;
		return true;
	}
	
	public void reset(){
		src.setCurrentPosition(mark);
	}
	
	public long skip(long n){
		return src.skipBytes(n);
	}

	public void close() throws IOException {
		isClosed = true;
		src = null;
	}

}
