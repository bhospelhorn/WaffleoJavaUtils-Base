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
 * 
 * 
 */

/**
 * A simpler successor to CompositeBuffer, this is a structure
 * for chaining together multiple file buffers that may not have been
 * together initially.
 * <br>This can be handy for serializing into an object to hold in memory.
 * <br>CachedFileBuffer utilizes very similar logic and can be used in the same way.
 * @author Blythe Hospelhorn
 * @version 1.0.0
 * @since February 14, 2020
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
		if(last_read.onPage(offset)) return last_read;
		
		Page page = root_page;
		while(page != null)
		{
			if(page.onPage(offset)) return page;
			if(offset < page.offset) page = page.child_l;
			else page = page.child_r;
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
		if(p == null) throw new IndexOutOfBoundsException();
		
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
		Page pg = new Page(current_file_size);
		pg.data = addition.createReadOnlyCopy(stPos, edPos);
		current_file_size += pg.data.getFileSize();
		list.add(pg);
		Page last = list.get(list.size()-1);
		while(last.child_r != null) last = last.child_r;
		last.child_r = pg;
		pg.parent = last;
		
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
  		
  		for(Page p : list)
  		{
  			long epos = cpos + p.data.getFileSize();
  			if(epos < stPos){cpos = epos; continue;}
  			
  			long st = 0;
  			if(stPos > cpos) st = stPos-cpos;
  			long ed = p.data.getFileSize();
  			if(ed > edPos) ed = edPos;
  			
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
  		
  		long cpos = 0;
  		
  		for(Page p : list)
  		{
  			long epos = cpos + p.data.getFileSize();
  			if(epos < stPos){cpos = epos; continue;}
  			
  			long st = 0;
  			if(stPos > cpos) st = stPos-cpos;
  			long ed = p.data.getFileSize();
  			if(ed > edPos) ed = edPos;
  			
  			bb.put(p.data.getBytes(st, ed));
  			
  			cpos = epos;
  			if(cpos >= edPos) break;
  		}
  		
  		bb.rewind();
  		return bb;
  	}
  	
  	public FileBuffer createCopy(int stPos, int edPos)
  	{
  		return createCopy(Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
  	}
  	
  	public FileBuffer createCopy(long stPos, long edPos)
  	{
  		MultiFileBuffer copy = new MultiFileBuffer(list.size()+1);
  		copy.current_file_size = this.current_file_size;
  		
  		for(Page p : list)
  		{
  			Page pcopy = new Page(p.offset);
  			FileBuffer dat = p.data;
  			if(dat instanceof ROSubFileBuffer)
  			{
  				pcopy.data = ((ROSubFileBuffer)dat).copyReference();
  			}
  			else pcopy.data = dat.createReadOnlyCopy(0, dat.getFileSize());
  		}
  		
  		copy.rebuildTree();
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
	
}
