package waffleoRai_Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.NoSuchFileException;
import java.util.Collection;

//OVERRIDE isEmpty

/*UPDATES
 * 2017.08.31
 * 	2.0 -> 2.0.1 | Added getMemoryBurden()
 * 2017.09.01
 * 	2.0.1 -> 3.0 | Altered implementation to reference parent instead of use a super class.
 * 2017.09.08
 * 	3.0 -> 3.0.1 | Updated to reflect changes in parent class 
 * 		(deletion of certain methods, internal method calling, reference implementation...)
 * 2017.09.20
 * 	3.0.1 -> 3.0.2 | Javadoc updates, change to IndexOutOfBoundsException thrown
 * 2018.01.22
 * 	3.0.2 -> 3.0.3 | Added more technical memory calculation method (getMinimumMemoryUsage())
 * 2019.07.11
 * 	3.0.3 -> 3.0.4 | On construction, copies byte-order flag from source FileBuffer
 * 2020.02.14
 * 	3.0.4 -> 3.1.0 | Added writeToStream()
 * 2020.11.03
 * 	3.1.0 -> 3.1.1 | Updated createCopy() to handle block-aligned sources properly!
 * 2021.01.28
 * 	3.1.1 -> 3.1.2 | Debugging and error reporting offset rejections
 * 
 * */

/**
 * For creating a new file buffer from a piece of an existing one, so
 * that copying to a new array is not required.
 * Because this is basically just a wrapper for a reference to an existing buffer,
 * all write functions will throw an exception when called.
 * @author Blythe Hospelhorn
 * @version 3.1.2
 * @since January 28, 2021
 */
public class ROSubFileBuffer extends FileBuffer{

	private long posZero;
	private long posEnd;
	
	private int invalid_off_error;
	
	private FileBuffer parent;
	
	protected ROSubFileBuffer(FileBuffer file, int stPos, int edPos)
	{
		super();
		super.setEndian(file.isBigEndian());
		this.parent = file;
		if (edPos > (int)parent.getFileSize()) edPos = (int)parent.getFileSize();
		if (stPos < 0) stPos = 0;
		this.posZero = (long)stPos;
		this.posEnd = (long)edPos;
		super.setReadOnly();
	}
	
	protected ROSubFileBuffer(FileBuffer file, long stPos, long edPos)
	{
		super();
		super.setEndian(file.isBigEndian());
		this.parent = file;
		if (edPos > parent.getFileSize()) edPos = parent.getFileSize();
		if (stPos < 0) stPos = 0;
		this.posZero = stPos;
		this.posEnd = edPos;
		super.setReadOnly();
	}
	
	protected ROSubFileBuffer()
	{
		super();
		this.parent = null;
		this.posZero = -1;
		this.posEnd = -1;
	}
		
	private long getPosition(long locPos)
	{
		if (!this.offsetValid(locPos)){
			System.err.println("ROSubFileBuffer.getPosition || Invalid position found: 0x" + Long.toHexString(locPos));
			//System.err.println("ROSubFileBuffer.getPosition || Parent is null? " + (parent == null));
			switch(invalid_off_error){
			case 1: System.err.println("Reason: Parent reference is null"); break;
			case 2: System.err.println("Reason: File is empty"); break;
			case 3: System.err.println("Reason: Offset is less than 0x0"); break;
			case 4: System.err.println("Reason: File reference offset exceeds size of source"); break;
			case 5: System.err.println("Reason: Offset exceeds reference size 0x" + Long.toHexString(posEnd-posZero)); break;
			case 6: System.err.println("Reason: Offset exceeds source size 0x" + Long.toHexString(parent.getFileSize())); break;
			default: System.err.println("Reason: unknown"); break;
			}
			
			System.err.println("DEBUG Info --");
			System.err.println("Requested Offset: 0x" + Long.toHexString(locPos));
			System.err.println("posZero: 0x" + Long.toHexString(posZero));
			System.err.println("posEnd: 0x" + Long.toHexString(posEnd));
			System.err.println("Size: 0x" + Long.toHexString(this.getFileSize()));
			if(parent != null){
				System.err.println("Parent Class: " + parent.getClass());	
				System.err.println("Parent Size: 0x" + Long.toHexString(parent.getFileSize()));
			}
			
			throw new IndexOutOfBoundsException();
		}
		return locPos + posZero;
	}
	
	private long[] checkPositionPair(long stPos, long edPos)
	{
		long[] offs = new long[2];
		if (stPos >= edPos) throw new IndexOutOfBoundsException("Start offset 0x" + Long.toHexString(stPos) + 
				" exceeds end offset 0x" + Long.toHexString(edPos));
		if (stPos < 0) stPos = 0;
		if (edPos - stPos > this.getFileSize()) edPos = this.getFileSize() - stPos;
		stPos = getPosition(stPos);
		edPos = getPosition(edPos-1);
		edPos++;
		offs[0] = stPos;
		offs[1] = edPos;
		return offs;
	}
	
	/* --- Getter/ Setter Override --- */
	
 	  public long getFileSize()
	  {
		  return posEnd - posZero;
	  }
	  
 	  /**
 	   * @throws IndexOutOfBoundsException If position is invalid.
 	   */
	  public byte getByte(int position)
	  {
		if (!this.offsetValid(position))
		{
			System.err.println("ROSubFileBuffer.getByte || Invalid index: 0x" + Integer.toHexString(position));
			throw new IndexOutOfBoundsException();
		}
		return parent.getByte((int)posZero + position);
	  }
	  
		/**
		 * Get a byte from this file at the provided position.
		 * @param position Offset from FileBuffer start to get byte from.
		 * @return Byte at position specified
		 * @throws IndexOutOfBoundsException If position is invalid.
		 */
	  public byte getByte(long position)
	  {
		  if (!this.offsetValid(position))
			{
				System.err.println("ROSubFileBuffer.getByte || Invalid index: 0x" + Long.toHexString(position));
				throw new IndexOutOfBoundsException();
			}
		  return parent.getByte(posZero + position);
	  }
	  
	  public byte[] getBytes()
	  {
		  return parent.getBytes(posZero, posEnd);
	  }
	  
	  public byte[] getBytes(long stpos, long edpos)
	  {
		  long stAdj = stpos + posZero;
		  long edAdj = edpos + posZero;
		  return parent.getBytes(stAdj, edAdj);
	  }
	  
	/* --- Reading Override --- */
	  	/*--- Anything that relies on getByte apparently doesn't need to be overridden!*/
	  
	  /**
	   * @throws NullPointerException If charset string is null.
	   * @throws IndexOutOfBoundsException If any positions are invalid.
	   * @throws IllegalCharsetNameException If the given charset name is illegal.
	   * @throws UnsupporedCharsetException If no support for the named charset is available in this instance of the Java virtual machine.
	   */
	  public String readEncoded_string(String charset, long stPos, long edPos)
	  {
		  if (charset == null) throw new NullPointerException();
		  long[] myPos = checkPositionPair(stPos, edPos);
		  if (myPos == null || myPos.length != 2) throw new IndexOutOfBoundsException();
		  return parent.readEncoded_string(charset, myPos[0], myPos[1]);
	  }
	   
	/* --- Other Override --- */
	  
	  public boolean isEmpty()
	  {
		  if (parent == null) return true;
		  return false;
	  }
	  
	  public boolean readOnly()
	  {
		  return true;
	  }
	  
	  /**
	   * This sub-type of FileBuffer can only be read-only.
	   * An exception will be thrown if this function is called.
	   * @throws UnsupportedOperationException If this function is called.
	   */
	  public void unsetReadOnly()
	  {
		  throw new UnsupportedOperationException();
	  }
	  
	  public boolean offsetValid(int off)
	  {
		  return offsetValid((long)off);
	  }
	  
	  public boolean offsetValid(long off){
		  invalid_off_error = 0;
		  if (parent == null){invalid_off_error = 1; return false;}
		  if (this.getFileSize() <= 0) {invalid_off_error = 2; return false;}
		  if (off < 0) {invalid_off_error = 3; return false;}
		  if (posZero >= parent.getFileSize()) {invalid_off_error = 4; return false;}
		  if (posZero + off >= posEnd) {invalid_off_error = 5; return false;}
		  if (posZero + off >= parent.getFileSize()) {invalid_off_error = 6; return false;}
		  return true;
	  }
	  
	  public void writeFile() throws IOException
	  {
		  this.writeFile(this.getPath(), 0, this.getFileSize());
	  }
	  
	  public void writeFile(String path) throws IOException
	  { 
		  this.writeFile(path, 0, this.getFileSize());
	  }
	  
	  public void writeFile(int stPos) throws IOException
	  {
		  this.writeFile((long)stPos);
	  }
	  
	  public void writeFile(long stPos) throws IOException
	  {
		  this.writeFile(stPos, this.getFileSize());
	  }
	  
	  public void writeFile(int stPos, int edPos) throws IOException
	  {
		  this.writeFile((long)stPos, (long)edPos);
	  }
	  
	  public void writeFile(long stPos, long edPos) throws IOException
	  {
		  writeFile(this.getPath(), stPos, edPos);
	  }
	  
	  public void writeFile(String path, int stPos) throws IOException
	  {
		  this.writeFile(path, (long)stPos);
	  }
	  
	  public void writeFile(String path, int stPos, int edPos) throws IOException
	  {
		  this.writeFile(path, (long)stPos, (long)edPos);
	  }
	  
	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid.
	   */
	  public void writeFile(String path, long stPos, long edPos) throws IOException
	  {
		//  System.err.println("ROSubFileBuffer.writeFile || -DEBUG- Start: 0x" + Long.toHexString(stPos) + " | End: 0x" + Long.toHexString(edPos));
		  long[] myPos = checkPositionPair(stPos, edPos);
		  if (myPos == null || myPos.length != 2) throw new IndexOutOfBoundsException();
		  parent.writeFile(path, myPos[0], myPos[1]);
	  }

	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid.
	   */
	  public void appendToFile(String path, long stPos, long edPos) throws IOException, NoSuchFileException
	  {
		  long[] myPos = checkPositionPair(stPos, edPos);
		  if (myPos == null || myPos.length != 2) throw new IndexOutOfBoundsException();
		  parent.appendToFile(path, myPos[0], myPos[1]);
	  }
	  
	  public long writeToStream(OutputStream out) throws IOException
	  {
		  return parent.writeToStream(out, posZero, posEnd);
	  }
	  	
	  public long writeToStream(OutputStream out, long stPos, long edPos) throws IOException
	  {
		  if(edPos > posEnd) throw new IndexOutOfBoundsException();
		  return parent.writeToStream(out, posZero + stPos, posZero + edPos);
	  }
	  
	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid, or requested buffer size
	   * exceeds the maximum integer value.
	   * @throws NullPointerException If parent buffer has no contents.
	   */
	  public ByteBuffer toByteBuffer()
	  {
		  if (this.isEmpty()) throw new ArrayIndexOutOfBoundsException();
		  return parent.toByteBuffer(posZero, posEnd);
	  }
	  
	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid, or requested buffer size
	   * exceeds the maximum integer value.
	   * @throws NullPointerException If parent buffer has no contents.
	   */
	  public ByteBuffer toByteBuffer(int stPos, int edPos)
	  {
		  return this.toByteBuffer((long)stPos, (long)edPos);
	  }
	  
	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid, or requested buffer size
	   * exceeds the maximum integer value.
	   * @throws NullPointerException If parent buffer has no contents.
	   */
	  public ByteBuffer toByteBuffer(long stPos, long edPos)
	  {
		  long[] myPos = checkPositionPair(stPos, edPos);
		  if (myPos == null || myPos.length != 2) throw new IndexOutOfBoundsException();
		  return parent.toByteBuffer(myPos[0], myPos[1]);
	  }
	  
	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid.
	   */
	  public FileBuffer createCopy(int stPos, int edPos) throws IOException
	  {
		  return this.createCopy((long)stPos, (long)edPos);
	  }
	  
	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid.
	   */
	  public FileBuffer createCopy(long stPos, long edPos) throws IOException
	  {
		  long[] myPos = checkPositionPair(stPos, edPos);
		  if (myPos == null || myPos.length != 2) throw new IndexOutOfBoundsException();
		  
		  if(!parent.hasBlockAlignment()) return parent.createCopy(myPos[0], myPos[1]); //Just copy the parent data!
		  //Get the offsets the parent copy will snap to...
		  long[] pPos = parent.translateAlignedRange(myPos[0], myPos[1]);
		  if((pPos[0] == myPos[0]) && (pPos[1] == myPos[1])) return parent.createCopy(myPos[0], myPos[1]); //Offsets match anyway.
		  FileBuffer newp = parent.createCopy(pPos[0], pPos[1]);
		  long l_st = myPos[0] - pPos[0];
		  long l_ed = myPos[1] - pPos[0];
		  
		  return newp.createReadOnlyCopy(l_st, l_ed);
	  }
	  
	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid.
	   */
	  public FileBuffer createReadOnlyCopy(int stPos, int edPos)
	  {
		  return this.createReadOnlyCopy((long)stPos, (long)edPos);
	  }
	  
	  /**
	   * @throws IndexOutOfBoundsException If any position is invalid.
	   */
	  public FileBuffer createReadOnlyCopy(long stPos, long edPos)
	  {
		  long[] myPos = checkPositionPair(stPos, edPos);
		  if (myPos == null || myPos.length != 2) throw new IndexOutOfBoundsException();
		//  System.err.println("ROSubFileBuffer.createReadOnlyCopy || parent is null? " + (parent == null));
		  return parent.createReadOnlyCopy(myPos[0], myPos[1]);
	  }
	  
	  /**
	   * Get the minimum amount of memory taken up by buffer data.
	   * @return 0 by default as it itself takes up no memory in file data (only object info)
	   */
	  public long getMemoryBurden()
	  {
		  return getMinimumMemoryUsage();
	  }
	  
	  /**
	   * Overload for more accurate calculation.
	   * It can scan a collection of already counted FileBuffers for its parent.
	   * If its parent has already been counted, then it will return 0.
	   * If not, it will return the amount of memory its parent takes up in data.
	   * @param inMem : Collection (eg. List) of FileBuffer references already counted
	   * @return 0 if parent is in list, otherwise allocated size of parent it references.
	   */
	  public long getMemoryBurden(Collection<FileBuffer> inMem)
	  {
		  for (FileBuffer f : inMem)
		  {
			  if (f == this.parent) return getMinimumMemoryUsage();
		  }
		  inMem.add(parent);
		  return parent.getMemoryBurden(inMem) + getMinimumMemoryUsage();
	  }
	  
	  public long getMinimumMemoryUsage()
		{
			long tot = super.getMinimumMemoryUsage();
			int estPtrSz = SystemUtils.approximatePointerSize();
			tot += 8 + 8 + estPtrSz;
			return tot;
		}
	  
	  public String toString()
	  {
		  String str = "";
		  
		  str += "READ ONLY REFERENCE COPY!!\n";
		  str += "File Name: " + this.getName() + "\n";
		  str += "Directory: " + this.getDir() + "\n";
		  str += "Extension: " + this.getExt() + "\n";
		  str += "Parent File Size: " + super.getFileSize() + "\n";
		  str += "Child File Size: " + this.getFileSize() + "\n";
		  str += "Parent Base Buffer Capacity: " + super.getBaseCapacity() + "\n";
		  str += "Byte Order: ";
		  if (this.isBigEndian()) str += "Big-Endian \n";
		  else str += "Little-Endian\n";
		  str += "Parent Overflowing: " + super.isOverflowing() + "\n";
		  str += "Child Index Range: " + this.posZero + " - " + this.posEnd + "\n";
		  str += "First 32 Bytes: \n";
		  
		  for (int i = 0; i < 32; i++)
		  {
			  System.out.println("i = " + i);
			  if (i >= (int)this.getFileSize()) break;
			  str += byteToHexString(this.getByte(i)) + " ";
			  if (i % 16 == 15) str += "\n";
		  }
		  
		  return str;
	  }
	  
	  public String typeString()
	  {
		  return "Standard FileBuffer RO Reference\n";
	  }
	  
	  public void dispose()
	  {
	  		delinkParent(parent);
	  		parent = null;
	  		posZero = 0;
	  		posEnd = 0;
	  }
	  
	  /* --- Parent Access --- */
	  
	  /**
	   * Returns the start offset relative to the parent.
	   * @param key Parent to retrieve offset relative to.
	   * @return Long integer representing the offset from the parent start that this buffer uses
	   * as offset 0.
	   */
	  protected long getStartOffset(FileBuffer key)
	  {
		  return this.posZero;
	  }
	  
	  /**
	   * Returns the end offset relative to the parent.
	   * @param key Parent to retrieve offset relative to.
	   * @return Long integer representing the first byte position in the parent after
	   * the end of the section this child references.
	   */
	  protected long getEndOffset(FileBuffer key)
	  {
		  return this.posEnd;
	  }
	  
	  protected void setStartOffset(FileBuffer key, long newStart)
	  {
		  if (key == this.parent)
		  {
			  if (newStart < 0) newStart = 0;
			  if (newStart > parent.getFileSize()) newStart = parent.getFileSize();
			  this.posZero = newStart;
		  }
	  }
	  
	  protected void setEndOffset(FileBuffer key, long newEnd)
	  {
		  if (key == this.parent)
		  {
			  if (newEnd < this.posZero) newEnd = posZero;
			  if (newEnd > parent.getFileSize()) newEnd = parent.getFileSize();
			  this.posEnd = newEnd;
		  }
	  }
	  
	  protected void delinkParent(FileBuffer key)
	  {
		  if (key != parent) return;
		  parent = null;
		  this.posZero = -1;
		  this.posEnd = -1;
	  }
	  
	  /* --- Other --- */
	  
	  public ROSubFileBuffer copyReference(){
		  return new ROSubFileBuffer(parent, posZero, posEnd);
	  }
	  
}
