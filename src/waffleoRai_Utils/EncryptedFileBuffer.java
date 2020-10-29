package waffleoRai_Utils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import waffleoRai_Encryption.DecryptorMethod;

//TODO update sizes (decryptor input and output block sizes may not be the same!)
//TODO also add start offset field since start of desired data might be mid-block

public class EncryptedFileBuffer extends FileBuffer{
	
	/* --- Constants --- */
	
	public static final int DEFO_BLOCKSIZE_SHAMT = 10; //1kb
	
	/* --- Instance Variables --- */
	
	private FileBuffer src;
	private DecryptorMethod decm;
	//private int block_shamt;
	
	//private long start;
	//private int start_block_in; //Precalculated start info
	//private long start_bpos_in;
	//private long start_block_size_out;
	private int block_size_in;
	private int block_size_out;
	private int blocks_per_buffer;
	
	private long buff_off; //Offset relative to source of beginning of current buffer
	private long buff_ed;
	private byte[] buffer;
	
	/* --- Construction --- */
	
	public EncryptedFileBuffer(FileBuffer source, DecryptorMethod decryptor){
		//this(source, decryptor, DEFO_BLOCKSIZE_SHAMT);
		//this(source, decryptor, 0L);
		super();
		super.setReadOnly();
		super.setEndian(source.isBigEndian());
		
		src = source;
		decm = decryptor;
		//block_shamt = blocksz_shift;
		
		buff_off = -1;
		buff_ed = -1;
		
		updatePrecalculatedStartValues();
	}
	
	/*public EncryptedFileBuffer(FileBuffer source, DecryptorMethod decryptor, int blocksz_shift){
		super();
		super.setReadOnly();
		super.setEndian(source.isBigEndian());
		
		src = source;
		decm = decryptor;
		block_shamt = blocksz_shift;
		
		buff_off = -1;
		buff_ed = -1;
	}*/
	
	/* --- Getters --- */
	
	public long getFileSize(){
		if(block_size_in == block_size_out) return src.getFileSize();
		
		long insize = src.getFileSize();
		long in_end_b = insize/block_size_in;
		long in_end_pos = insize%block_size_in;
		
		if(in_end_pos == 0){
			return in_end_b * block_size_out;
		}
		else{
			in_end_b--;
			long opos = decm.getOutputBlockOffset(in_end_pos);
			return (in_end_b * block_size_out) + opos;
		}
		
	}
	
	public byte getByte(int position){return getByte(Integer.toUnsignedLong(position));}
	
	public byte getByte(long position){
		if(!offset_buffered(position)) bufferBlock(position);
		try{return buffer[(int)(position - buff_off)];}
		catch(ArrayIndexOutOfBoundsException x){
			System.err.println("Buffer error | Annotated buffer range: 0x" + Long.toHexString(buff_off) + " - 0x" + Long.toHexString(buff_ed));
			System.err.println("Buffer size: 0x" + Integer.toHexString(buffer.length));
			throw new IndexOutOfBoundsException("Position out of bounds for buffer: 0x" + Long.toHexString(position));
		}
	}
	
	public byte[] getBytes(){
		return getBytes(0L, getFileSize());
	}
	
	public byte[] getBytes(int stOff, int edOff){
		return getBytes(Integer.toUnsignedLong(stOff), Integer.toUnsignedLong(edOff));
	}
	
	public byte[] getBytes(long stOff, long edOff){
		if(stOff < 0) throw new IndexOutOfBoundsException("Start offset cannot be less than 0!");
		if(edOff > getFileSize()) throw new IndexOutOfBoundsException("End offset cannot exceed buffer size!");
		if(stOff >= edOff) throw new IndexOutOfBoundsException("Start offset must be smaller than end offset!");
		
		//Check size
		long llen = edOff - stOff;
		if(llen > 0x7FFFFFFF) throw new IndexOutOfBoundsException("Byte array size cannot exceed 2GB");
		byte[] out = new byte[(int)llen];
		
		//This method only doesn't call getByte() in order to reduce the number of
		// range checks. It should be ever so slightly faster.
		//int blocksize = 1 << block_shamt;
		int blocksize = block_size_out * blocks_per_buffer;
		if(!offset_buffered(stOff)) bufferBlock(stOff); //Load start block
		int pos = (int)(stOff % blocksize);
		for(int i = 0; i < out.length; i++){
			if(pos >= buffer.length){
				bufferBlock(stOff + i);
				pos = 0;
			}
			out[i] = buffer[pos++];
		}
		
		return out;
	}
	
	public ByteBuffer toByteBuffer(){
		return toByteBuffer(0L, getFileSize());
	}
	
	public ByteBuffer toByteBuffer(int stPos, int edPos){
		return toByteBuffer(Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
	}
	
	public ByteBuffer toByteBuffer(long stPos, long edPos){
		byte[] bytes = getBytes(stPos, edPos);
		ByteBuffer bb = ByteBuffer.allocate(bytes.length);
		bb.put(bytes);
		bb.rewind();
		return bb;
	}
	
	/* --- Setters --- */
	
	public void unsetReadOnly(){
		throw new UnsupportedOperationException();
	}
	
	/* --- Decryption --- */
	
	private boolean offset_buffered(long offset){
		return (buffer != null && 
				offset >= buff_off && 
				offset < buff_ed);
	}
	
	private void bufferBlock(long offset){
		int obsz = decm.getOutputBlockSize();
		//long pos = offset - start_block_size_out; //Block align location
		
		long b = offset/obsz;
		//b += start_block_in;
		//if(start_block_size_out != 0) b++; //Count partial
		
		//Now calculate the buffer block
		int bblock = (int)(b/blocks_per_buffer);
		bufferBlock(bblock);
	}
	
	private void bufferBlock(int buff_block){
		
		//Get offset
		//Determine how many bytes are needed based on buffer size...
		//int ibsz = decm.getInputBlockSize();
		//int obsz = decm.getOutputBlockSize();
		//bsz = decm.getPreferredBufferSizeBlocks();
		int getsize = block_size_in * blocks_per_buffer;
		long b_start = (long)buff_block*(long)getsize;
		long b_end = b_start + (long)getsize;
		
		//Snap to block start...
		//long b_start = offset & (~(0L) << block_shamt);
		//long b_end = b_start + (1L << block_shamt);
		
		//Check source size to adjust end point
		long fsize = src.getFileSize();
		if(b_end > fsize) b_end = fsize;
		
		//Check for preloaded bytes (in cases of CBC for example, need row before)
		int bbcount = decm.backbyteCount();
		if(bbcount > 0){
			long bbstart = b_start - bbcount;
			if(bbstart >= 0){
				decm.putBackbytes(src.getBytes(bbstart, b_start));
			}
		}
		
		//Load
		//System.err.println("EncryptedFileBuffer.bufferBlock || Block @ 0x" + Long.toHexString(b_start) + " - 0x" + Long.toHexString(b_end));
		byte[] rawdat = src.getBytes(b_start, b_end);
		buffer = decm.decrypt(rawdat, b_start);
		
		buff_off = (((long)buff_block * (long)blocks_per_buffer) * block_size_out);
		buff_ed = buff_off + (block_size_out * blocks_per_buffer);
		fsize = this.getFileSize();
		if(buff_ed > fsize) buff_ed = fsize;
	}
	
	/* --- Writing --- */
	
	public void writeFile(String path, long stPos, long edPos) throws IOException{
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		writeToStream(bos, stPos, edPos);
		bos.close();
	}
	
	public long writeToStream(OutputStream out, long stPos, long edPos) throws IOException{

		if(stPos < 0) throw new IndexOutOfBoundsException("Start offset cannot be less than 0!");
		if(edPos > getFileSize()) throw new IndexOutOfBoundsException("End offset cannot exceed buffer size!");
		if(stPos >= edPos) throw new IndexOutOfBoundsException("Start offset must be smaller than end offset!");
		
		int blocksize = block_size_out * blocks_per_buffer;
		if(!offset_buffered(stPos)) bufferBlock(stPos); //Load start block
		int pos = (int)(stPos % blocksize);
		long fpos = stPos;
		while(fpos < edPos){
			int b_len = buffer.length - pos;
			long b_end = fpos + b_len;
			if(b_end > edPos) b_len -= (b_end - edPos);
			out.write(buffer, pos, b_len);
			
			fpos+=b_len;
			if(fpos < edPos) bufferBlock(fpos);
			pos = 0;
		}
		
		return fpos - stPos;
	}
	
	/* --- Location Conversion --- */
	
	private void updatePrecalculatedStartValues(){
		
		if(decm != null){
			block_size_in = decm.getInputBlockSize();
			block_size_out = decm.getOutputBlockSize();
			blocks_per_buffer = decm.getPreferredBufferSizeBlocks();
		}
		else{
			block_size_in = 1;
			block_size_out = 1;
			blocks_per_buffer = 0x400;
		}
		

		/*start_block_in = (int)(start/ibsz);
		start_bpos_in = start % ibsz;
		long opos = decm.getOutputBlockOffset(start_bpos_in);
		if(opos != 0) start_block_size_out = (long)obsz - opos;
		else start_block_size_out = 0L;*/ //Technically it is a full block, but we use this as a marker to show start is block aligned.
		
	}
	
	/* --- Copying --- */
	
	public FileBuffer createCopy(int stPos, int edPos) throws IOException{
		return createCopy(Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
	}
	
	public FileBuffer createCopy(long stPos, long edPos) throws IOException{
		//EncryptedFileBuffer copy = new EncryptedFileBuffer(src.createCopy(stPos, edPos), decm, block_shamt);
		EncryptedFileBuffer copy = new EncryptedFileBuffer(src.createCopy(stPos, edPos), decm);
		return copy;
	}
	
	/* --- Cleanup --- */
	
	public void dispose() throws IOException{
		buffer = null;
		buff_off = -1; buff_ed = -1;
		src.dispose();
	}
	
	public void flush() throws IOException {
		buffer = null;
		buff_off = -1; buff_ed = -1;
		src.flush();
	}
	
}
