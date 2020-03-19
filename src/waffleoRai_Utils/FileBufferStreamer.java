package waffleoRai_Utils;

public class FileBufferStreamer implements StreamWrapper{

	private FileBuffer buffer;
	private long f_pos;
	private long e_pos;
	
	public FileBufferStreamer(FileBuffer file)
	{
		buffer = file;
		f_pos = 0;
		e_pos = file.getFileSize();
	}
	
	@Override
	public byte get() 
	{
		if(f_pos >= e_pos) return 0;
		return buffer.getByte(f_pos++);
	}

	@Override
	public int getFull() 
	{
		return Byte.toUnsignedInt(get());
	}

	@Override
	public void push(byte b) 
	{
		buffer.addToFile(b, 0);
	}

	@Override
	public void put(byte b) 
	{
		buffer.addToFile(b);
	}

	@Override
	public boolean isEmpty() 
	{
		return (f_pos >= e_pos);
	}

	@Override
	public void close() {}

	@Override
	public void rewind() {f_pos = 0;}

}
