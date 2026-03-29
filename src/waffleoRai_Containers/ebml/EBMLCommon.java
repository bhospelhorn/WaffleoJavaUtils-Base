package waffleoRai_Containers.ebml;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import waffleoRai_Files.tree.FileNode;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class EBMLCommon {
	
	public static final int TYPE_UNK = -1;
	public static final int TYPE_BLOB = 0;
	public static final int TYPE_UINT = 1;
	public static final int TYPE_INT = 2;
	public static final int TYPE_FLOAT = 3;
	public static final int TYPE_ASCII = 4;
	public static final int TYPE_UTF8 = 5;
	public static final int TYPE_DATE = 6;
	public static final int TYPE_MASTER = 0x10;
	
	public static final int ID_EBML = 0xa45dfa3; //0x1a45dfa3
	public static final int ID_EBML_VER = 0x286; //0x4286
	public static final int ID_EBML_READVER = 0x2f7; //0x42f7
	public static final int ID_EBML_MAXIDLEN = 0x2f2; //0x42f2
	public static final int ID_EBML_MAXSIZELEN = 0x2f3; //0x42f3
	public static final int ID_EBML_DOCTYPE = 0x282; //0x4282
	public static final int ID_EBML_DOCTYPE_VER = 0x287; //0x4287
	public static final int ID_EBML_DOCTYPE_READVER = 0x285; //0x4285
	
	public static int calcVLQLength(int value) {
		if(value < 0x7f) return 1;
		else if(value < 0x3fff) return 2;
		else if(value < 0x1fffff) return 3;
		else if(value < 0x0fffffff) return 4;
		return 5;
	}
	
	public static int calcVLQLength(long value) {
		if(value < 0x7fL) return 1;
		else if(value < 0x3fffL) return 2;
		else if(value < 0x1fffffL) return 3;
		else if(value < 0x0fffffffL) return 4;
		else if(value < 0x7ffffffffL) return 5;
		else if(value < 0x3ffffffffffL) return 6;
		else if(value < 0x1ffffffffffffL) return 7;
		else if(value < 0x0ffffffffffffffL) return 8;
		return 0;
	}
	
	public static int vlqToValue(int rawVLQ) {
		//How many bytes does the value use?
		int clearMask = 0;
		if(rawVLQ < 0x100) clearMask = 0x80;
		else if(rawVLQ < 0x8000) clearMask = 0x4000;
		else if(rawVLQ < 0x400000) clearMask = 0x200000;
		else if(rawVLQ < 0x20000000) clearMask = 0x10000000;
		return rawVLQ & ~clearMask;
	}
	
	public static int parseNextVLQ(BufferReference data) {
		int val = 0;
		int b0 = Byte.toUnsignedInt(data.nextByte());
		int b0Mask = 0x7f;
		int count = 0;
		while((~b0Mask & b0) == 0) {
			b0Mask >>>= 1;
			count++;
		}
		
		val = b0 & b0Mask;
		while(count-- > 0) {
			val <<= 8;
			val |= Byte.toUnsignedInt(data.nextByte());
		}
		return val;
	}
	
	public static long parseNextLongVLQ(BufferReference data) {
		long val = 0;
		int b0 = Byte.toUnsignedInt(data.nextByte());
		int b0Mask = 0x7f;
		int count = 0;
		while((~b0Mask & b0) == 0) {
			b0Mask >>>= 1;
			count++;
		}
		
		val = Integer.toUnsignedLong(b0 & b0Mask);
		while(count-- > 0) {
			val <<= 8;
			val |= Byte.toUnsignedLong(data.nextByte());
		}
		return val;
	}
	
	public static byte[] encodeVLQ(int val) {
		byte[] bb = null;
		int onMask = 0;
		int offMask = 0;
		int bCount = 0;
		int shamt = 0;
		
		if(val < 0x7f) bCount = 1;
		else if(val < 0x3fff) bCount = 2;
		else if(val < 0x1fffff) bCount = 3;
		else if(val < 0x0fffffff) bCount = 4;
		else {
			return encodeVLQ(Integer.toUnsignedLong(val));
		}
		
		shamt = (bCount - 1) << 3;
		onMask = 0x80 >>> (bCount - 1);
		offMask = 0xff >>> (bCount - 1);
		bb = new byte[bCount];
		bb[0] = (byte)(((val >>> shamt) & offMask) | onMask);
		for(int i = 1; i < bCount; i++) {
			shamt -= 8;
			bb[i] = (byte)((val >>> shamt) & 0xff);
		}
		return bb;
	}

	public static byte[] encodeVLQ(long val) {
		byte[] bb = null;
		long onMask = 0;
		long offMask = 0;
		int bCount = 0;
		int shamt = 0;
		
		if(val < 0x7fL) bCount = 1;
		else if(val < 0x3fffL) bCount = 2;
		else if(val < 0x1fffffL) bCount = 3;
		else if(val < 0x0fffffffL) bCount = 4;
		else if(val < 0x7ffffffffL) bCount = 5;
		else if(val < 0x3ffffffffffL) bCount = 6;
		else if(val < 0x1ffffffffffffL) bCount = 7;
		else if(val < 0x0ffffffffffffffL) bCount = 8;
		
		shamt = (bCount - 1) << 3;
		onMask = 0x80L >>> (bCount - 1);
		offMask = 0xffL >>> (bCount - 1);
		bb = new byte[bCount];
		bb[0] = (byte)(((val >>> shamt) & offMask) | onMask);
		for(int i = 1; i < bCount; i++) {
			shamt -= 8;
			bb[i] = (byte)((val >>> shamt) & 0xff);
		}
		return bb;
	}
	
	public static void loadStandardDefinitions(Map<Integer, EBMLFieldDef> defMap) {
		defMap.put(EBMLCommon.ID_EBML, 
				new EBMLFieldDef(EBMLCommon.TYPE_MASTER, EBMLCommon.ID_EBML, "EBML", 0, true, false));
		defMap.put(EBMLCommon.ID_EBML_VER, 
				new EBMLFieldDef(EBMLCommon.TYPE_UINT, EBMLCommon.ID_EBML_VER, "EBMLVersion", 0, false, false));
		defMap.put(EBMLCommon.ID_EBML_READVER, 
				new EBMLFieldDef(EBMLCommon.TYPE_UINT, EBMLCommon.ID_EBML_READVER, "EBMLReadVersion", 0, false, false));
		defMap.put(EBMLCommon.ID_EBML_MAXIDLEN, 
				new EBMLFieldDef(EBMLCommon.TYPE_UINT, EBMLCommon.ID_EBML_MAXIDLEN, "EBMLMaxIDLength", 0, false, false));
		defMap.put(EBMLCommon.ID_EBML_MAXSIZELEN, 
				new EBMLFieldDef(EBMLCommon.TYPE_UINT, EBMLCommon.ID_EBML_MAXSIZELEN, "EBMLMaxSizeLength", 0, false, false));
		defMap.put(EBMLCommon.ID_EBML_DOCTYPE, 
				new EBMLFieldDef(EBMLCommon.TYPE_ASCII, EBMLCommon.ID_EBML_DOCTYPE, "DocType", 0, false, false));
		defMap.put(EBMLCommon.ID_EBML_DOCTYPE_VER, 
				new EBMLFieldDef(EBMLCommon.TYPE_UINT, EBMLCommon.ID_EBML_DOCTYPE_VER, "DocTypeVersion", 0, false, false));
		defMap.put(EBMLCommon.ID_EBML_DOCTYPE_READVER, 
				new EBMLFieldDef(EBMLCommon.TYPE_UINT, EBMLCommon.ID_EBML_DOCTYPE_READVER, "DocTypeReadVersion", 0, false, false));
	}
	
	public static void loadDefinitionCSV(BufferedReader source, Map<Integer, EBMLFieldDef> defMap) throws IOException {
		if(source == null) return;
		if(defMap == null) return;
		
		int col_rawid = -1;
		int col_baseid = -1;
		int col_name = -1;
		int col_type = -1;
		int col_op = -1;
		int col_mult = -1;
		int col_ver = -1;
		int col_fmt = -1;
		
		String line = null;
		while((line = source.readLine()) != null) {
			if(line.isEmpty()) continue;
			String[] fields = line.split(",");
			if(line.startsWith("#")) {
				//Header
				for(int i = 0; i < fields.length; i++) {
					String s = fields[i].trim();
					if(s.startsWith("#")) s = s.substring(1);
					s = s.toUpperCase();
					if(s.equals("RAW_ID")) col_rawid = i;
					else if(s.equals("BASE_ID")) col_baseid = i;
					else if(s.equals("NAME")) col_name = i;
					else if(s.equals("TYPE")) col_type = i;
					else if(s.equals("IS_OPTIONAL")) col_op = i;
					else if(s.equals("MULT_OK")) col_mult = i;
					else if(s.equals("MIN_VER")) col_ver = i;
					else if(s.equals("TEXT_FMT")) col_fmt = i;
				}
			}
			else {
				EBMLFieldDef def = new EBMLFieldDef();
				if((col_baseid >= 0) && (col_baseid < fields.length)) {
					String s = fields[col_baseid].trim();
					def.baseId = Integer.parseUnsignedInt(s.substring(2), 16);
				}
				else {
					//Try to derive from raw id
					if((col_rawid >= 0) && (col_rawid < fields.length)) {
						String s = fields[col_rawid].trim();
						def.baseId = vlqToValue(Integer.parseUnsignedInt(s.substring(2), 16));
					}
					else def = null;
				}
				
				if(def != null) {
					//Read the rest of the fields
					if((col_name >= 0) && (col_name < fields.length)) {
						def.stringId = fields[col_name].trim();
					}
					if((col_type >= 0) && (col_type < fields.length)) {
						char tc = fields[col_type].trim().charAt(0);
						switch(tc) {
						case 'u': def.type = EBMLCommon.TYPE_UINT; break;
						case 'i': def.type = EBMLCommon.TYPE_INT; break;
						case 'f': def.type = EBMLCommon.TYPE_FLOAT; break;
						case '8': def.type = EBMLCommon.TYPE_UTF8; break;
						case 's': def.type = EBMLCommon.TYPE_ASCII; break;
						case 'd': def.type = EBMLCommon.TYPE_DATE; break;
						case 'b': def.type = EBMLCommon.TYPE_BLOB; break;
						case 'm': def.type = EBMLCommon.TYPE_MASTER; break;
						}
					}
					if((col_op >= 0) && (col_op < fields.length)) {
						String s = fields[col_op].trim();
						if(s.equals("1")) def.optional = true;
						else def.optional = false;
					}
					if((col_mult >= 0) && (col_mult < fields.length)) {
						String s = fields[col_mult].trim();
						if(s.equals("1")) def.multOk = true;
						else def.multOk = false;
					}
					if((col_ver >= 0) && (col_ver < fields.length)) {
						String s = fields[col_ver].trim();
						try{def.minVer = Integer.parseInt(s);}
						catch(NumberFormatException ex) {} //Leave for now
					}
					if((col_fmt >= 0) && (col_fmt < fields.length)) {
						def.fmtPref = fields[col_fmt].trim();
					}
					
					defMap.put(def.baseId, def);
				}
			}
		}
	}
	
	public static long readIntElementLong(EBMLElement element) {
		if(element == null) return -1L;
		if(element instanceof EBMLIntElement) {
			EBMLIntElement ee = (EBMLIntElement)element;
			return Integer.toUnsignedLong(ee.getValue());
		}
		else if(element instanceof EBMLBigIntElement) {
			EBMLBigIntElement ee = (EBMLBigIntElement)element;
			return ee.getValue();
		}
		return -1L;
	}
	
	public static int readIntElement(EBMLElement element) {
		if(element == null) return -1;
		if(element instanceof EBMLIntElement) {
			EBMLIntElement ee = (EBMLIntElement)element;
			return ee.getValue();
		}
		else if(element instanceof EBMLBigIntElement) {
			EBMLBigIntElement ee = (EBMLBigIntElement)element;
			return (int)ee.getValue();
		}
		return -1;
	}
	
	public static double readFloatElement(EBMLElement element) {
		if(element == null) return Double.NaN;
		if(element instanceof EBMLFloatElement) {
			EBMLFloatElement ee = (EBMLFloatElement)element;
			return ee.getValue();
		}
		return Double.NaN;
	}
	
	public static String readStringElement(EBMLElement element) {
		if(element == null) return null;
		if(element instanceof EBMLStringElement) {
			EBMLStringElement ee = (EBMLStringElement)element;
			return ee.getValue();
		}
		return null;
	}
	
	public static byte[] loadBlobElement(EBMLElement element) {
		if(element == null) return null;
		if(element instanceof EBMLExtBlobElement) {
			EBMLExtBlobElement ee = (EBMLExtBlobElement)element;
			try {
				FileNode src = ee.getSourceReference();
				if(src != null) {
					long len = src.getLength();
					if(len < 0x40000000L) {
						FileBuffer loaded = src.loadDecompressedData();
						loaded.setCurrentPosition(0L);
						int ilen = (int)len;
						byte[] barr = new byte[ilen];
						for(int i = 0; i < ilen; i++) barr[i] = loaded.nextByte();
						loaded.dispose();
						return barr;
					}
				}
			}
			catch(IOException ex) {ex.printStackTrace();}
		}
		return null;
	}
	
	public static FileNode readExternalBlobElement(EBMLElement element) {
		if(element == null) return null;
		if(element instanceof EBMLExtBlobElement) {
			EBMLExtBlobElement ee = (EBMLExtBlobElement)element;
			return ee.getSourceReference();
		}
		return null;
	}
	
}
