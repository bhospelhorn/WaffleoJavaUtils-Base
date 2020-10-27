package waffleoRai_Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class RawDiskBuffer extends CacheFileBuffer{
	
	/* ----- Constants ----- */
	
	public static final int DEFO_MAX_SIZE = 0x100000;
	
	/* ----- Instance Variables ----- */
	
	private int header_size;
	private int data_size; 
	private int footer_size;
	
	//This is relative to raw sector
	private long st_offset; //Offset of data start from start of containing sector
	private long length;
	
	/* ----- Construction ----- */
	
	private RawDiskBuffer(int len_head, int len_dat, int len_foot, int page_count, boolean allowWrite) throws IOException{
		super(len_head + len_dat + len_foot, page_count, allowWrite);
		header_size = len_head;
		data_size = len_dat;
		footer_size = len_foot;
	}
	
	public static RawDiskBuffer openReadOnly(String path, int head_len, int dat_len, boolean endian) throws IOException{
		int page_count = DEFO_MAX_SIZE/(head_len + dat_len);
		long fsz = FileBuffer.fileSize(path);
		return openReadOnly(path, head_len, dat_len, 0, page_count, 0L, fsz, endian);
	}
	
	public static RawDiskBuffer openReadOnly(String path, int head_len, int dat_len, int page_count, int stSec, int edSec, boolean endian) throws IOException{
		long ssize = head_len + dat_len;
		return openReadOnly(path, head_len, dat_len, 0, page_count, (long)stSec * ssize, (long)edSec * ssize, endian);
	}
	
	public static RawDiskBuffer openReadOnly(String path, int head_len, int dat_len, int page_count, long stPos, long edPos, boolean endian) throws IOException{
		return openReadOnly(path, head_len, dat_len, 0, page_count, stPos, edPos, endian);
	}
	
	public static RawDiskBuffer openReadOnly(String path, int head_len, int dat_len, int foot_len, int page_count, int stSec, int edSec, boolean endian) throws IOException{
		long ssize = head_len + dat_len + foot_len;
		return openReadOnly(path, head_len, dat_len, 0, page_count, (long)stSec * ssize, (long)edSec * ssize, endian);
	}
	
	public static RawDiskBuffer openReadOnly(String path, int head_len, int dat_len, int foot_len, int page_count, long stPos, long edPos, boolean endian) throws IOException{
		RawDiskBuffer buffer = new RawDiskBuffer(head_len, dat_len, foot_len, page_count, false);
		
		buffer.setDir(FileBuffer.chopPathToDir(path));
		buffer.setName(FileBuffer.chopPathToFName(path));
		buffer.setExt(FileBuffer.chopPathToExt(path));
		
		buffer.setEndian(endian);
		
		//- Snap positions to sector boundaries
		buffer.length = edPos - stPos;
		int ssize = buffer.getTotalSectorSize();
		buffer.st_offset = stPos%ssize;
		
		stPos -= buffer.st_offset;
		long edsec = edPos/ssize;
		if(edPos % ssize != 0){
			edsec = ((edsec+1) * ssize);
		}
		
		buffer.loadReadOnlyCacheBuffer(path, stPos, edPos);
		
		return buffer;
	}
	
	/* ----- DISK IMAGE POSITIONING ----- */
	
	public FileBuffer getFullSector(int sector_idx){
		long ssize = Integer.toUnsignedLong(getTotalSectorSize());
		long stpos = ssize * (long)sector_idx;
		long edpos = stpos + ssize;
		
		FileBuffer sub = FileBuffer.wrap(super.getBytes(stpos, edpos));
		sub.setEndian(this.isBigEndian());
		return sub;
	}
	
	public FileBuffer getSectorHeader(int sector_idx){
		if(header_size == 0) return null;
		long ssize = Integer.toUnsignedLong(getTotalSectorSize());
		long stpos = ssize * (long)sector_idx;
		
		FileBuffer sub = FileBuffer.wrap(super.getBytes(stpos, stpos + header_size));
		sub.setEndian(this.isBigEndian());
		return sub;
	}
	
	public FileBuffer getSectorFooter(int sector_idx){
		if(footer_size == 0) return null;
		long ssize = Integer.toUnsignedLong(getTotalSectorSize());
		long stpos = ssize * (long)sector_idx;
		stpos += header_size + data_size;

		FileBuffer sub = FileBuffer.wrap(super.getBytes(stpos, stpos + footer_size));
		sub.setEndian(this.isBigEndian());
		return sub;
	}
	
	public int getSectorIndexOf(long data_position){
		data_position += (st_offset - header_size);
		return (int)(data_position/(long)data_size);
	}
	
	protected long getAdjustedOffset(long data_offset){
		data_offset += (st_offset - header_size);
		long sec_idx = data_offset/(long)data_size;
		long sec_off = data_offset%(long)data_size;
		long ssize = Integer.toUnsignedLong(getTotalSectorSize());
		
		long apos = sec_idx * ssize;
		apos += sec_off + header_size;
		
		return apos;
	}
	
	/* ----- BASIC GETTERS ----- */
	
	public int getTotalSectorSize(){
		return super.getPageSize();
	}
	
	public long getFileSize(){
		return length;
	}
	
	public byte getByte(long position){
		long apos = getAdjustedOffset(position);
		return super.getByte(apos);
	}
	
	/* ----- CONTENT RETRIEVAL ----- */
	
	public byte[] getBytes(){
		long fsize = getFileSize();
		if(fsize <= 0x7FFFFFFF) return getBytes(0, fsize);
		throw new IndexOutOfBoundsException("CachedBuffer is too large to store in byte array!");
	}
	
	public byte[] getBytes(long stOff, long edOff){
		long llen = edOff - stOff;
		if(llen > 0x7FFFFFFFL) throw new IndexOutOfBoundsException("Byte buffer cannot exceed 2GB in size!");
		int len = (int)llen;
		
		//Get sector coordinates of start...
		stOff += (st_offset - header_size);
		long sec_idx = stOff/(long)data_size;
		long sec_off = stOff%(long)data_size; //Data relative...
		long ssize = Integer.toUnsignedLong(getTotalSectorSize());
		
		//Allocate byte array
		byte[] out = new byte[len];
		long spos = (sec_idx * ssize) + sec_off + header_size;
		int apos = 0;
		int remain = len;
		while(remain > 0){
			//Do next sector
			int sleft = data_size - (int)sec_off;
			if(remain < sleft) sleft = remain;
			
			for(int i = 0; i < sleft; i++){
				out[apos++] = super.getByte(spos);
				remain--;
			}
			
			sec_off = 0;
			sec_idx++;
			spos = (sec_idx * ssize) + header_size;
		}
		
		return out;
	}
	
	/* ----- WRITING TO DISK ----- */
	
	public void appendToFile(String path, long stPos, long edPos) throws IOException, NoSuchFileException{
		long remaining = edPos - stPos;
		
		//Get sector coordinates of start...
		stPos += (st_offset - header_size);
		long sec_idx = stPos/(long)data_size;
		long sec_off = stPos%(long)data_size; //Data relative...
		long ssize = Integer.toUnsignedLong(getTotalSectorSize());
		
		long spos = (sec_idx * ssize) + sec_off + header_size;
		while(remaining > 0){
			//Do next sector
			int sleft = data_size - (int)sec_off;
			if(remaining < (long)sleft) sleft = (int)remaining;
			
			byte[] get = super.getBytes(spos, spos+sleft);
			Files.write(Paths.get(path), get, StandardOpenOption.APPEND);
			
			sec_off = 0;
			sec_idx++;
			spos = (sec_idx * ssize) + header_size;
			
			remaining -= sleft;	
		}

	}
	
	public long writeToStream(OutputStream out, long stPos, long edPos) throws IOException{
		//Get write amount
		long remaining = edPos - stPos;
		
		//Get sector coordinates of start...
		stPos += (st_offset - header_size);
		long sec_idx = stPos/(long)data_size;
		long sec_off = stPos%(long)data_size; //Data relative...
		long ssize = Integer.toUnsignedLong(getTotalSectorSize());
		
		long wcount = 0L;
		long spos = (sec_idx * ssize) + sec_off + header_size;
		while(remaining > 0){
			//Do next sector
			int sleft = data_size - (int)sec_off;
			if(remaining < (long)sleft) sleft = (int)remaining;
			
			byte[] get = super.getBytes(spos, spos+sleft);
			out.write(get);
			
			sec_off = 0;
			sec_idx++;
			spos = (sec_idx * ssize) + header_size;
			
			remaining -= sleft;
			wcount += sleft;
		}
		
		return wcount;
	}
	
	/* ----- STATUS CHECKERS ----- */
	
	public boolean offsetValid(long off){
  		if(off < 0) return false;
  		if(off >= getFileSize()) return false;
  		
  		return true;
  	}

	/* ----- CONVERSION ----- */
	
	public ByteBuffer toByteBuffer(){
		long fsize = getFileSize();
  		if(fsize > 0x7FFFFFFF) throw new IndexOutOfBoundsException("Cannot store >2GB in byte buffer!");
  		return toByteBuffer(0, fsize);
  	}
	
	public ByteBuffer toByteBuffer(long stPos, long edPos){
		//BE KIND REWIND
		long llen = edPos - stPos;
		if(llen > 0x7FFFFFFFL) throw new IndexOutOfBoundsException("Byte buffer cannot exceed 2GB in size!");
		int len = (int)llen;
		
		ByteBuffer bb = ByteBuffer.allocateDirect(len);
		bb.put(getBytes(stPos, edPos));
		bb.rewind();
		
		return bb;
	}
	
	public FileBuffer createCopy(long stPos, long edPos) throws IOException{
		//TODO
		return null;
	}
	
	/* ----- FILE STATISTICS/ INFO ----- */
  	
  	public String typeString(){
  		return "RawDiskBuffer";
  	}
	
}
