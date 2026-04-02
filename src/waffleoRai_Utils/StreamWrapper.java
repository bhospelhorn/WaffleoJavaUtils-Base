package waffleoRai_Utils;

import java.io.InputStream;

public interface StreamWrapper {
	
	public long getPosition();
	public byte get();
	public int getFull();
	public void push(byte b);
	public void put(byte b);
	public boolean isEmpty();
	public boolean isReadOnly();
	public void close();
	
	public void rewind();
	public void rewind(long amt);
	public long skipToEnd();
	public long skip(long amt);
	
	public InputStream asInputStream();
	
}
