package waffleoRai_Files;

import waffleoRai_Encryption.DecryptorMethod;

public class DiskDataFilter implements DecryptorMethod{
	
	private int sz_head;
	private int sz_data;
	private int sz_foot;
	
	public DiskDataFilter(int bytes_head, int bytes_data, int bytes_foot){
		sz_head = bytes_head;
		sz_data = bytes_data;
		sz_foot = bytes_foot;
	}
	
	public int getTotalSectorSize(){
		return sz_head + sz_data + sz_foot;
	}

	public byte[] decrypt(byte[] input, long offval) {
		//Strips header and footer and returns data
		if(input == null) return null;
		
		//Get sector count.
		int insize = getTotalSectorSize();
		int secs = input.length/insize;
		if(secs == 0) return null;
		
		int out_total = secs * sz_data;
		byte[] out = new byte[out_total];
		int ipos = 0;
		int opos = 0;
		for(int s = 0; s < secs; s++){
			ipos += sz_head;
			for(int i = 0; i < sz_data; i++){
				out[opos++] = input[ipos++];
			}
			ipos += sz_foot;
		}
		
		return out;
	}

	public void adjustOffsetBy(long value) {}
	public int getInputBlockSize() {return getTotalSectorSize();}
	public int getOutputBlockSize() {return sz_data;}
	public int getPreferredBufferSizeBlocks() {return 1;}

	public long getOutputBlockOffset(long inputBlockOffset) {
		if(inputBlockOffset <= sz_head) return 0;
		long ooff = inputBlockOffset - sz_head;
		if(ooff >= sz_foot) return (long)sz_foot;
		
		return ooff;
	}

	public long getInputBlockOffset(long outputBlockOffset) {
		return outputBlockOffset + sz_head;
	}

	public long getOutputCoordinate(long inputCoord) {
		int ssz = getTotalSectorSize();
		long sec = inputCoord/ssz;
		long off = inputCoord%ssz;
		long ooff = getOutputBlockOffset(off);
		
		long outc = (sec*sz_data) + ooff;
		
		return outc;
	}

	public long getInputCoordinate(long outputCoord) {
		int ssz = getTotalSectorSize();
		long sec = outputCoord/sz_data;
		long off = outputCoord%sz_data;
		long ioff = getInputBlockOffset(off);
		
		return (sec * ssz) + ioff;
	}

	public int backbyteCount() {return 0;}
	public void putBackbytes(byte[] dat) {}

	public DecryptorMethod createCopy() {
		return new DiskDataFilter(sz_head, sz_data, sz_foot);
	}

}
