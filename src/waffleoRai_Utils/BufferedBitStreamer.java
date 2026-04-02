package waffleoRai_Utils;

public class BufferedBitStreamer extends BitStreamer{
	
	private FileBuffer myFile;
	private long byPos;
	
	public BufferedBitStreamer(FileBuffer f, boolean readOnly) {
		this(f, 0, readOnly);
	}
	
	public BufferedBitStreamer(FileBuffer f, long stPos, boolean readOnly) {
		super(readOnly);
		this.myFile = f;
		
		if (stPos < 0) stPos = 0;
		if (stPos >= f.getFileSize()) stPos = f.getFileSize() - 1;
		
		this.byPos = stPos;
	}
	
	/*----- Getters -----*/
	
	public FileBuffer getFile(){return myFile;}
	public long getBytePosition() {return byPos;}
	
	/*----- Setters -----*/
	
	public void setFile(FileBuffer f){this.setFile(f, 0);}
	
	public void setFile(FileBuffer f, long stPos){
		this.myFile = f;
		
		if (stPos < 0) stPos = 0;
		if (stPos >= f.getFileSize()) stPos = f.getFileSize() - 1;
		this.byPos = stPos;
		this.biPos = 7;
	}
	
	/*----- Movers -----*/
	
	public void rewind(){
		this.byPos = 0;
		this.biPos = 7;
	}
	
	public void rewind(int bits){
		int by = bits / 8;
		int bi = bits % 8;
		this.rewind(by, bi);
	}
	
	public void rewind(int bytes, int bits){
		this.byPos -= bytes;
		if (this.byPos < 0) this.byPos = 0;
		
		this.biPos += bits;
		if (this.biPos >= 8){
			this.biPos = this.biPos % 8;
		}
	}
	
	public void fastForward() {
		byPos = myFile.getFileSize() - 1;
		biPos = 0;
	}
	
	public void fastForward(int bytes, int bits) {
		byPos += bytes;
		if (byPos >= myFile.getFileSize()) byPos = myFile.getFileSize();
		
		biPos -= bits;
		while (biPos < 0){
			biPos += 8;
		}
	}
	
	public boolean canMoveForward(int bits) {		
		int by = bits/8;
		int bi = bits%8;
		if (this.byPos + by >= this.myFile.getFileSize()) return false;
		if (this.byPos + by == this.myFile.getFileSize() - 1 
				&& this.biPos - bi < 0) return false;
		return true;
	}
	
	public boolean canMoveBackward(int bits) {
		int by = bits/8;
		int bi = bits%8;
		if (this.byPos - by < 0) return false;
		if (this.byPos - by == 0 && this.biPos + bi >= 8) return false;
		return true;
	}
	
	/*----- Readers -----*/
	
	public boolean readNextBit(){
		byte b = this.myFile.getByte(byPos);
		boolean bit = readABit(b, biPos);
		fastForward(1);
		return bit;
	}
	
	public byte readToByte(int bits){
		byte myByte = 0;
		myByte = this.myFile.getBits8(bits, byPos, biPos);
		fastForward(bits);
		return myByte;
	}
	
	public short readToShort(int bits){
		short myShort = myFile.getBits16(bits, byPos, biPos + 8);
		fastForward(bits);
		return myShort;
	}
	
	public int readToInt(int bits){
		int myInt = myFile.getBits32(bits, byPos, biPos + 24);
		fastForward(bits);
		return myInt;
	}
	
	public long readToLong(int bits){
		long myLong = myFile.getBits64(bits, byPos, biPos + 56);
		fastForward(bits);
		return myLong;
	}
	
	/*----- Writers -----*/
	
	protected void writeTempByte(){
		myFile.addToFile(tempByteW);
		tempByteW = 0;
		byPos++;
	}
	
}
