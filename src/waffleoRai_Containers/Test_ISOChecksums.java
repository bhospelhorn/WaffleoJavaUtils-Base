package waffleoRai_Containers;

import waffleoRai_Utils.FileBuffer;

public class Test_ISOChecksums {

	private static void printByteArray(byte[] array, int perRow) {
		for(int i = 0; i < array.length; i++) {
			if((i % perRow) == 0) {
				if(i != 0) System.err.print("\n");
				System.err.print("\t");
			}
			System.err.print(String.format("%02x ", array[i]));
		}
		System.err.print("\n");
	}
	
	private static void printByteArray(int[] array, int perRow) {
		for(int i = 0; i < array.length; i++) {
			if((i % perRow) == 0) {
				if(i != 0) System.err.print("\n");
				System.err.print("\t");
			}
			System.err.print(String.format("%02x ", array[i]));
		}
		System.err.print("\n");
	}
	
	public static void main(String[] args) {
		String inpath = args[0];
		int insec = Integer.parseInt(args[1]);
		
		try {
			long st = (long)insec * ISO.SECSIZE;
			long ed = st + ISO.SECSIZE;
			
			FileBuffer secRaw = FileBuffer.createBuffer(inpath, st, ed, true);
			int mode = Byte.toUnsignedInt(secRaw.getByte(0xf));
			System.err.println("Detected Mode: " + mode);
			FileBuffer secData = null;
			int edc = 0;
			byte[] p = null;
			byte[] q = null;
			
			switch(mode) {
			case 0:
			case 1:
				secData = secRaw.createReadOnlyCopy(0, ISO.F1SIZE + 0x10);
				edc = ISOUtils.calculateEDC(secData);
				System.err.println("Calculated EDC: 0x" + Integer.toHexString(edc));
				secData.dispose();
				secData = secRaw.createReadOnlyCopy(0xc, 0x81c);
				p = ISOUtils.calculateParityP(secData);
				System.err.println("----- ECC P -----");
				printByteArray(p, 16);
				secData.dispose();
				
				secData = secRaw.createReadOnlyCopy(0xc, ISO.F1SIZE + 0x1c + (43 << 2));
				q = ISOUtils.calculateParityQ(secData);
				System.err.println("----- ECC Q -----");
				printByteArray(q, 16);
				secData.dispose();
				break;
			case 2:
				int sh3 = Byte.toUnsignedInt(secRaw.getByte(0x13));
				boolean form2 = false;
				if((sh3 & 0x20) != 0) form2 = true;
				if(form2) {
					System.err.println("Form 2");
					secData = secRaw.createReadOnlyCopy(0x10, ISO.F2SIZE + 0x18);
					edc = ISOUtils.calculateEDC(secData);
					System.err.println("Calculated EDC: 0x" + Integer.toHexString(edc));
					secData.dispose();
				}
				else {
					System.err.println("Form 1");
					secData = secRaw.createReadOnlyCopy(0x10, ISO.F1SIZE + 0x18);
					edc = ISOUtils.calculateEDC(secData);
					System.err.println("Calculated EDC: 0x" + Integer.toHexString(edc));
					secData.dispose();
					FileBuffer secCopy = secRaw.createCopy(0xc, ISO.F1SIZE + 0x1c);
					secCopy.replaceInt(0, 0L);
					p = ISOUtils.calculateParityP(secCopy);
					System.err.println("----- ECC P -----");
					printByteArray(p, 16);
					secCopy.dispose();
					
					secCopy = secRaw.createCopy(0xc, ISO.F1SIZE + 0x1c + (43 << 2));
					secCopy.replaceInt(0, 0L);
					q = ISOUtils.calculateParityQ(secCopy);
					System.err.println("----- ECC Q -----");
					printByteArray(q, 16);
					secCopy.dispose();
					
					/*System.err.println("----- DEBUG: LOG Table -----");
					printByteArray(ISOUtils.gf8LogTable, 16);
					
					System.err.println("----- DEBUG: ILOG Table -----");
					printByteArray(ISOUtils.gf8ILogTable, 16);*/
				}
				break;
			}
			
		}
		catch(Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		
		
	}

}
