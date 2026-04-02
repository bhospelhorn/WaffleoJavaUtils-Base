package waffleoRai_Utils;

import java.io.InputStream;
import java.util.LinkedList;

import waffleoRai_Files.LinkedBytesStream;

public class LinkedBytesStreamer implements StreamWrapper{
	
	private LinkedList<Byte> list;
	
	public LinkedBytesStreamer(){
		list = new LinkedList<Byte>();
	}
	
	public boolean isReadOnly() {return false;}
	public long getPosition() {return -1L;}
	
	public byte get(){
		if(list.isEmpty()) return -1;
		return list.pop();
	}
	
	public int getFull(){
		if(list.isEmpty()) return -1;
		return Byte.toUnsignedInt(list.pop());
	}
	
	public void push(byte b){
		list.push(b);
	}
	
	public void put(byte b){
		list.add(b);
	}
	
	public boolean isEmpty(){
		return list.isEmpty();
	}
	
	public void close(){
		list.clear();
	}
	
	public int size(){
		return list.size();
	}
	
	public void rewind() {
		throw new UnsupportedOperationException();
	}
	
	public void rewind(long amt) {
		throw new UnsupportedOperationException();
	}
	
	public long skipToEnd() {
		int listLen = list.size();
		list.clear();
		return listLen;
	}
	
	public long skip(long amt) {
		long c = 0;
		while(!list.isEmpty() && (c < amt)) {
			list.pop();
			c++;
		}
		return c;
	}
	
	public InputStream asInputStream() {
		return LinkedBytesStream.wrap(list);
	}

}
