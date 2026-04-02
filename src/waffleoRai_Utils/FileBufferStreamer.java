package waffleoRai_Utils;

import java.io.InputStream;

import waffleoRai_Files.FileBufferInputStream;

public class FileBufferStreamer implements StreamWrapper{

	private FileBuffer buffer;
	private long f_pos;
	private long e_pos;
	
	public FileBufferStreamer(FileBuffer file){
		buffer = file;
		f_pos = 0;
		e_pos = file.getFileSize();
	}
	
	public FileBufferStreamer(FileBuffer file, long stPos){
		buffer = file;
		f_pos = stPos;
		e_pos = file.getFileSize();
	}
	
	public byte get() {
		if(f_pos >= e_pos) return 0;
		return buffer.getByte(f_pos++);
	}

	public int getFull(){
		return Byte.toUnsignedInt(get());
	}

	public void push(byte b){
		buffer.addToFile(b, 0);
		e_pos++;
	}

	public void put(byte b){
		buffer.addToFile(b);
		e_pos++;
	}

	public boolean isEmpty(){
		return (f_pos >= e_pos);
	}
	
	public boolean isReadOnly() {
		return buffer.readOnly();
	}

	public void close() {}

	public void rewind() {f_pos = 0;}
	
	public void rewind(long amt) {
		f_pos -= amt;
		if(f_pos < 0) f_pos = 0;
	}
	
	public FileBuffer getData(){return buffer;}
	public long getPosition() {return f_pos;}
	
	public long skipToEnd() {
		long diff = e_pos - f_pos;
		f_pos = e_pos;
		return diff;
	}
	
	public long skip(long amt) {
		long n_pos = f_pos + amt;
		long o_pos = f_pos;
		if(n_pos > e_pos) n_pos = e_pos;
		f_pos = n_pos;
		return n_pos - o_pos;
	}
	
	public InputStream asInputStream() {
		return new FileBufferInputStream(buffer);
	}

}
