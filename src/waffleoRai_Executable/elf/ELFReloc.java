package waffleoRai_Executable.elf;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class ELFReloc {
	
	public static final int SIZE_32 = 0x8;
	public static final int SIZE_64 = 0xc; //TODO No idea if true
	
	public static final int SIZE_32_A = 0xc;
	public static final int SIZE_64_A = 0x10; //TODO No idea if true
	
	/*----- Instance Variables -----*/
	
	private long addr;
	private int symIdx;
	private int type;
	private int addend;
	
	/*----- Init -----*/
	
	/*----- Getters -----*/
	
	public long getAddress() {return addr;}
	public int getSymbolIndex() {return symIdx;}
	public int getType() {return type;}
	public int getAddend() {return addend;}
	
	/*----- Setters -----*/
	
	/*----- Read -----*/
	
	public static ELFReloc read(BufferReference ref, boolean is64Bit, boolean inclAddend) {
		if(ref == null) return null;
		ELFReloc rel = new ELFReloc();
		
		if(is64Bit) {
			rel.addr = ref.nextLong();
		}
		else {
			rel.addr = Integer.toUnsignedLong(ref.nextInt());
		}
		
		int infoRaw = ref.nextInt();
		rel.symIdx = infoRaw >>> 8;
		rel.type = infoRaw & 0xff;
		
		if(inclAddend) rel.addend = ref.nextInt();
		
		return rel;
	}
	
	/*----- Write -----*/
	
	public long serializeMeTo(FileBuffer target, boolean is64Bit, boolean inclAddend) {
		if(target == null) return 0L;
		long stpos = target.getFileSize();
		
		if(is64Bit) target.addToFile(addr);
		else target.addToFile((int)addr);
		
		int infoRaw = type & 0xff;
		infoRaw |= symIdx << 8;
		target.addToFile(infoRaw);
		
		if(inclAddend) target.addToFile(addend);
		
		return target.getFileSize() - stpos;
	}

}
