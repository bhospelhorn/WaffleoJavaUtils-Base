package waffleoRai_Utils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/*
 * UPDATES ----
 * 
 * 2020.02.14 | Initial 1.0.0 Documentation
 * 2020.06.18 | 1.0.0 -> 1.0.1
 * 		Debugging
 * 2020.06.21 | 1.0.1 -> 1.1.0
 * 		createCopy(long,long) should be only copying between points, was copying whole damn thing.
 * 2020.06.23 | 1.1.0 -> 1.1.1
 * 		Debugging toByteBuffer()
 * 2020.08.21 | 1.1.1 -> 1.2.0
 * 		Added flush()
 */

/**
 * A simpler successor to CompositeBuffer, this is a structure
 * for chaining together multiple file buffers that may not have been
 * together initially.
 * <br>This can be handy for serializing into an object to hold in memory.
 * <br>CachedFileBuffer utilizes very similar logic and can be used in the same way.
 * @author Blythe Hospelhorn
 * @version 1.2.0
 * @since August 21, 2020
 *
 */
public class MultiFileBuffer extends FileBuffer{
	
	private static final int RESTRUCT_AFTER = 16;
	
	private List<Page> list;
	private long current_file_size;
	
	private int restruct_counter;
	private Page root_page; //For tree
	private Page last_read;
	
	/* ----- INTERNAL STRUCTURES ----- */
	
	protected class Page
	{
		private long offset;
		private FileBuffer data;
		
		private Page parent;
		private Page child_l;
		private Page child_r;
		
		public Page(long off){offset = off;}
		
		public void clearLinks()
		{
			parent = null;
			child_l = null;
			child_r = null;
		}
		
		public boolean onPage(long off)
		{
			if(off < offset) return false;
			if(off >= offset + data.getFileSize()) return false;
			return true;
		}
		
		public Page getTreeParent()
		{
			return parent;
		}
		
	}
	
	/* ----- CONSTRUCTORS ----- */
	
	/**
	 * Construct a MultiFileBuffer backed by a LinkedList.
	 */
	public MultiFileBuffer()
	{
		list = new LinkedList<Page>();
		setReadOnly();
	}
	
	/**
	 * Construct a MultiFileBuffer backed by an ArrayList initialized
	 * with the specified capacity.
	 * @param capacity Initial ArrayList size.
	 */
	public MultiFileBuffer(int capacity)
	{
		list = new ArrayList<Page>(capacity);
		setReadOnly();
	}
	
	/* ----- STRUCTURE ----- */
	
	/*private void adjustOffsets()
	{
		long pos = 0;
		for(Page p : list)
		{
			p.offset = pos;
			pos += p.data.getFileSize();
		}
	}*/
	
	private void treeBranch(Page[] pages, int st, int ed, int idx)
	{
		if(st >= (ed-1)) return;
		Page parent = pages[idx];
		
		//Left
		if(idx > st)
		{
			int i = (idx - st)/2;
			i += st;
			if(i < idx)
			{
				Page left = pages[i];
				parent.child_l = left;
				left.parent = parent;
				treeBranch(pages, st, idx, i);		
			}
		}
		
		//Right
		if(idx < ed-1)
		{
			int i = (ed - idx)/2;
			i+=idx;
			if(i > idx)
			{
				Page right = pages[i];
				parent.child_r = right;
				right.parent = parent;
				treeBranch(pages, idx+1, ed, i);	
			}
		}
	}
	
	private void rebuildTree()
	{
		restruct_counter = 0;
		if(list.isEmpty()) {
			root_page = null;
			current_file_size = 0;
			return;
		}
		
		int pcount = list.size();
		Page[] pages = new Page[pcount];
		
		int i = 0;
		for(Page p : list){pages[i] = p; i++; p.clearLinks();}
		
		int mididx = pcount/2;
		root_page = pages[mididx];
		treeBranch(pages, 0, pcount, mididx);
	}
	
	private Page getPage(long offset)
	{
		if(last_read == null) {
			if(list.isEmpty()) return null;
			last_read = list.get(0);
		}
		if(last_read.onPage(offset)) return last_read;
		Page page = root_page;
		while(page != null)
		{
			//System.err.println("Checking page 0x" + Long.toHexString(page.offset) + " - 0x" + Long.toHexString(page.offset + page.data.getFileSize()));
			if(page.onPage(offset)) return page;
			if(offset < page.offset){
				//System.err.println("Going left...");
				page = page.child_l;
			}
			else{
				//System.err.println("Going right...");
				page = page.child_r;
			}
		}
		
		return null;
	}
	
	/* ----- BASIC GETTERS ----- */
	
	public long getFileSize()
	{
		return current_file_size;
	}
  
	public byte getByte(int position)
	{
		return getByte(Integer.toUnsignedLong(position));
	}
  
	public byte getByte(long position)
	{
		Page p = getPage(position);
		if(p == null) throw new IndexOutOfBoundsException("Page not found for position 0x" + Long.toHexString(position));
		
		last_read = p;
		long poff = position - p.offset;
		
		return p.data.getByte(poff);
	}

	public long getMemoryBurden()
	{
		return getMinimumMemoryUsage();
	}
  
	public long getMemoryBurden(Collection<FileBuffer> inMem)
	{
		return getMemoryBurden();
	}
	
  	public long getMinimumMemoryUsage()
  	{
  		long est = 0;
  		est = 8+8+4+8+8;
  		
  		for(Page p : list)
  		{
  			est += p.data.getFileSize();
  			est+=40;
  		}
  		
  		return est;
  	}
	
	protected boolean contentsEqual(FileBuffer other)
	{
		return (this == other);
	}
	
	/* ----- BASIC SETTERS ----- */

	public void unsetReadOnly()
	{
		throw new UnsupportedOperationException();
	}
	
	/* ----- CAPACITY MANAGEMENT ----- */
	
	public void changeBaseCapacity(int newCapacity)
	{
		throw new UnsupportedOperationException();
	}
  
	public void adjustBaseCapacityToSize()
	{
		throw new UnsupportedOperationException();
	}
	
	/* ----- ADDITION TO FILE ----- */
	
	public void addToFile(FileBuffer addition)
	{
		addToFile(addition, 0, addition.getFileSize());
	}
  
	public void addToFile(FileBuffer addition, int stPos, int edPos)
	{
		addToFile(addition, Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
	}
 
	public void addToFile(FileBuffer addition, long stPos, long edPos)
	{
		//Get previous page, if present
		Page last = null;
		if(!list.isEmpty()) last = list.get(list.size()-1);
		
		Page pg = new Page(current_file_size);
		pg.data = addition.createReadOnlyCopy(stPos, edPos);
		current_file_size += pg.data.getFileSize();
		list.add(pg);
		
		//Tack onto last page added...
		if(last != null){
			while(last.child_r != null) last = last.child_r;
			last.child_r = pg;
			pg.parent = last;
		}
		else rebuildTree();
		
		restruct_counter++;
		if(restruct_counter >= RESTRUCT_AFTER) rebuildTree();
	}
	
	/* ----- CONTENT RETRIEVAL ----- */
	
	public byte[] getBytes()
	{
		if(current_file_size > 0x7FFFFFFF) throw new IndexOutOfBoundsException();
		return getBytes(0, current_file_size);
	}
	
	/* ----- WRITING TO DISK ----- */
	
	public void writeFile(String path, long stPos, long edPos) throws IOException
	{
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		writeToStream(bos, stPos, edPos);
		bos.close();
	}
	
	public void appendToFile(String path, long stPos, long edPos) throws IOException
	{
		long cpos = 0;
  		
  		for(Page p : list)
  		{
  			long epos = cpos + p.data.getFileSize();
  			if(epos < stPos){cpos = epos; continue;}
  			
  			long st = 0;
  			if(stPos > cpos) st = stPos-cpos;
  			long ed = p.data.getFileSize();
  			if(ed > edPos) ed = edPos;
  			
  			p.data.appendToFile(path, st, ed);
  			
  			cpos = epos;
  			if(cpos >= edPos) break;
  		}
		
	}
	
	public long writeToStream(OutputStream out) throws IOException
  	{
		long w = 0;
		for(Page p : list){p.data.writeToStream(out); w += p.data.getFileSize();}
		
  		return w;
  	}
  	
  	public long writeToStream(OutputStream out, long stPos, long edPos) throws IOException
  	{
  		long w = 0;
  		long cpos = 0;
  		
  		//int dc = -1; //Debug counter
  		//System.err.println("Pages detected: " + list.size());
  		//System.err.println("edPos = 0x" + Long.toHexString(edPos));
  		for(Page p : list)
  		{
  			//dc++;
  			
  			long epos = cpos + p.data.getFileSize();
  			if(epos < stPos){cpos = epos; continue;}
  			
  			long st = 0;
  			if(stPos > cpos) st = stPos-cpos;
  			long ed = p.data.getFileSize();
  			if(ed > edPos) ed = edPos;
  			
  			//System.err.println("MultiFileBuffer.writeToStream || Writing page " + dc);
  			//System.err.println("\t Page Size 0x" + Long.toHexString(p.data.getFileSize()));
  			//System.err.println("\t Writing 0x" + Long.toHexString(st) + " - 0x" + Long.toHexString(ed));
  			p.data.writeToStream(out, st, ed);
  			w += (ed-st);
  			
  			cpos = epos;
  			if(cpos >= edPos) break;
  		}
  		
  		return w;
  	}
  	
  	/* ----- STATUS CHECKERS ----- */
  	
  	public boolean isEmpty()
  	{
  		return (list.isEmpty());
  	}
  	
  	/* ----- CONVERSION ----- */
  	
  	public ByteBuffer toByteBuffer()
  	{
  		return toByteBuffer(0, current_file_size);
  	}
  	
  	public ByteBuffer toByteBuffer(int stPos, int edPos)
  	{
  		return toByteBuffer(Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
  	}
  	
  	public ByteBuffer toByteBuffer(long stPos, long edPos)
  	{
  		long sz = edPos - stPos;
  		if(sz > 0x7FFFFFFF) throw new IndexOutOfBoundsException();
  		ByteBuffer bb = ByteBuffer.allocate((int)sz);
  		
  		boolean copying = false;
  		for(Page p : list){
  			long st = 0;
  			if(!copying){
  				if(p.onPage(stPos)){
  					copying = true;
  					st = stPos - p.offset;
  				}
  				else continue;
  			}

  			long ed = p.data.getFileSize();
  			if(p.onPage(edPos)){
  				copying = false;
  				ed = edPos - p.offset;
  			}
  			
  			bb.put(p.data.getBytes(st, ed));
  		}
  		
  		bb.rewind();
  		return bb;
  	}
  	
  	public FileBuffer createCopy(){
  		MultiFileBuffer copy = new MultiFileBuffer(list.size()+1);
  		copy.current_file_size = this.current_file_size;
  		//System.err.println("MultiFileBuffer.createCopy || File Size: 0x" + Long.toHexString(copy.current_file_size));
  		
  		//int c = 0;
  		for(Page p : list){
  			//System.err.println("MultiFileBuffer.createCopy || copying page: " + c++);
  			Page pcopy = new Page(p.offset);
  			FileBuffer dat = p.data;
  			if(dat instanceof ROSubFileBuffer){
  				//System.err.println("MultiFileBuffer.createCopy || page is reference ");
  				pcopy.data = ((ROSubFileBuffer)dat).copyReference();
  			}
  			else pcopy.data = dat.createReadOnlyCopy(0, dat.getFileSize());
  			copy.list.add(pcopy);
  		}
  		
  		copy.rebuildTree();
  		//System.err.println("MultiFileBuffer.createCopy || File Size (after rebuild): 0x" + Long.toHexString(copy.current_file_size));
  		return copy;
  	}
  	
  	public FileBuffer createCopy(int stPos, int edPos)
  	{
  		return createCopy(Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
  	}
  	
  	public FileBuffer createCopy(long stPos, long edPos)
  	{
  		//Sanity checks
  		if(stPos < 0) throw new IndexOutOfBoundsException("Start position must be at least 0!");
  		if(edPos > current_file_size) throw new IndexOutOfBoundsException("End position cannot exceed 0x" + Long.toHexString(current_file_size));
  		if(stPos >= edPos) throw new IndexOutOfBoundsException("End position must be greater than start position!");
  		
  		MultiFileBuffer copy = new MultiFileBuffer(list.size()+1);
  		copy.current_file_size = edPos - stPos;
  		//System.err.println("MultiFileBuffer.createCopy || Target Size: 0x" + Long.toHexString(copy.current_file_size));

  		boolean copying = false;
  		long cpos = 0;
  		for(Page p : list){
  			//System.err.println("MultiFileBuffer.createCopy || cpos: 0x" + Long.toHexString(cpos));
  			boolean wholepage = true;
  			long st = 0;
  			if(!copying){
  				if(!p.onPage(stPos)) continue;
  				else{
  					copying = true;
  					st = stPos - p.offset;
  					if(st != 0) wholepage = false;
  				}
  			}
  			
  			//Check end
  			long ed = p.data.getFileSize();
  			if(p.onPage(edPos)){
  				copying = false;
  				ed = edPos - p.offset;
  				if(ed == 0) break;
  				wholepage = false;
  			}
  			
  			Page pcopy = new Page(cpos);
  			if(wholepage){
  				FileBuffer dat = p.data;
  	  			if(dat instanceof ROSubFileBuffer){
  	  				pcopy.data = ((ROSubFileBuffer)dat).copyReference();
  	  			}
  	  			else pcopy.data = dat.createReadOnlyCopy(0, dat.getFileSize());
  	  			copy.list.add(pcopy);
  	  			cpos += dat.getFileSize();
  			}
  			else{
  				//System.err.println("MultiFileBuffer.createCopy || Copying page: 0x" + Long.toHexString(st) + " - 0x" + Long.toHexString(ed));
  				//System.err.println("MultiFileBuffer.createCopy || Page size: 0x" + Long.toHexString(p.data.getFileSize()));
  				pcopy.data = p.data.createReadOnlyCopy(st, ed);
  	  			copy.list.add(pcopy);
  	  			cpos += pcopy.data.getFileSize();
  			}
  			
  			if(!copying) break;
  		}
  		
  		copy.rebuildTree();
  		//System.err.println("MultiFileBuffer.createCopy || File Size (after rebuild): 0x" + Long.toHexString(copy.current_file_size));
  		return copy;
  	}
  	
  	/* ----- CLEAN UP ----- */
  	
  	public void dispose() throws IOException
  	{
  		for(Page p : list) p.data.dispose();
  		current_file_size = 0;
  		list.clear();
  		root_page = null;
  		last_read = null;
  	}
	
  	public void flush() throws IOException{
  		for(Page p : list){
  			if(p.data != null) p.data.flush();
  		}
  	}
  	
}
