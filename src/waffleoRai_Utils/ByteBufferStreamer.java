package waffleoRai_Utils;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ByteBufferStreamer implements StreamWrapper{
	
	private ByteBuffer buffer;
	private int readcount = 0;
	
	//private LinkedList<Byte> pushStack;
	
	private ByteBufferStreamer(){};
	
	public ByteBufferStreamer(int alloc)
	{
		buffer = ByteBuffer.allocate(alloc);
		//pushStack = new LinkedList<Byte>();
	}
	
	public static ByteBufferStreamer wrap(ByteBuffer byteBuffer)
	{
		ByteBufferStreamer stream = new ByteBufferStreamer();
		stream.buffer = byteBuffer;
		//stream.pushStack = new LinkedList<Byte>();
		return stream;
	}
	
	public static ByteBufferStreamer wrap(FileBuffer fileBuffer)
	{
		ByteBufferStreamer stream = new ByteBufferStreamer();
		stream.buffer = fileBuffer.toByteBuffer();
		//stream.pushStack = new LinkedList<Byte>();
		return stream;
	}
	
	public ByteBuffer getBuffer(){return buffer;}
	
	public int getFull(){return Byte.toUnsignedInt(get());}
	
	public byte get()
	{
		//if(!pushStack.isEmpty()) return pushStack.pop();
		try{
			readcount++;
			return buffer.get();
		}
		catch(BufferUnderflowException ex){
			System.err.println("Woah, this buffer appears to be empty...");
			System.err.println("Bytes read: 0x" + Integer.toHexString(readcount));
			throw ex;
		}
	}
	
	public void put(byte b)
	{
		buffer.limit(buffer.position()+1);
		buffer.put(b);
	}
	
	public boolean isEmpty()
	{
		return(!buffer.hasRemaining());
		//return buffer.position() >= buffer.limit();
	}
	
	public void close(){buffer.clear();}
	
	public void push(byte b)
	{
		//pushStack.push(b);
		throw new UnsupportedOperationException();
	}
	
	public void rewind()
	{
		readcount = 0;
		buffer.rewind();
	}

}
