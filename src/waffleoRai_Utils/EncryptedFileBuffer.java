package waffleoRai_Utils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import waffleoRai_Encryption.DecryptorMethod;

public class EncryptedFileBuffer extends FileBuffer{
	
	/* --- Constants --- */
	
	public static final int DEFO_BLOCKSIZE_SHAMT = 10; //1kb
	
	/* --- Instance Variables --- */
	
	private FileBuffer src;
	private DecryptorMethod decm;
	private int block_shamt;
	
	private long buff_off; //Offset relative to source of beginning of current buffer
	private long buff_ed;
	private byte[] buffer;
	
	/* --- Construction --- */
	
	public EncryptedFileBuffer(FileBuffer source, DecryptorMethod decryptor){
		this(source, decryptor, DEFO_BLOCKSIZE_SHAMT);
	}

	public EncryptedFileBuffer(FileBuffer source, DecryptorMethod decryptor, int blocksz_shift){
		super();
		super.setReadOnly();
		
		src = source;
		decm = decryptor;
		block_shamt = blocksz_shift;
		
		buff_off = -1;
		buff_ed = -1;
	}
	
	/* --- Getters --- */
	
	public long getFileSize(){return src.getFileSize();}
	
	public byte getByte(int position){return getByte(Integer.toUnsignedLong(position));}
	
	public byte getByte(long position){
		if(!offset_buffered(position)) bufferBlock(position);
		return buffer[(int)(position - buff_off)];
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
		int blocksize = 1 << block_shamt;
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
		//Snap to block start...
		long b_start = offset & (~(0L) << block_shamt);
		long b_end = b_start + (1L << block_shamt);
		
		//Check source size to adjust end point
		long fsize = src.getFileSize();
		if(b_end > fsize) b_end = fsize;
		
		//Load
		//System.err.println("EncryptedFileBuffer.bufferBlock || Block @ 0x" + Long.toHexString(b_start) + " - 0x" + Long.toHexString(b_end));
		byte[] rawdat = src.getBytes(b_start, b_end);
		buffer = decm.decrypt(rawdat, b_start);
		buff_off = b_start;
		buff_ed = b_end;
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
		
		int blocksize = 1 << block_shamt;
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
	
	/* --- Copying --- */
	
	public FileBuffer createCopy(int stPos, int edPos) throws IOException{
		return createCopy(Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
	}
	
	public FileBuffer createCopy(long stPos, long edPos) throws IOException{
		EncryptedFileBuffer copy = new EncryptedFileBuffer(src.createCopy(stPos, edPos), decm, block_shamt);
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
