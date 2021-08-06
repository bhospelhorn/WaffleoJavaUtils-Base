package waffleoRai_Sound;

public interface ADPCMCoder {

	//public ADPCMTable getTable();
	public int getSamplesPerBlock();
	public int getBytesPerBlock(); //Including control bytes
	public boolean getHiNybbleFirst();
	
	public void reset();
	public void setToLoop(int loopIndex);
	public void setControlByte(int val);
	public boolean newBlock();
	public int decompressNextNybble(int raw_sample);
	
}
