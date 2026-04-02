package waffleoRai_Utils;

/*
 * UPDATES
 *  1.0.1 | Sep 8, 2017
 *  
 *  1.0.1 -> 2.0.0 | Apr 2, 2026
 *  	Changed to ABC to expand compatibility for wrapping input streams
 */

/**
 * A sleaker way to stream bits from a byte buffer (FileBuffer object).
 * Also includes static methods for individual bit reading and writing.
 * @author Blythe Hospelhorn
 * @version 2.0.0
 * @since April 2, 2026
 */
public abstract class BitStreamer {

	protected int biPos;
	protected byte tempByteR;
	protected byte tempByteW;
	protected boolean readMode;
	
	protected BitStreamer(boolean readOnly) {
		this.biPos = 7;
		this.tempByteR = 0;
		this.tempByteW = 0;
		this.readMode = readOnly;
	}
	
	/*----- Getters -----*/
	
	public int getBitPosition(){return this.biPos;}
	public boolean inReadOnlyMode(){return this.readMode;}
	
	/*----- Setters -----*/
	
	/*----- Movers -----*/
	
	public void fastForward(int bits) {
		int by = bits >> 3;
		int bi = bits & 0x7;
		this.fastForward(by,  bi);
	}
	
	public abstract void fastForward(int bytes, int bits);
	
	/*----- Readers -----*/
	
	public static boolean readABit(byte b, int bitPos) {
		if (bitPos < 0 || bitPos >= 8) return false;
		return readABit((Byte.toUnsignedInt(b)), bitPos);
	}
	
	public static boolean readABit(short s, int bitPos) {
		if (bitPos < 0 || bitPos >= 16) return false;
		return readABit((Short.toUnsignedInt(s)), bitPos);
	}
	
	public static boolean readABit(int i, int bitPos) {
		if (bitPos < 0 || bitPos >= 32) return false;
		
		int mask = 1 << bitPos;
		int result = i & mask;
		if (result == 0) return false;
		else return true;
	}
	
	public static boolean readABit(long l, int bitPos) {
		if (bitPos < 0 || bitPos >= 64) return false;
		
		long mask = 1L << bitPos;
		long result = l & mask;
		if (result == 0) return false;
		else return true;
	}
	
	public abstract boolean readNextBit();
	public abstract byte readToByte(int bits);
	public abstract short readToShort(int bits);
	public abstract int readToInt(int bits);
	public abstract long readToLong(int bits);
	
	/*----- Writers -----*/
	
	protected abstract void writeTempByte();
	
	public static byte writeABit(byte target, boolean bit, int bitPos) {
		int ti = writeABit(Byte.toUnsignedInt(target), bit, bitPos);
		return (byte)ti;
	}
	
	public static short writeABit(short target, boolean bit, int bitPos) {
		int ti = writeABit(Short.toUnsignedInt(target), bit, bitPos);
		return (short)ti;
	}
	
	public static int writeABit(int target, boolean bit, int bitPos) {
		int i = target;
		int mask = 1 << bitPos;
		if (bit) i = target | mask;
		else i = target & (~mask);
		return i;
	}
	
	public static long writeABit(long target, boolean bit, int bitPos) {
		long l = target;
		long mask = 1L << bitPos;
		if (bit) l = target | mask;
		else l = target & (~mask);
		return l;
	}
	
	public void writeIncompleteTemp() {
		if (readMode) return;
		if (biPos == 7) return;
		else writeTempByte();
	}
	
	public boolean writeBits(boolean bit) {
		if (readMode) return false;
		
		writeABit(tempByteW, bit, biPos);
		biPos--;
		if (biPos < 0) {
			writeTempByte();
			biPos = 7;
		}
		
		return true;
	}
	
	public boolean writeBits(byte val, int bits) {
		if (readMode) return false;
		if (bits < 0 || bits > 8) return false;
		
		for (int i = bits - 1; i >= 0; i--) {
			boolean myBit = readABit(val, i);
			writeBits(myBit);
		}
		
		return true;
	}
	
	public boolean writeBits(short val, int bits) {
		if (readMode) return false;
		if (bits < 0 || bits > 16) return false;
		
		for (int i = bits - 1; i >= 0; i--) {
			boolean myBit = readABit(val, i);
			writeBits(myBit);
		}
		
		return true;
	}
	
	public boolean writeBits(int val, int bits) {
		if (readMode) return false;
		if (bits < 0 || bits > 32) return false;
		
		for (int i = bits - 1; i >= 0; i--) {
			boolean myBit = readABit(val, i);
			writeBits(myBit);
		}
		
		return true;
	}
	
	public boolean writeBits(long val, int bits) {
		if (readMode) return false;
		if (bits < 0 || bits > 64) return false;
		
		for (int i = bits - 1; i >= 0; i--) {
			boolean myBit = readABit(val, i);
			writeBits(myBit);
		}
		
		return true;
	}
	
}
