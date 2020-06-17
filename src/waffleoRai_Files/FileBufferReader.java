package waffleoRai_Files;

import java.io.IOException;
import java.io.Reader;

import waffleoRai_Utils.FileBuffer;

public class FileBufferReader extends Reader{
	
	private FileBuffer src;
	
	private boolean isClosed;
	private long mark;
	private int markLim;
	private int sinceMark;
	
	public FileBufferReader(FileBuffer buffer){
		src = buffer;
		markLim = -1;
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
		
		return b;
	}
	
	public int read(char[] cbuf) throws IOException {
		if(cbuf == null) return 0;
		
		int c = 0;
		for(int i = 0; i < cbuf.length; i++){
			int b = read();
			if(b != -1){
				c++;
				cbuf[i] = (char)c;
			}
			else return c;
		}
		
		return c;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if(cbuf == null || off < 0 || len < 1 || off >= cbuf.length) return 0;
		
		int c = 0;
		for(int i = 0; i < len; i++){
			int b = read();
			if(b != -1){
				c++;
				cbuf[off+i] = (char)c;
			}
			else return c;
		}
		
		return c;
	}
	
	
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
