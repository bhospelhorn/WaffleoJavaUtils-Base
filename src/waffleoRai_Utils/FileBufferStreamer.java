package waffleoRai_Utils;

public class FileBufferStreamer implements StreamWrapper{

	private FileBuffer buffer;
	private long f_pos;
	private long e_pos;
	
	public FileBufferStreamer(FileBuffer file){
		buffer = file;
		f_pos = 0;
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

	public void close() {}

	public void rewind() {f_pos = 0;}
	
	public FileBuffer getData(){return buffer;}

}
