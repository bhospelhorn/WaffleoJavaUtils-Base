package waffleoRai_Containers;

import java.io.IOException;

import waffleoRai_Utils.FileBuffer;

public class ISOUtils {

	/*----- Checksums -----*/
	
	public static final int EDC_OFFSET_M1 = 0x810;
	public static final int EDC_OFFSET_M2F1 = 0x818;
	public static final int EDC_OFFSET_M2F2 = 0x92c;
	public static final int ECC_AREA_START = 0xc;
	public static final int ECCP_OFFSET = 0x81c;
	public static final int ECCP_SIZE = (43 << 2);
	public static final int ECCQ_OFFSET = 0x81c + ECCP_SIZE;
	
	//Info/algorithms from nocash's PSX document
	
	private static int[] edcTable;
	
	private static int []gf8LogTable;
	private static int []gf8ILogTable;
	private static int [][]gf8ProductTable;
	
	public static int[] getEDCTable() {
		if(edcTable == null) {
			edcTable = new int[256];
			for(int i = 0; i < 256; i++) {
				int x = i;
				for(int j = 0; j < 8; j++) {
					boolean bit = (x & 0x1) != 0;
					x >>>= 1;
					if(bit) x ^= 0xd8018001;
				}
				edcTable[i] = x;
			}
		}
		return edcTable;
	}
	
	private static int genGF8TablesInner(int a, int b) {
		if(a > 0) {
			a = gf8LogTable[a] - b;
			if(a < 0) a += 255;
			a = gf8ILogTable[a];
		}
		return a;
	}
	
	private static void genGF8Tables() {
		gf8LogTable = new int[256];
		gf8ILogTable = new int[256];
		gf8ProductTable = new int[43][256];
		
		int x = 1; //gf8LogTable[0] = 255;
		for(int i = 0; i < 255; i++) {
			gf8LogTable[x] = i;
			gf8ILogTable[i] = x;
			boolean bit = (x & 0x80) != 0;
			x <<= 1; x &= 0xff; //???
			if(bit) x ^= 0x1d;
		}
		
		for(int j = 0; j < 43; j++) {
			int xx = gf8ILogTable[44 - j];
			int yy = genGF8TablesInner(xx ^ 1, 0x19);
			xx = genGF8TablesInner(xx, 0x1);
			xx = genGF8TablesInner(xx ^ 1, 0x18);
			xx = gf8LogTable[xx];
			yy = gf8LogTable[yy];
			
			gf8ProductTable[j][0] = 0;
			for(int k = 1; k < 256; k++) {
				x = xx + gf8LogTable[k];
				if(x >= 255) x -= 255;
				int y = yy + gf8LogTable[k];
				if(y >= 255) y -= 255;
				gf8ProductTable[j][k] = gf8ILogTable[x] + (gf8ILogTable[y] << 8);
			}
		}
	}
	
	public static int calculateEDC(byte[] data, int offset, int len) {
		if(data == null) return 0;
		getEDCTable();
		
		int x = 0;
		for(int j = 0; j < len; j++){
			int b = Byte.toUnsignedInt(data[offset+j]);
			x ^= b;
			x = (x >>> 8) ^ edcTable[x & 0xff];
		}
		
		return x;
	}
	
	public static int calculateEDC(FileBuffer data) {
		if(data == null) return 0;
		getEDCTable();
		
		int x = 0;
		long cpos = 0;
		long fsize = data.getFileSize();
		while(cpos < fsize) {
			int b = Byte.toUnsignedInt(data.getByte(cpos));
			x ^= b;
			x = (x >>> 8) ^ edcTable[x & 0xff];
			cpos++;
		}
		
		return x;
	}
	
	private static void calculateParity(byte[] data, int outOff, int len, int j0, int step1, int step2) {
		if(gf8ProductTable == null) genGF8Tables();
		
		if(data == null || data.length < ISO.SECSIZE) return;
		
		int cpos = ECC_AREA_START;
		int opos = outOff;
		for(int i = 0; i < len; i++) {
			int bpos = cpos;
			int x = 0; int y = 0;
			for(int j = j0; j < 43; j++) {
				int b0 = Byte.toUnsignedInt(data[cpos]);
				int b1 = Byte.toUnsignedInt(data[cpos+1]);
				x ^= gf8ProductTable[j][b0];
				y ^= gf8ProductTable[j][b1];
				cpos += step1;
				if((step1 == (44 << 1)) && (cpos >= ECCQ_OFFSET)) {
					cpos -= (1118 << 1);
				}
			}
			data[opos + (len << 1)] = (byte)(x & 0xff);
			data[opos + (len << 1) + 1] = (byte)(y & 0xff);
			data[opos] = (byte)(x >>> 8);
			data[opos + 1] = (byte)(y >>> 8);
			opos += 2;
			cpos = bpos + step2;
		}
	}
	
	private static byte[] calculateParity(FileBuffer data, int len, int j0, int step1, int step2) {
		if(gf8ProductTable == null) genGF8Tables();
		
		byte[] out = new byte[len << 2];
		
		int cpos = 0;
		int opos = 0;
		int fsize = (int)data.getFileSize();
		for(int i = 0; i < len; i++) {
			int bpos = cpos;
			int x = 0; int y = 0;
			for(int j = j0; j < 43; j++) {
				int b0 = Byte.toUnsignedInt(data.getByte(cpos));
				int b1 = Byte.toUnsignedInt(data.getByte(cpos+1));
				x ^= gf8ProductTable[j][b0];
				y ^= gf8ProductTable[j][b1];
				cpos += step1;
				if((step1 == (44 << 1)) && (cpos >= fsize)) {
					cpos -= (1118 << 1);
				}
			}
			out[opos + (len << 1)] = (byte)(x & 0xff);
			out[opos + (len << 1) + 1] = (byte)(y & 0xff);
			out[opos] = (byte)(x >>> 8);
			out[opos + 1] = (byte)(y >>> 8);
			opos += 2;
			cpos = bpos + step2;
		}
		
		return out;
	}
	
	public static void calculateParityP(byte[] data) {
		if(data == null) return;
		calculateParity(data, ECCP_OFFSET, 43, 19, 43 << 1, 2);
	}
	
	public static byte[] calculateParityP(FileBuffer data) {
		if(data == null) return null;
		return calculateParity(data, 43, 19, 43 << 1, 2);
	}
	
	public static void calculateParityQ(byte[] data) {
		if(data == null) return;
		calculateParity(data, ECCQ_OFFSET, 26, 0, 44 << 1, 43 << 1);
	}
	
	public static byte[] calculateParityQ(FileBuffer data) {
		if(data == null) return null;
		return calculateParity(data, 26, 0, 44 << 1, 43 << 1);
	}
	
	public static boolean updateSectorChecksumsMode1(byte[] rawSector) {
		if(rawSector == null) return false;
		if(rawSector.length < ISO.SECSIZE) return false;
		
		int edc = calculateEDC(rawSector, 0, EDC_OFFSET_M1);
		for(int i = 0; i < 4; i++) {
			rawSector[EDC_OFFSET_M1 + i] = (byte)(edc & 0xff);
			edc >>>= 8;
		}
		for(int i = 4; i < 12; i++) {
			rawSector[EDC_OFFSET_M1 + i] = (byte)0;
		}
		
		//ECC1
		calculateParityP(rawSector);
		
		//ECC2
		calculateParityQ(rawSector);
		
		return true;
	}
	
	public static boolean updateSectorChecksumsMode1(FileBuffer rawSector) {
		if(rawSector == null) return false;
		if(rawSector.readOnly()) return false;
		if(rawSector.getFileSize() < ISO.SECSIZE) return false;
		
		//Zero out checksum area
		for(long i = EDC_OFFSET_M1; i < ISO.SECSIZE; i++) rawSector.replaceByte((byte)0, i);
		FileBuffer region = rawSector.createReadOnlyCopy(0, EDC_OFFSET_M1);
		int edc = calculateEDC(region);
		try {region.dispose();} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		rawSector.setEndian(false);
		rawSector.replaceInt(edc, EDC_OFFSET_M1);
		
		//ECC1
		region = rawSector.createReadOnlyCopy(ECC_AREA_START, ECCP_OFFSET);
		byte[] p = calculateParityP(region);
		try {region.dispose();} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		for(int i = 0; i < ECCP_SIZE; i++) rawSector.replaceByte(p[i], ECCP_OFFSET + i);
		
		//ECC2
		region = rawSector.createReadOnlyCopy(ECC_AREA_START, ECCQ_OFFSET);
		byte[] q = calculateParityQ(region);
		try {region.dispose();} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		for(int i = 0; i < q.length; i++) rawSector.replaceByte(q[i], ECCQ_OFFSET + i);
		
		return true;
	}
	
	public static boolean updateSectorChecksumsM2F1(byte[] rawSector) {
		if(rawSector == null) return false;
		if(rawSector.length < ISO.SECSIZE) return false;
		
		int edc = calculateEDC(rawSector, 0x10, EDC_OFFSET_M2F1 - 0x10);
		for(int i = 0; i < 4; i++) {
			rawSector[EDC_OFFSET_M2F1 + i] = (byte)(edc & 0xff);
			edc >>>= 8;
		}
		
		//ECC1
		byte[] temp = new byte[4];
		for(int i = 0; i < 4; i++) {
			temp[i] = rawSector[i+0xc];
			rawSector[i+0xc] = 0;
		}
		calculateParityP(rawSector);
		
		//ECC2
		calculateParityQ(rawSector);
		for(int i = 0; i < 4; i++) {
			rawSector[i+0xc] = temp[i];
		}
		
		return true;
	}
	
	public static boolean updateSectorChecksumsM2F1(FileBuffer rawSector) {
		if(rawSector == null) return false;
		if(rawSector.readOnly()) return false;
		if(rawSector.getFileSize() < ISO.SECSIZE) return false;
		
		//Zero out checksum area
		for(long i = EDC_OFFSET_M2F1; i < ISO.SECSIZE; i++) rawSector.replaceByte((byte)0, i);
		FileBuffer region = rawSector.createReadOnlyCopy(0x10, EDC_OFFSET_M2F1);
		int edc = calculateEDC(region);
		try {region.dispose();} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		rawSector.setEndian(false);
		rawSector.replaceInt(edc, EDC_OFFSET_M2F1);
		
		//ECC1
		int sech = rawSector.intFromFile(0xcL);
		rawSector.replaceInt(0, 0xcL);
		
		region = rawSector.createReadOnlyCopy(ECC_AREA_START, ECCP_OFFSET);
		byte[] p = calculateParityP(region);
		try {region.dispose();} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		for(int i = 0; i < ECCP_SIZE; i++) rawSector.replaceByte(p[i], ECCP_OFFSET + i);
		
		//ECC2
		region = rawSector.createReadOnlyCopy(ECC_AREA_START, ECCQ_OFFSET);
		byte[] q = calculateParityQ(region);
		try {region.dispose();} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		for(int i = 0; i < q.length; i++) rawSector.replaceByte(q[i], ECCQ_OFFSET + i);
		
		rawSector.replaceInt(sech, 0xcL);
		
		return true;
	}
	
	public static boolean updateSectorChecksumsM2F2(byte[] rawSector) {
		if(rawSector == null) return false;
		if(rawSector.length < ISO.SECSIZE) return false;
		
		int edc = calculateEDC(rawSector, 0x10, EDC_OFFSET_M2F2 - 0x10);
		for(int i = 0; i < 4; i++) {
			rawSector[EDC_OFFSET_M2F2 + i] = (byte)(edc & 0xff);
			edc >>>= 8;
		}
		
		return true;
	}
	
	public static boolean updateSectorChecksumsM2F2(FileBuffer rawSector) {
		if(rawSector == null) return false;
		if(rawSector.readOnly()) return false;
		if(rawSector.getFileSize() < ISO.SECSIZE) return false;
		
		//Zero out checksum area
		for(long i = EDC_OFFSET_M2F2; i < ISO.SECSIZE; i++) rawSector.replaceByte((byte)0, i);
		FileBuffer region = rawSector.createReadOnlyCopy(0x10, EDC_OFFSET_M2F2);
		int edc = calculateEDC(region);
		try {region.dispose();} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		rawSector.setEndian(false);
		rawSector.replaceInt(edc, EDC_OFFSET_M2F2);
		
		//Form 2 has no ECC.
		return true;
	}
	
}
