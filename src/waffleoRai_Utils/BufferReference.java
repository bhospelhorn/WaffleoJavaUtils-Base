package waffleoRai_Utils;

import java.util.LinkedList;
import java.util.List;

public class BufferReference{

	/*----- Instance Variables -----*/
	
	private FileBuffer src;
	private long pos;
	
	private List<BufferWriteListener> listeners;
	
	/*----- Init -----*/
	
	protected BufferReference(){}
	
	protected BufferReference(FileBuffer source, long position){
		if(source == null) throw new NullPointerException("Cannot reference null source!");
		src = source;
		pos = position;
		if(position < 0 || position >= source.getFileSize()) throw new IndexOutOfBoundsException("Position 0x" + Long.toHexString(position) 
		   + " out of bounds for buffer of length 0x" + Long.toHexString(src.getFileSize()));
	}
	
	/*----- Getters -----*/
	
	public FileBuffer getBuffer(){return src;}
	public long getBufferPosition(){return pos;}
	
	/*----- Setters -----*/
	
	public void addWriteListener(BufferWriteListener l){
		if(listeners == null) listeners = new LinkedList<BufferWriteListener>();
		listeners.add(l);
	}
	
	public void clearWriteListeners(){
		if(listeners != null) listeners.clear();
	}
	
	public void setByteOrder(boolean big_endian){
		src.setEndian(big_endian);
	}
	
	/*----- Read -----*/
	
	public byte getByte(){
		return src.getByte(pos);
	}
	
	public short getShort(){
		return src.shortFromFile(pos);
	}
	
	public int get24Bits(){
		return src.shortishFromFile(pos);
	}
	
	public int getInt(){
		return src.intFromFile(pos);
	}
	
	public long getLong(){
		return src.longFromFile(pos);
	}
	
	public byte nextByte(){
		return src.getByte(pos++);
	}
	
	public short nextShort(){
		short val = src.shortFromFile(pos); pos += 2;
		return val;
	}
	
	public int next24Bits(){
		int val = src.shortishFromFile(pos); pos += 3;
		return val;
	}
	
	public int nextInt(){
		int val = src.intFromFile(pos); pos += 4;
		return val;
	}
	
	public long nextLong(){
		long val = src.longFromFile(pos); pos += 8;
		return val;
	}
	
	public String nextASCIIString(int len){
		String s = src.getASCII_string(pos, len);
		pos += s.length();
		return s;
	}
	
	public String nextASCIIString(){
		String s = src.getASCII_string(pos, '\0');
		pos += s.length() + 1;
		return s;
	}
	
	public byte getByte(int offset){
		return src.getByte(pos+offset);
	}
	
	public short getShort(int offset){
		return src.shortFromFile(pos+offset);
	}
	
	public int get24Bits(int offset){
		return src.shortishFromFile(pos+offset);
	}
	
	public int getInt(int offset){
		return src.intFromFile(pos+offset);
	}
	
	public long getLong(int offset){
		return src.longFromFile(pos+offset);
	}
	
	
	/*----- Write -----*/
	
	public boolean writeByte(byte val){
		try{
			src.replaceByte(val, pos);
			notifyWriteListeners(pos, 1);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean writeShort(short val){
		try{
			src.replaceShort(val, pos);
			notifyWriteListeners(pos, 2);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean write24(int val){
		try{
			src.replaceShortish(val, pos);
			notifyWriteListeners(pos, 3);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean writeInt(int val){
		try{
			src.replaceInt(val, pos);
			notifyWriteListeners(pos, 4);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean writeLong(long val){
		try{
			src.replaceLong(val, pos);
			notifyWriteListeners(pos, 8);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean writeByte(byte val, int offset){
		try{
			long npos = pos+offset;
			src.replaceByte(val, npos);
			notifyWriteListeners(npos, 1);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean writeShort(short val, int offset){
		try{
			long npos = pos+offset;
			src.replaceShort(val, npos);
			notifyWriteListeners(npos, 2);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean write24(int val, int offset){
		try{
			long npos = pos+offset;
			src.replaceShortish(val, npos);
			notifyWriteListeners(npos, 3);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean writeInt(int val, int offset){
		try{
			long npos = pos+offset;
			src.replaceInt(val, npos);
			notifyWriteListeners(npos, 4);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	public boolean writeLong(long val, int offset){
		try{
			long npos = pos+offset;
			src.replaceLong(val, npos);
			notifyWriteListeners(npos, 8);
			return true;
		}
		catch(Exception ex){return false;}
	}
	
	/*----- Utils -----*/
	
	private void notifyWriteListeners(long pos, int len){
		if(listeners == null) return;
		for(BufferWriteListener l : listeners) l.onBufferWrite(pos, len);
	}
	
	public boolean increment(){
		long max = src.getFileSize();
		if(pos >= max) return false;
		pos++;
		return true;
	}
	
	public long add(long amount){
		long max = src.getFileSize();
		max -= pos;
		if(amount > max) amount = max;
		pos += amount;
		return amount;
	}
	
	public boolean decrement(){
		if(pos <= 0) return false;
		pos--;
		return true;
	}
	
	public long subtract(long amount){
		if(pos < amount) amount = pos;
		pos -= amount;
		return amount;
	}
	
	public BufferReference copy(){
		return new BufferReference(src, pos);
	}
	
}
