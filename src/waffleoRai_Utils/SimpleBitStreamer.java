package waffleoRai_Utils;

public class SimpleBitStreamer extends BitStreamer{
	
	private StreamWrapper src;
	
	private boolean tempDirty = true;
	private boolean eof = false;
	
	public SimpleBitStreamer(StreamWrapper source) {
		super(source.isReadOnly());
		src = source;
		fetchNextByte();
	}

	/*----- Getters -----*/
	
	public boolean hasRemaining() {return !eof;}
	
	/*----- Movers -----*/
	
	public void fastForward(int bytes, int bits) {
		while(bits >= 8) {
			bytes++;
			bits -= 8;
		}
		
		src.skip(bytes);
		biPos -= bits;
		while (biPos < 0){
			biPos += 8;
		}
		tempDirty = true;
	}
	
	/*----- Readers -----*/
	
	private void fetchNextByte() {
		if(eof) return;
		int b = src.getFull();
		if(b != -1) {
			tempByteR = (byte)b;
			biPos = 7;
			tempDirty = false;
		}
		else eof = true;
	}
	
	public boolean readNextBit() {
		if(eof) return false;
		if(tempDirty || (biPos < 0)) {
			fetchNextByte();
		}
		
		return readABit(tempByteR, biPos--);
	}
	
	public byte readToByte(int bits) {
		if(eof) return 0;
		if(tempDirty || (biPos < 0)) {
			fetchNextByte();
		}
		if((bits == 8) && (biPos == 7)) {
			biPos = -1;
			return tempByteR;
		}
		return (byte)readToInt(bits);
	}
	
	public short readToShort(int bits) {
		if(eof) return 0;
		return (short)readToInt(bits);
	}
	
	public int readToInt(int bits) {
		if(eof) return 0;
		
		int bitsRem = bits;
		if(tempDirty || (biPos < 0)) {
			fetchNextByte();
		}
		
		//Start partial
		int val = 0;
		int b = Byte.toUnsignedInt(tempByteR);
		if((biPos < 7) || (bitsRem < 8)) {
			int takeBits = biPos + 1;
			if(bitsRem < takeBits) takeBits = bitsRem;
			int mask = ~(~0 << takeBits);
			b >>>= (biPos + 1) - takeBits;
			val |= (b & mask);
			bitsRem -= takeBits;
			biPos -= takeBits;
			if(biPos < 0) fetchNextByte();
		}
		
		//Middle bytes
		while(bitsRem >= 8) {
			val <<= 8;
			val |= Byte.toUnsignedInt(tempByteR);
			fetchNextByte();
			bitsRem -= 8;
		}
		
		//End partial
		if(bitsRem > 0) {
			b = Byte.toUnsignedInt(tempByteR);
			int mask = ~(~0 << bitsRem);
			b >>>= 8 - bitsRem;
			val <<= bitsRem;
			val |= (b & mask);
			biPos -= bitsRem;
			if(biPos < 0) fetchNextByte();
		}
		
		return val;
	}
	
	public long readToLong(int bits) {
		if(eof) return 0;
		
		int bitsRem = bits;
		if(tempDirty || (biPos < 0)) {
			fetchNextByte();
		}
		
		//Start partial
		long val = 0;
		int b = Byte.toUnsignedInt(tempByteR);
		if((biPos < 7) || (bitsRem < 8)) {
			int takeBits = biPos + 1;
			if(bitsRem < takeBits) takeBits = bitsRem;
			int mask = ~(~0 << takeBits);
			b >>>= (biPos + 1) - takeBits;
			val |= (b & mask);
			bitsRem -= takeBits;
			biPos -= takeBits;
			if(biPos < 0) fetchNextByte();
		}
		
		//Middle bytes
		while(bitsRem >= 8) {
			val <<= 8;
			val |= Byte.toUnsignedInt(tempByteR);
			fetchNextByte();
			bitsRem -= 8;
		}
		
		//End partial
		if(bitsRem > 0) {
			b = Byte.toUnsignedInt(tempByteR);
			int mask = ~(~0 << bitsRem);
			b >>>= 8 - bitsRem;
			val <<= bitsRem;
			val |= (b & mask);
			biPos -= bitsRem;
			if(biPos < 0) fetchNextByte();
		}
		
		return val;
	}
	
	/*----- Writers -----*/
	
	protected void writeTempByte() {
		src.put(tempByteW);
		tempByteW = 0;
	}
	
}
