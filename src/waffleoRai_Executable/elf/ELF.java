package waffleoRai_Executable.elf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

public class ELF {
	
	/*----- Constants -----*/
	
	public static final byte[] MAGIC = {0x7f, 0x45, 0x4c, 0x46};
	
	/*----- Instance Variables -----*/
	
	//Header
	private boolean is64Bit = false;
	private boolean isBigEndian = true;
	private int version = 1;
	private int abiIdent = 0;
	private int abiVer = 0;
	private int objType = 0;
	private int machine = 0;
	private int flags = 0;
	
	private long entryAddr = 0L;
	
	//Program Headers
	private ArrayList<ELFProgramHeader> progHeaders;
	
	//Sections
	private ArrayList<ELFSection> sectionList;
	private Map<String, ELFSection> secNameMap;
	
	//Temp header fields
	private long phOff;
	private long shOff;
	private int pheCount;
	private int sheCount;
	private int shstrIdx = -1;
	
	/*----- Init -----*/
	
	private ELF() {
		progHeaders = new ArrayList<ELFProgramHeader>(4);
		sectionList = new ArrayList<ELFSection>(4);
		secNameMap = new HashMap<String, ELFSection>();
	}
	
	/*----- Getters -----*/
	
	public long getEntryAddress() {return entryAddr;}
	
	public List<String> getSortedSectionNames(){
		List<String> list = new ArrayList<String>(secNameMap.size()+1);
		list.addAll(secNameMap.keySet());
		Collections.sort(list);
		return list;
	}
	
	public List<String> getOrderedSectionNames(){
		List<String> list = new ArrayList<String>(sectionList.size()+1);
		for(ELFSection sec : sectionList) {
			list.add(sec.getName());
		}
		return list;
	}
	
	public ELFSection getSection(int index) {
		if(index < 0) return null;
		if(index >= sectionList.size()) return null;
		return sectionList.get(index);
	}
	
	public ELFSection getSectionByName(String name) {
		return secNameMap.get(name);
	}
	
	public List<ELFSymbol> getSymbolTable(){
		//Check for symbol section
		ELFSection sec = secNameMap.get(".symtab");
		if(sec == null) return null;
		
		FileBuffer secdat = FileBuffer.wrap(sec.getRawData());
		secdat.setEndian(isBigEndian);
		int tsize = (int)secdat.getFileSize();
		if(this.is64Bit) tsize /= ELFSymbol.SIZE_64;
		tsize /= ELFSymbol.SIZE_32;
		
		List<ELFSymbol> stbl = new ArrayList<ELFSymbol>(tsize+1);
		BufferReference ref = secdat.getReferenceAt(0L);
		while(ref.hasRemaining()) {
			ELFSymbol sym = ELFSymbol.read(ref, is64Bit);
			if(sym != null) stbl.add(sym);
		}
		
		//Get names
		sec = secNameMap.get(".strtab");
		if(sec != null) {
			secdat = FileBuffer.wrap(sec.getRawData());
			for(ELFSymbol sym : stbl) {
				sym.setName(secdat.getASCII_string(sym.getStringOffset(), '\0'));
			}
		}
		
		return stbl;
	}
	
	public List<ELFReloc> getRelocTableForSection(String secName){
		boolean rela = false;
		String rsecname = ".rel" + secName;
		ELFSection sec = secNameMap.get(rsecname);
		if(sec == null) {
			//Try rela
			rsecname = ".rela" + secName;
			sec = secNameMap.get(rsecname);
			rela = true;
			if(sec == null) return null;
		}
		
		FileBuffer secdat = FileBuffer.wrap(sec.getRawData());
		secdat.setEndian(isBigEndian);
		int relCount = (int)secdat.getFileSize();
		if(rela) {
			if(is64Bit) relCount /= ELFReloc.SIZE_64_A;
			else relCount /= ELFReloc.SIZE_32_A;
		}
		else {
			if(is64Bit) relCount /= ELFReloc.SIZE_64;
			else relCount /= ELFReloc.SIZE_32;
		}
		
		List<ELFReloc> rellist = new ArrayList<ELFReloc>(relCount+1);
		BufferReference ref = secdat.getReferenceAt(0L);
		while(ref.hasRemaining()) {
			ELFReloc rel = ELFReloc.read(ref, is64Bit, rela);
			if(rel != null) rellist.add(rel);
		}
		
		return rellist;
	}
	
	/*----- Read -----*/
	
 	private long readHeader(BufferReference ref) throws UnsupportedFileTypeException {
		if(ref == null) return 0L;
		long stpos = ref.getBufferPosition();
		
		for(int i = 0; i < 4; i++) {
			byte b = ref.nextByte();
			if(b != MAGIC[i]) {
				throw new UnsupportedFileTypeException("ELF.readHeader || Magic number does not match!");
			}
		}
		
		byte b = ref.nextByte();
		if(b == 1) is64Bit = false;
		else if(b == 2) is64Bit = true;
		else {
			throw new UnsupportedFileTypeException("ELF.readHeader || 64-bit marker " + b + " is not valid!");
		}
		
		b = ref.nextByte();
		if(b == 1) isBigEndian = false;
		else if(b == 2) isBigEndian = true;
		else {
			throw new UnsupportedFileTypeException("ELF.readHeader || Byte order marker " + b + " is not valid!");
		}
		ref.setByteOrder(isBigEndian);
		
		version = Byte.toUnsignedInt(ref.nextByte());
		abiIdent = Byte.toUnsignedInt(ref.nextByte());
		abiVer = Byte.toUnsignedInt(ref.nextByte());
		ref.add(7); //Reserved
		objType = Short.toUnsignedInt(ref.nextShort());
		machine = Short.toUnsignedInt(ref.nextShort());
		ref.add(4); //Version again?
		
		if(is64Bit) {
			entryAddr = ref.nextLong();
			phOff = ref.nextLong();
			shOff = ref.nextLong();
		}
		else {
			entryAddr = Integer.toUnsignedLong(ref.nextInt());
			phOff = Integer.toUnsignedLong(ref.nextInt());
			shOff = Integer.toUnsignedLong(ref.nextInt());
		}
		
		flags = ref.nextInt();
		ref.add(2L); //Header size
		ref.add(2L); //PH entry size
		pheCount = Short.toUnsignedInt(ref.nextShort());
		ref.add(2L); //SH entry size
		sheCount = Short.toUnsignedInt(ref.nextShort());
		shstrIdx = Short.toUnsignedInt(ref.nextShort());
		
		return ref.getBufferPosition() - stpos;
		
	}
	
	public static ELF read(FileBuffer buffer) throws UnsupportedFileTypeException {
		if(buffer == null) return null;
		ELF elf = new ELF();
		
		elf.readHeader(buffer.getReferenceAt(0L));
		buffer.setEndian(elf.isBigEndian);
		
		//Read program header table
		if(elf.phOff > 0) {
			elf.progHeaders.ensureCapacity(elf.pheCount);
			BufferReference readRef = buffer.getReferenceAt(elf.phOff);
			for(int i = 0; i < elf.pheCount; i++) {
				ELFProgramHeader ph = ELFProgramHeader.read(readRef, elf.is64Bit);
				elf.progHeaders.add(ph);
			}
		}
		
		//Read section header table
		if(elf.shOff > 0) {
			elf.sectionList.ensureCapacity(elf.sheCount);
			BufferReference readRef = buffer.getReferenceAt(elf.shOff);
			for(int i = 0; i < elf.sheCount; i++) {
				ELFSection sec = new ELFSection();
				sec.readSectionHeader(readRef, elf.is64Bit);
				elf.sectionList.add(sec);
			}
		}
		
		//Parse sections (including getting their names)
		if(elf.shstrIdx >= 0) {
			ELFSection shstrtbl = elf.sectionList.get(elf.shstrIdx);
			long toff = shstrtbl.getFileOffset();
			for(ELFSection sec : elf.sectionList) {
				String n = buffer.getASCII_string(toff + sec.getStringOffset(), '\0');
				sec.setName(n);
				if(n != null) elf.secNameMap.put(n, sec);
			}
		}
		for(ELFSection sec : elf.sectionList) {
			if(sec.getType() == ELFSecType.SHT_NOBITS) continue;
			int size = (int)sec.getROMSize();
			if(size < 1) continue;
			
			BufferReference readRef = buffer.getReferenceAt(sec.getFileOffset());
			byte[] dat = new byte[size];
			for(int i = 0; i < size; i++) {
				dat[i] = readRef.nextByte();
			}
			sec.setRawData(dat);
		}
		
		return elf;
	}
	
	/*----- Write -----*/
	
	public FileBuffer serializeMe() {
		//TODO
		return null;
	}
	
	
	

}
