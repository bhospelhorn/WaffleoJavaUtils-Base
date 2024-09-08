package waffleoRai_Executable.elf;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class ELFProgramHeader {
	
	/*----- Constants -----*/
	
	public static final int ENTRY_SIZE_32 = 0x20;
	public static final int ENTRY_SIZE_64 = 0x38;
	
	/*----- Instance Variables -----*/
	
	private int segType;
	private int flags;
	private long alignment;
	
	private long fileOfs;
	
	private long vAddr;
	private long pAddr;
	private long romSize;
	private long ramSize;
	
	/*----- Init -----*/
	
	/*----- Getters -----*/
	
	public int getSegType() {return segType;}
	public int getFlags() {return flags;}
	public long getVirtualAddr() {return vAddr;}
	public long getPhysicalAddr() {return pAddr;}
	public long getROMSize() {return romSize;}
	public long getRAMSize() {return ramSize;}
	public long getAlignment() {return alignment;}
	public long getFileOffset() {return fileOfs;}
	
	/*----- Setters -----*/
	
	public void setFileOffset(long val) {fileOfs = val;}
	
	/*----- Read -----*/
	
	public static ELFProgramHeader read(BufferReference ref, boolean is64Bit) {
		if(ref == null) return null;
		
		ELFProgramHeader ph = new ELFProgramHeader();
		ph.segType = ref.nextInt();
		
		if(is64Bit) {
			ph.flags = ref.nextInt();
			ph.fileOfs = ref.nextLong();
			ph.vAddr = ref.nextLong();
			ph.pAddr = ref.nextLong();
			ph.romSize = ref.nextLong();
			ph.ramSize = ref.nextLong();
			ph.alignment = ref.nextLong();
		}
		else {
			ph.fileOfs = Integer.toUnsignedLong(ref.nextInt());
			ph.vAddr = Integer.toUnsignedLong(ref.nextInt());
			ph.pAddr = Integer.toUnsignedLong(ref.nextInt());
			ph.romSize = Integer.toUnsignedLong(ref.nextInt());
			ph.ramSize = Integer.toUnsignedLong(ref.nextInt());
			ph.flags = ref.nextInt();
			ph.alignment = Integer.toUnsignedLong(ref.nextInt());
		}
		
		return ph;
	}
	
	/*----- Write -----*/
	
	public long serializeMeTo(FileBuffer target, boolean is64Bit) {
		if(target == null) return 0L;
		long stpos = target.getFileSize();
		target.addToFile(segType);
		
		if(is64Bit) {
			target.addToFile(flags);
			target.addToFile(fileOfs);
			target.addToFile(vAddr);
			target.addToFile(pAddr);
			target.addToFile(romSize);
			target.addToFile(ramSize);
			target.addToFile(alignment);
		}
		else {
			target.addToFile((int)fileOfs);
			target.addToFile((int)vAddr);
			target.addToFile((int)pAddr);
			target.addToFile((int)romSize);
			target.addToFile((int)ramSize);
			target.addToFile(flags);
			target.addToFile((int)alignment);
		}
		
		
		return target.getFileSize() - stpos;
	}
	
	public FileBuffer serializeMe(boolean is64Bit, boolean isBigEndian) {
		int alloc = ENTRY_SIZE_32;
		if(is64Bit) alloc = ENTRY_SIZE_64;
		FileBuffer buff = new FileBuffer(alloc, isBigEndian);
		serializeMeTo(buff, is64Bit);
		return buff;
	}

}
