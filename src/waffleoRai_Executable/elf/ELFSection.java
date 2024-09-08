package waffleoRai_Executable.elf;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class ELFSection {
	
	/*----- Constants -----*/
	
	public static final int HEADER_SIZE_32 = 0x28;
	public static final int HEADER_SIZE_64 = 0x40;
	
	/*----- Instance Variables -----*/
	
	private String name;
	private byte[] rawData;
	
	//Header
	private int secType;
	private long secFlags;
	private int linkId;
	private int info;
	
	private long vAddr;
	private long romSize;
	private long alignment;
	private long fixedEntrySize;
	
	//Temp for reading
	private int strOfs;
	private long fileOfs;
	
	/*----- Init -----*/
	
	public ELFSection() {
		//TODO?
	}
	
	/*----- Getters -----*/
	
	protected int getStringOffset() {return strOfs;}
	protected long getFileOffset() {return fileOfs;}
	
	public int getType() {return secType;}
	public long getFlags() {return secFlags;}
	public int getLinkId() {return linkId;}
	public int getInfoField() {return info;}
	public long getVirtualAddr() {return vAddr;}
	public long getROMSize() {return romSize;}
	public long getAlignment() {return alignment;}
	public long getFixedEntrySize() {return fixedEntrySize;}
	
	public String getName() {return name;}
	public byte[] getRawData() {return rawData;}
	
	/*----- Setters -----*/
	
	protected void setStringOffset(int val) {strOfs = val;}
	protected void setFileOffset(long val) {fileOfs = val;}
	
	public void setName(String val) {name = val;}
	public void setRawData(byte[] val) {rawData = val;}
	
	/*----- Read -----*/
	
	public long readSectionHeader(BufferReference ref, boolean is64Bit) {
		if(ref == null) return 0L;
		long stpos = ref.getBufferPosition();
		
		strOfs = ref.nextInt();
		secType = ref.nextInt();
		
		if(is64Bit) {
			secFlags = ref.nextLong();
			vAddr = ref.nextLong();
			fileOfs = ref.nextLong();
			romSize = ref.nextLong();
		}
		else {
			secFlags = Integer.toUnsignedLong(ref.nextInt());
			vAddr = Integer.toUnsignedLong(ref.nextInt());
			fileOfs = Integer.toUnsignedLong(ref.nextInt());
			romSize = Integer.toUnsignedLong(ref.nextInt());
		}
		
		linkId = ref.nextInt();
		info = ref.nextInt();
		
		if(is64Bit) {
			alignment = ref.nextLong();
			fixedEntrySize = ref.nextLong();
		}
		else {
			alignment = Integer.toUnsignedLong(ref.nextInt());
			fixedEntrySize = Integer.toUnsignedLong(ref.nextInt());
		}
		
		return ref.getBufferPosition() - stpos;
	}
	
	/*----- Write -----*/
	
	public long serializeHeaderTo(FileBuffer target, boolean is64Bit) {
		if(target == null) return 0L;
		target.addToFile(strOfs);
		target.addToFile(secType);
		
		if(is64Bit) {
			target.addToFile(secFlags);
			target.addToFile(vAddr);
			target.addToFile(fileOfs);
			target.addToFile(romSize);
		}
		else {
			target.addToFile((int)secFlags);
			target.addToFile((int)vAddr);
			target.addToFile((int)fileOfs);
			target.addToFile((int)romSize);
		}
		
		target.addToFile(linkId);
		target.addToFile(info);
		
		if(is64Bit) {
			target.addToFile(alignment);
			target.addToFile(fixedEntrySize);
		}
		else {
			target.addToFile((int)alignment);
			target.addToFile((int)fixedEntrySize);
		}
		
		return 0L;
	}
	
	public FileBuffer serializeHeader(boolean isBigEndian, boolean is64Bit) {
		int alloc = HEADER_SIZE_32;
		if(is64Bit) alloc = HEADER_SIZE_64;
		FileBuffer buff = new FileBuffer(alloc, isBigEndian);
		serializeHeaderTo(buff, is64Bit);
		return buff;
	}

}
