package waffleoRai_Executable.elf;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class ELFSymbol {
	
	/*----- Constants -----*/
	
	public static final int SIZE_32 = 0x10;
	public static final int SIZE_64 = 0x20; //TODO No idea if true
	
	//Binding
	public static final int STB_LOCAL = 0;
	public static final int STB_GLOBAL = 1;
	public static final int STB_WEAK = 2;
	public static final int STB_LOPROC = 13;
	public static final int STB_HIPROC = 15;
	
	//Type
	public static final int STT_NOTYPE = 0;
	public static final int STT_OBJECT = 1;
	public static final int STT_FUNC = 2;
	public static final int STT_SECTION = 3;
	public static final int STT_FILE = 4;
	public static final int STT_LOPROC = 13;
	public static final int STT_HIPROC = 15;
	
	/*----- Instance Variables -----*/
	
	private String name;
	private long addr; //Sometimes holds alignment info or sec offset in reloc files
	private int size;
	
	private int binding;
	private int type;
	private int other;
	
	//----Temp
	private int strOfs;
	private int linkId; //Index of section this symbol table is for

	/*----- Init -----*/
	
	/*----- Getters -----*/
	
	public String getName() {return name;}
	public long getAddress() {return addr;}
	public int getSize() {return size;}
	public int getBinding() {return binding;}
	public int getType() {return type;}
	
	protected int getLinkId() {return linkId;}
	protected int getStringOffset() {return strOfs;}
	
	/*----- Setters -----*/
	
	public void setName(String val) {name = val;}
	
	protected void setStringOffset(int val) {strOfs = val;}
	protected void setLinkId(int val) {linkId = val;}
	
	/*----- Read -----*/
	
	public static ELFSymbol read(BufferReference ref, boolean is64Bit) {
		//I am not sure about the 64 bit format. The doc I have is very old and appears to be 32 bit only?
		if(ref == null) return null;
		ELFSymbol sym = new ELFSymbol();
		
		sym.strOfs = ref.nextInt();
		
		if(is64Bit) {
			//This would not align. Fields are probably in a different order
			//TODO
			sym.addr = ref.nextLong();
		}
		else {
			sym.addr = Integer.toUnsignedLong(ref.nextInt());
		}
		
		sym.size = ref.nextInt();
		int infoRaw = Byte.toUnsignedInt(ref.nextByte());
		sym.binding = (infoRaw >>> 4);
		sym.type = infoRaw & 0xf;
		sym.other = Byte.toUnsignedInt(ref.nextByte());
		sym.linkId = Short.toUnsignedInt(ref.nextShort());
		
		return sym;
	}
	
	/*----- Write -----*/
	
	public long serializeMeTo(FileBuffer target, boolean is64Bit) {
		long stpos = target.getFileSize();
		target.addToFile(strOfs);
		
		if(is64Bit) {
			//TODO
		}
		else {
			target.addToFile((int)addr);
		}
		
		target.addToFile(size);
		target.addToFile((byte)(((binding & 0xf) << 4) | (type & 0xf)));
		target.addToFile((byte)other);
		target.addToFile((short)linkId);
		
		return target.getFileSize() - stpos;
	}
	
}
