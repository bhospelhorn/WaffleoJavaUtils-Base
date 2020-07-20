package waffleoRai_Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/*
 * UPDATES
 * 
 * 1.0.0 | August 5, 2019
 * 
 * 1.1.0 | February 7, 2020
 * 	Actually started writing functions...
 * 
 * 1.1.1 | July 3, 2020
 * 	getBytes(long, long) debug. Was always reading from start of first page.
 * 
 * 1.1.2 | July 16, 2020
 * 	If read in from a path, should save that path.
 */

/**
 * A better version of StreamBuffer (which was a cache, not a stream)
 * that facilitates random access in large files without loading the
 * full file into memory.
 * <br>The size of each page and the number of pages (with data in memory) can be modified.
 * <br>WARNING: This class is not threadsafe.
 * @author Blythe Hospelhorn
 * @version 1.1.2
 * @since July 16, 2020
 */
public class CacheFileBuffer extends FileBuffer{
	
	/* ----- Constants ----- */
	
	public static final int DEFO_PAGE_SIZE = 0x1000; //4096
	public static final int DEFO_PAGE_NUM = 0x10000; //65536
	
	public static final int TREE_RESTRUCT_PAGES = 16; //Rebuild tree every time 8 pages are added
	
	/* ----- Instance Variables ----- */
	
	private String tempdir;
	private Random rng;
	//private boolean threadsafe;
	
	private int page_size;
	private int page_count;
	private int p_add_count;
	
	private long current_file_size;
	private boolean dirty_offsets; 
	//If dirty_offsets is set, the offsets recorded in the Pages are not accurate
	//(because bytes have been written to the middle)
	//Update if page offsets need to be accurate
	
	private CachePage first_page; //For linked list
	private CachePage last_page;
	private CachePage root_page; //For search tree
	
	private List<String> temp_files;
	private Deque<CachePage> freeQueue;
	
	private CachePage last_read_page;
	private CachePage last_written_page; //Not for end - random access writes only
	
	/* ----- Cache ----- */
	
	protected class CachePage
	{
		/*--- Instance Variables ---*/
		
		private String src_path;
		private long src_offset;
		
		private boolean isDirty;
		private boolean write_flag;
		private long offset_value;
		private int size;
		
		private FileBuffer loaded_data;
		
		/*--- Tumbleweed ---*/
		
		private CachePage parent;
		private CachePage child_l;
		private CachePage child_r;
		private CachePage previous;
		private CachePage next;
		
		/*--- Construction ---*/
		
		public CachePage(String srcpath, long srcoff, long myoff, int size)
		{
			src_path = srcpath;
			src_offset = srcoff;
			isDirty = false;
			offset_value = myoff;
			this.size = size;
		}
		
		/*--- Disk ---*/
		
		public void loadData() throws IOException
		{
			loaded_data = new FileBuffer(src_path, src_offset, src_offset + size, isBigEndian());
		}
		
		public void freeData()
		{
			loaded_data = null;
		}
		
		public void freeMemory() throws IOException
		{
			//If dirty, write to disk...
			if(isDirty())
			{
				loaded_data.writeFile(src_path);
			}
			loaded_data = null;
		}
		
		public boolean writeDataTo(OutputStream out) throws IOException
		{
			if(loaded_data == null) return false;
			if(isDirty)
			{
				out.write(loaded_data.getBytes(0, loaded_data.getFileSize()));
				return true;
			}
			else
			{
				out.write(loaded_data.getBytes());
				return true;
			}
		}
		
		/*--- Getters ---*/
		
		public String getSourcePath(){return this.src_path;}
		public long getSourceOffset(){return this.src_offset;}
		public long getOffset(){return this.offset_value;}
		public int getSize(){return this.size;}
		public boolean isDirty(){return this.isDirty;}
		public boolean writeFlag(){return this.write_flag;}
		public FileBuffer getDataBuffer(){return this.loaded_data;}
		public boolean dataLoaded(){return (this.loaded_data != null);}
		
		public CachePage getParent(){return parent;}
		public CachePage getLeftChild(){return child_l;}
		public CachePage getRightChild(){return child_r;}
		public CachePage getNextPage(){return next;}
		public CachePage getPreviousPage(){return previous;}
		
		public FileBuffer getDataBufferForWrite()
		{
			setDirty();
			if(size == 0)
			{
				loaded_data = new FileBuffer(page_size, isBigEndian());
			}
			return this.loaded_data;
		}
		
		/*--- Setters ---*/
		
		public void setOffset(long off){this.offset_value = off;}
		public void setDirty(){this.isDirty = true;}
		public void setWriteFlag(){this.write_flag = true;}
		public void incrementSize(){this.size++; current_file_size++;}
		public void incrementSize(int n){this.size+=n; current_file_size+=n;}
		public void decrementSize(){this.size--; current_file_size--;}
		public void decrementSize(int n){this.size-=n; current_file_size-=n;}
		
		public boolean copyFromBuffer(FileBuffer src, long start, long end)
		{
			isDirty = true;
			try 
			{
				loaded_data = src.createCopy(start, end);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				return false;
			}
			incrementSize((int)(end-start));
			return true;
		}
		
		/*--- Other ---*/
		
		public boolean equals(Object o)
		{
			return (this == o);
		}
		
		public boolean onPage(long offset)
		{
			if(offset < this.offset_value) return false;
			if(offset >= this.offset_value + this.size) return false;
			return true;
		}
		
		public long estimateMemorySize()
		{
			long est = 40 + 8+8+8+4+1+8;
			if(src_path != null) est += src_path.length() << 1;
			if(loaded_data != null) est += loaded_data.getMinimumMemoryUsage();
			return est;
		}
		
		public CachePage split(long page_offset) throws IOException //Returns page to insert new page BEFORE
		{
			if(page_offset == 0) return this;
			if(page_offset >= getSize()) return next;
			if(!dataLoaded()) loadData();
			
			//Left
			String temppath = tempdir + File.separator + 
					"cachebuff_page_" + Long.toHexString(OffsetDateTime.now().toEpochSecond()) +
					"_" + Long.toHexString(rng.nextLong()) + ".tmp";
			temp_files.add(temppath);
			
			CachePage left = new CachePage(temppath, 0, this.offset_value, (int)page_offset);
			left.loaded_data = this.loaded_data.createCopy(0, page_offset);
			left.setWriteFlag();
			left.setDirty();
			freeQueue.add(left);
			
			//Right
			temppath = tempdir + File.separator + 
					"cachebuff_page_" + Long.toHexString(OffsetDateTime.now().toEpochSecond()) +
					"_" + Long.toHexString(rng.nextLong()) + ".tmp";
			temp_files.add(temppath);
			
			int rsz = size - (int)page_offset;
			CachePage right = new CachePage(temppath, 0, offset_value + page_offset, rsz);
			right.loaded_data = this.loaded_data.createCopy(page_offset, size);
			right.setWriteFlag();
			right.setDirty();
			freeQueue.add(right);
			
			//Link up
			left.next = right;
			right.previous = left;
			left.previous = this.previous;
			if(this.previous != null) this.previous.next = left;
			right.next = this.next;
			if(this.next != null) this.next.previous = right;
			if(first_page == this) first_page = left;
			if(last_page == this) last_page = right;
			
			left.parent = this.parent;
			if(this.parent != null)
			{
				if(this.parent.child_l == this) this.parent.child_l = left;
				else if (this.parent.child_r == this) this.parent.child_r = left;
			}
			left.child_l = this.child_l;
			if(this.child_l != null) this.child_l.parent = left;
			left.child_r = right;
			right.parent = left;
			right.child_r = this.child_r;
			if(this.child_r != null) this.child_r.parent = right;
			if(root_page == this) root_page = left;
			
			//Free me from the queue...
			freeQueue.remove(this);
			freeData();
			
			return right;
		}
		
		public void loadToCache() throws IOException
		{
			if(!this.dataLoaded()) 
			{
				if(freeQueue.size() >= page_count)
				{
					CachePage old = freeQueue.pop();
					old.freeMemory();
				}	
				loadData();
			}
			else freeQueue.remove(this);
			freeQueue.add(this);
		}
		
		public void writeToStream(OutputStream out) throws IOException
		{
			loadToCache();
			//out.write(loaded_data.getBytes(0, (long)size));
			loaded_data.writeToStream(out, 0, (long)size);
		}
		
		public void writeToStream(OutputStream out, long stpos, long edpos) throws IOException
		{
			loadToCache();
			//out.write(loaded_data.getBytes(stpos, edpos));
			loaded_data.writeToStream(out, stpos, edpos);
		}
		
		public void appendToFile(Path path, long stpos, long edpos) throws IOException
		{
			loadToCache();
			Files.write(path, loaded_data.getBytes(stpos, edpos), StandardOpenOption.APPEND);
		}
		
		public CachePage createWritableCopy(long stpos, int sz)
		{
			CachePage copy = new CachePage(src_path, src_offset + stpos, offset_value, sz);
			
			try
			{
				if(write_flag)
				{
					copy.write_flag = true;
					//Change source path...
					String temppath = tempdir + File.separator + 
							"cachebuff_page_" + Long.toHexString(OffsetDateTime.now().toEpochSecond()) +
							"_" + Long.toHexString(rng.nextLong()) + ".tmp";
					
					loaded_data.writeFile(temppath, stpos, stpos+sz);
					
					copy.src_path = temppath;
					copy.src_offset = 0;
				}
			}
			catch(IOException x)
			{
				x.printStackTrace();
				return null;
			}
			
			return copy;
		}
		
	}
	
	/* ----- Construction ----- */
	
	private CacheFileBuffer() throws IOException
	{
		//This is just an override to prevent use of defo constructor
		this(DEFO_PAGE_SIZE, DEFO_PAGE_NUM, false);
	}
	
	private CacheFileBuffer(int pageSize, int pageCount, boolean allowWrite) throws IOException
	{
		rng = new Random();
		tempdir = FileBuffer.getTempDir();
		
		page_count = pageCount;
		page_size = pageSize;
		
		current_file_size = 0;
		dirty_offsets = false;
		if(!allowWrite) super.setReadOnly();
		
		freeQueue = new LinkedList<CachePage>();
		temp_files = new LinkedList<String>();
	}

	/* ----- Static Object Generators ----- */
	
	private void loadReadOnlyCacheBuffer(String filepath, long stoff, long edoff) throws IOException
	{
		//Build reference tree
		if(!FileBuffer.fileExists(filepath)) throw new IOException();
		//current_file_size = FileBuffer.fileSize(filepath);
		current_file_size = edoff - stoff;
		
		//Generate all nodes and store in array.
		boolean lastpg = (current_file_size % page_size != 0);
		int total_pgs = (int)(current_file_size / page_size);
		if(lastpg) total_pgs++;
		CachePage[] pgs = new CachePage[total_pgs];
		int ct = total_pgs;
		if(lastpg) ct--;
		long pos = stoff;
		long relpos = 0;
		for(int i = 0; i < ct; i++)
		{
			pgs[i] = new CachePage(filepath, pos, relpos, page_size);
			pos += page_size;
			relpos += page_size;
		}
		if(lastpg)
		{
			int sz = (int)(current_file_size - pos);
			pgs[ct] = new CachePage(filepath, pos, relpos, sz);
		}
		
		//Form linked list
		first_page = pgs[0];
		for(int i = 0; i < pgs.length; i++)
		{
			if(i > 0) pgs[i].previous = pgs[i-1];
			if(i < pgs.length-1) pgs[i].next = pgs[i+1];
		}
		last_page = pgs[pgs.length-1];
		
		//Form tree
		int mid = pgs.length >>> 1;
		root_page = pgs[mid];
		buildPageTree(mid, pgs, 0, pgs.length);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, DEFO_PAGE_SIZE, DEFO_PAGE_NUM, 0, FileBuffer.fileSize(filepath), true);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param bigEndian True if the byte order is to be set to Big Endian. False for Little Endian.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, boolean bigEndian) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, DEFO_PAGE_SIZE, DEFO_PAGE_NUM, 0, FileBuffer.fileSize(filepath), bigEndian);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, int pageSize) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, pageSize, DEFO_PAGE_NUM, 0, FileBuffer.fileSize(filepath), true);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @param bigEndian True if the byte order is to be set to Big Endian. False for Little Endian.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, int pageSize, boolean bigEndian) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, pageSize, DEFO_PAGE_NUM, 0, FileBuffer.fileSize(filepath), bigEndian);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @param pageCount Maximum number of loaded pages that should be held in memory.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, int pageSize, int pageCount) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, pageSize, pageCount, 0, FileBuffer.fileSize(filepath), true);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @param pageCount Maximum number of loaded pages that should be held in memory.
	 * @param bigEndian True if the byte order is to be set to Big Endian. False for Little Endian.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, int pageSize, int pageCount, boolean bigEndian) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, pageSize, pageCount, 0, FileBuffer.fileSize(filepath), bigEndian);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param stPos Start position (inclusive) to begin reading input file at. All offsets
	 * referenced in this buffer are relative to the start position at load.
	 * @param edPos End position (exclusive) of input file at which to stop reading.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, long stPos, long edPos) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, DEFO_PAGE_SIZE, DEFO_PAGE_NUM, stPos, edPos, true);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param stPos Start position (inclusive) to begin reading input file at. All offsets
	 * referenced in this buffer are relative to the start position at load.
	 * @param edPos End position (exclusive) of input file at which to stop reading.
	 * @param bigEndian True if the byte order is to be set to Big Endian. False for Little Endian.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, long stPos, long edPos, boolean bigEndian) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, DEFO_PAGE_SIZE, DEFO_PAGE_NUM, stPos, edPos, bigEndian);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @param pageCount Maximum number of loaded pages that should be held in memory.
	 * @param stPos Start position (inclusive) to begin reading input file at. All offsets
	 * referenced in this buffer are relative to the start position at load.
	 * @param edPos End position (exclusive) of input file at which to stop reading.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, int pageSize, int pageCount, long stPos, long edPos) throws IOException
	{
		return getReadOnlyCacheBuffer(filepath, pageSize, pageCount, stPos, edPos, true);
	}
	
	/**
	 * Create a read-only cached buffer for the file indicated by the provided path.
	 * @param filepath Path of file to open read-only.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @param pageCount Maximum number of loaded pages that should be held in memory.
	 * @param stPos Start position (inclusive) to begin reading input file at. All offsets
	 * referenced in this buffer are relative to the start position at load.
	 * @param edPos End position (exclusive) of input file at which to stop reading.
	 * @param bigEndian True if the byte order is to be set to Big Endian. False for Little Endian.
	 * @return Cached buffer that allows random read-only access to specified file.
	 * @throws IOException If file could not be found or opened.
	 */
	public static CacheFileBuffer getReadOnlyCacheBuffer(String filepath, int pageSize, int pageCount, long stPos, long edPos, boolean bigEndian) throws IOException
	{
		CacheFileBuffer buffer = new CacheFileBuffer(pageSize, pageCount, false);
		
		buffer.setDir(FileBuffer.chopPathToDir(filepath));
		buffer.setName(FileBuffer.chopPathToFName(filepath));
		buffer.setExt(FileBuffer.chopPathToExt(filepath));
		
		buffer.setEndian(bigEndian);
		buffer.loadReadOnlyCacheBuffer(filepath, stPos, edPos);
		
		return buffer;
	}
	
	/**
	 * Create a writable cached buffer. When memory limit specified by page size and count
	 * is reached, pages will be written to temporary files on disk.
	 * @return A write-enabled cached buffer.
	 * @throws IOException If the cached buffer could not be created.
	 */
	public static CacheFileBuffer getWritableCacheBuffer() throws IOException
	{
		return getWritableCacheBuffer(DEFO_PAGE_SIZE, DEFO_PAGE_NUM, true);
	}
	
	/**
	 * Create a writable cached buffer. When memory limit specified by page size and count
	 * is reached, pages will be written to temporary files on disk.
	 * @param bigEndian True if the byte order is to be set to Big Endian. False for Little Endian.
	 * @return A write-enabled cached buffer.
	 * @throws IOException If the cached buffer could not be created.
	 */
	public static CacheFileBuffer getWritableCacheBuffer(boolean bigEndian) throws IOException
	{
		return getWritableCacheBuffer(DEFO_PAGE_SIZE, DEFO_PAGE_NUM, bigEndian);
	}
	
	/**
	 * Create a writable cached buffer. When memory limit specified by page size and count
	 * is reached, pages will be written to temporary files on disk.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @return A write-enabled cached buffer.
	 * @throws IOException If the cached buffer could not be created.
	 */
	public static CacheFileBuffer getWritableCacheBuffer(int pageSize) throws IOException 
	{
		return getWritableCacheBuffer(pageSize, DEFO_PAGE_NUM, true);
	}
	
	/**
	 * Create a writable cached buffer. When memory limit specified by page size and count
	 * is reached, pages will be written to temporary files on disk.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @param pageCount Maximum number of loaded pages that should be held in memory.
	 * @return A write-enabled cached buffer.
	 * @throws IOException If the cached buffer could not be created.
	 */
	public static CacheFileBuffer getWritableCacheBuffer(int pageSize, int pageCount) throws IOException
	{
		return getWritableCacheBuffer(pageSize, pageCount, true);
	}
	
	/**
	 * Create a writable cached buffer. When memory limit specified by page size and count
	 * is reached, pages will be written to temporary files on disk.
	 * @param pageSize Size in bytes of cache pages buffer should use.
	 * @param pageCount Maximum number of loaded pages that should be held in memory.
	 * @param bigEndian True if the byte order is to be set to Big Endian. False for Little Endian.
	 * @return A write-enabled cached buffer.
	 * @throws IOException If the cached buffer could not be created.
	 */
	public static CacheFileBuffer getWritableCacheBuffer(int pageSize, int pageCount, boolean bigEndian) throws IOException
	{
		CacheFileBuffer buffer = new CacheFileBuffer(pageSize, pageCount, true);
		buffer.setEndian(bigEndian);
		
		return buffer;
	}
	
	/* ----- Page Management ----- */
	
	private void rebuildTree()
	{
		p_add_count = 0;
		
		int pcount = 0;
		CachePage page = first_page;
		while(page != null)
		{
			pcount++;
			page.parent = null;
			page.child_l = null;
			page.child_r = null;
			page = page.getNextPage();
		}
		
		CachePage[] pages = new CachePage[pcount];
		page = first_page;
		int i = 0;
		while(page != null)
		{
			pages[i] = page;
			page = page.getNextPage();
			i++;
		}
		
		int root_idx = pcount/2;
		root_page = pages[root_idx];
		buildPageTree(root_idx, pages, 0, pcount);
	}
	
	private void buildPageTree(int pidx, CachePage[] pages, int st, int ed)
	{
		if(st >= (ed-1)) return;
		CachePage parent = pages[pidx];
		
		//Left
		if(pidx > st)
		{
			int mid = (pidx-st >>> 1) + st;
			if(mid < pidx)
			{
				parent.child_l = pages[mid];
				parent.child_l.parent = parent;
				buildPageTree(mid, pages, st, pidx);	
			}
		}
		
		//Right
		if(pidx < ed)
		{
			int mid = (ed-pidx >>> 1) + pidx;
			if(mid > pidx)
			{
				parent.child_r = pages[mid];
				parent.child_r.parent = parent;
				buildPageTree(mid, pages, pidx+1, ed);	
			}
		}
		
	}
	
	private void updateDirtyOffsets()
	{
		if(!dirty_offsets) return;
		long pos = 0;
		CachePage page = first_page;
		while(page != null)
		{
			page.setOffset(pos);
			pos += page.getSize();
			page = page.getNextPage();
		}
		dirty_offsets = false;
	}
	
	private CachePage getPage(long offset)
	{
		updateDirtyOffsets();
		
		CachePage page = root_page;
		while(page != null)
		{
			if(offset < page.getOffset())
			{
				page = page.getLeftChild();
				continue;
			}
			if(offset >= (page.getOffset() + page.getSize()))
			{
				page = page.getRightChild();
				continue;
			}
			return page;
		}
		
		return null;
	}
	
	private CachePage getLoadedPage(long offset) throws IOException
	{
		CachePage page = getPage(offset);
		
		if(!page.dataLoaded())
		{
			//Free oldest page...
			if(freeQueue.size() >= page_count)
			{
				CachePage oldpage = freeQueue.pop();
				oldpage.freeMemory();
			}
			page.loadData();
		}
		else
		{
			//Put at back of queue...
			freeQueue.removeFirstOccurrence(page);
		}
		freeQueue.add(page);
		
		return page;
	}
	
	private CachePage newPageBefore(CachePage target)
	{
		//New page
		String temppath = tempdir + File.separator + 
				"cachebuff_page_" + Long.toHexString(OffsetDateTime.now().toEpochSecond()) +
				"_" + Long.toHexString(rng.nextLong()) + ".tmp";
		CachePage page = new CachePage(temppath, 0, target.offset_value, 0);
		page.setWriteFlag();
		
		//Set pointers
		page.next = target;
		page.previous = target.previous;
		target.previous = page;
		if(first_page == target) first_page = page;
		
		page.child_l = target.child_l;
		target.child_l = page;
		page.parent = target;
		if(page.child_l != null) page.child_l.parent = page;
		
		//Note temp path
		temp_files.add(temppath);
		
		//Free old page if needed
		if(freeQueue.size() >= page_count)
		{
			CachePage old = freeQueue.pop();
			try{old.freeMemory();}
			catch(Exception e){e.printStackTrace();}
		}
		freeQueue.add(page);
		
		return page;
	}
	
	private void addPageToEnd()
	{
		//New page
		String temppath = tempdir + File.separator + 
				"cachebuff_page_" + Long.toHexString(OffsetDateTime.now().toEpochSecond()) +
				"_" + Long.toHexString(rng.nextLong()) + ".tmp";
		CachePage page = new CachePage(temppath, 0, current_file_size, 0);
		page.setWriteFlag();
		
		//Set pointers
		page.previous = last_page;
		CachePage p = last_page;
		CachePage rc = p.getRightChild();
		while(rc != null)
		{
			p = rc;
			rc = p.getRightChild();
		}
		page.parent = p;
		p.child_r = page;
		
		last_page = page;
		if(first_page == null) first_page = last_page;
		
		//Note temp path
		temp_files.add(temppath);
		
		//Restructure tree if needed
		p_add_count++;
		if(p_add_count >= TREE_RESTRUCT_PAGES) rebuildTree();
		
		//Free old page if needed
		if(freeQueue.size() >= page_count)
		{
			CachePage old = freeQueue.pop();
			try{old.freeMemory();}
			catch(Exception e){e.printStackTrace();}
		}
		freeQueue.add(page);
	}
	
	private CachePage getEndPageForWrite() throws IOException
	{
		if(last_page == null || (last_page.getSize() >= page_size << 1))
		{
			addPageToEnd();
		}
		else
		{
			if(freeQueue.peekLast() != last_page)
			{
				freeQueue.remove(last_page);
				freeQueue.add(last_page);	
			}
		}
		
		return last_page;
	}
	
	private CachePage getWritablePage(long offset) throws IOException
	{
		updateDirtyOffsets();
		
		//Get page matching offset
		CachePage page = null;
		if(last_written_page.onPage(offset)) page = last_written_page;
		else page = getLoadedPage(offset);
		
		//See if it has space. If not, insert new page.
		if(page.getSize() >= (page_size << 1))
		{
			long pg_off = offset - page.getOffset();
			CachePage n = page.split(pg_off);
			page = newPageBefore(n);
		}
		else
		{
			//Put at back of freequeue
			freeQueue.remove(page);
			freeQueue.add(page);
		}
		
		//See if page has ever been written to.
		//If not, need to change source path.
		if(!page.writeFlag())
		{
			String temppath = tempdir + File.separator + 
					"cachebuff_page_" + Long.toHexString(OffsetDateTime.now().toEpochSecond()) +
					"_" + Long.toHexString(rng.nextLong()) + ".tmp";
			page.src_path = temppath;
			page.src_offset = 0;
			page.setWriteFlag();
			page.setDirty();
		}
		
		last_written_page = page;
		dirty_offsets = true;
		return page;
	}
	
	private CachePage getNewWritablePage(long offset) throws IOException
	{
		updateDirtyOffsets();
		
		//Get page matching offset
		CachePage ins = getPage(offset);
		
		long pg_off = offset - ins.getOffset();
		if(pg_off > 0) ins = ins.split(pg_off);
		
		CachePage page = newPageBefore(ins);
		
		//p_add_count++;
		//if(p_add_count >= TREE_RESTRUCT_PAGES) rebuildTree();
		
		return page;
	}
	
	/* ----- BASIC GETTERS ----- */
	
	public long getFileSize()
	{
		return current_file_size;
	}
  
	public long getBaseCapacity()
	{
		long maxpsz = (long)page_size << 1;
		long maxpgs = (long)page_count;
		
		return maxpgs * maxpsz;
	}
	
	public byte getByte(int position)
	{
		return getByte(Integer.toUnsignedLong(position));
	}
  
	public byte getByte(long position)
	{
		if(last_read_page != null && last_read_page.onPage(position))
		{
			long pos = position - last_read_page.getOffset();
			return last_read_page.getDataBuffer().getByte(pos);
		}
		else
		{
			try
			{
				last_read_page = getLoadedPage(position);
				long pos = position - last_read_page.getOffset();
				return last_read_page.getDataBuffer().getByte(pos);
			}
			catch(IOException e)
			{
				e.printStackTrace();
				throw new IllegalStateException();
			}
		}
	}

  	public long getMinimumMemoryUsage()
  	{
  		long est = 4+4+8+1+(8*6);
  		
  		CachePage page = first_page;
  		while(page != null)
  		{
  			est += page.estimateMemorySize();
  			page = page.getNextPage();
  		}
  		
  		return est;
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
  
	public void addToFile(byte i8)
	{  
		if(readOnly()) throw new UnsupportedOperationException();
		
		CachePage page = null;
		try{page = getEndPageForWrite();}
		catch(IOException e){e.printStackTrace(); return;}
		
		page.getDataBufferForWrite().addToFile(i8);
		page.incrementSize();
 	}
  
	public void addToFile(byte i8, int position)
	{
		addToFile(i8, Integer.toUnsignedLong(position));
	}
  
	public void addToFile(byte i8, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		if(position == current_file_size)
		{
			addToFile(i8);
			return;
		}
		
		CachePage page = null;
		try{page = getWritablePage(position);}
		catch(IOException e){e.printStackTrace(); return;}
		
		long p_start = page.getOffset();
		long p_off = position - p_start;
		
		FileBuffer dat = page.getDataBufferForWrite();
		dat.addToFile(i8, p_off);
		page.incrementSize();
		
		super.shiftReferencesAfter(position, 1);
	}
  
	public void addToFile(short i16)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		CachePage page = null;
		try{page = getEndPageForWrite();}
		catch(IOException e){e.printStackTrace(); return;}
		
		page.getDataBufferForWrite().addToFile(i16);
		page.incrementSize(2);
	}	
  
	public void addToFile(short i16, int position)
	{
		addToFile(i16, Integer.toUnsignedLong(position));
	}

	public void addToFile(short i16, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		if(position == current_file_size)
		{
			addToFile(i16);
			return;
		}
		
		CachePage page = null;
		try{page = getWritablePage(position);}
		catch(IOException e){e.printStackTrace(); return;}
		
		long p_start = page.getOffset();
		long p_off = position - p_start;
		
		FileBuffer dat = page.getDataBufferForWrite();
		dat.addToFile(i16, p_off);
		page.incrementSize(2);
		
		super.shiftReferencesAfter(position, 2);
	}
  
	public void addToFile(int i32)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		CachePage page = null;
		try{page = getEndPageForWrite();}
		catch(IOException e){e.printStackTrace(); return;}
		
		page.getDataBufferForWrite().addToFile(i32);
		page.incrementSize(4); 
	}
  
	public void addToFile(int i32, int position)
	{
		addToFile(i32, Integer.toUnsignedLong(position));
	}

	public void addToFile(int i32, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		if(position == current_file_size)
		{
			addToFile(i32);
			return;
		}
		
		CachePage page = null;
		try{page = getWritablePage(position);}
		catch(IOException e){e.printStackTrace(); return;}
		
		long p_start = page.getOffset();
		long p_off = position - p_start;
		
		FileBuffer dat = page.getDataBufferForWrite();
		dat.addToFile(i32, p_off);
		page.incrementSize(4);
		
		super.shiftReferencesAfter(position, 4);
	}
  
	public void addToFile(long i64)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		CachePage page = null;
		try{page = getEndPageForWrite();}
		catch(IOException e){e.printStackTrace(); return;}
		
		page.getDataBufferForWrite().addToFile(i64);
		page.incrementSize(8); 
	}

	public void addToFile(long i64, int position)
	{
		addToFile(i64, Integer.toUnsignedLong(position));
	}
  
	public void addToFile(long i64, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		if(position == current_file_size)
		{
			addToFile(i64);
			return;
		}
		
		CachePage page = null;
		try{page = getWritablePage(position);}
		catch(IOException e){e.printStackTrace(); return;}
		
		long p_start = page.getOffset();
		long p_off = position - p_start;
		
		FileBuffer dat = page.getDataBufferForWrite();
		dat.addToFile(i64, p_off);
		page.incrementSize(8);
		
		super.shiftReferencesAfter(position, 8);
	}
  
	public void add24ToFile(int i24)
	{	 
		if(readOnly()) throw new UnsupportedOperationException();
		
		CachePage page = null;
		try{page = getEndPageForWrite();}
		catch(IOException e){e.printStackTrace(); return;}
		
		page.getDataBufferForWrite().add24ToFile(i24);
		page.incrementSize(2); 
	}
  
	public void add24ToFile(int i24, int position)
	{
		add24ToFile(i24, Integer.toUnsignedLong(position));
	}
  
	public void add24ToFile(int i24, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		if(position == current_file_size)
		{
			add24ToFile(i24);
			return;
		}
		
		CachePage page = null;
		try{page = getWritablePage(position);}
		catch(IOException e){e.printStackTrace(); return;}
		
		long p_start = page.getOffset();
		long p_off = position - p_start;
		
		FileBuffer dat = page.getDataBufferForWrite();
		dat.add24ToFile(i24, p_off);
		page.incrementSize(3);
		
		super.shiftReferencesAfter(position, 3);
	}
  
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
		if(readOnly()) throw new UnsupportedOperationException();
		
		//Determine how many pages need to be added.
		long fsz = edPos - stPos;
		int add_pages = (int)(fsz/page_size);
		boolean partial_pg = (fsz % page_size) != 0;
		
		long st = stPos;
		long ed = st + page_size;
		for(int i = 0; i < add_pages; i++)
		{
			addPageToEnd();
			CachePage page = last_page;
			page.copyFromBuffer(addition, st, ed);
			st = ed;
			ed = st + page_size;
		}
		
		if(partial_pg)
		{
			ed = edPos;
			addPageToEnd();
			CachePage page = last_page;
			page.copyFromBuffer(addition, st, ed);
		}
		
	}

	public void addToFile(FileBuffer addition, int insertPos, int stPos, int edPos)
	{
		addToFile(addition, Integer.toUnsignedLong(insertPos), Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
	}
  
	public void addToFile(FileBuffer addition, long insertPos, long stPos, long edPos)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		long fsz = edPos - stPos;
		int add_pages = (int)(fsz/page_size);
		int mod = (int)(fsz % page_size);
		
		try
		{
			CachePage last = null;
		
			long ed = edPos;
			if(mod != 0)
			{
				last = getNewWritablePage(insertPos);
				long st = edPos - mod;
				last.copyFromBuffer(addition, st, edPos);
				ed = st;
			}
		
			long st = ed - page_size;
			for(int i = add_pages-1; i >= 0; i--)
			{
				if(last == null) last = getNewWritablePage(insertPos);
				else last = newPageBefore(last);
				last.copyFromBuffer(addition, st, edPos);
				ed = st;
				st = ed - page_size;
			}
		
			updateDirtyOffsets();
			if(add_pages > 2) rebuildTree();
			
			super.shiftReferencesAfter(insertPos, (int)(edPos - stPos));
		}
		catch(Exception x)
		{
			x.printStackTrace();
		}
		
	}
 	
	public void addToFile(int value, BinFieldSize size)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		switch(size)
		{
		case BYTE: addToFile((byte)value); break;
		case DWORD: addToFile((short)value); break;
		case QWORD: addToFile((long)value); break;
		case WORD: addToFile((int)value); break;
		default:
			break;
		}
	}
	
	/* ----- CONTENT RETRIEVAL ----- */
	  
	public byte[] getBytes()
	{
		if(current_file_size <= 0x7FFFFFFF) return getBytes(0, current_file_size);
		throw new IndexOutOfBoundsException("CachedBuffer is too large to store in byte array!");
	}
	
	public byte[] getBytes(long stOff, long edOff)
	{
		long sz = edOff - stOff;
		if(sz > 0x7FFFFFFFL) throw new IndexOutOfBoundsException("Cannot have byte array of > 2GB!");
		
		byte[] arr = new byte[(int)sz];
		long mySize = this.getFileSize();
		
		try{
			CachePage page = getLoadedPage(stOff);
			if(page == null){
				throw new IndexOutOfBoundsException("Page could not be found for offset 0x" + Long.toHexString(stOff));
			}
			if(page.getDataBuffer() == null) page.loadToCache();

			long ppos = stOff - page.offset_value;
			
			for(int i = 0; i < arr.length; i++){
				//System.err.println("Page -- start offset = 0x" + Long.toHexString(page.src_offset));
				arr[i] = page.getDataBuffer().getByte(ppos++);
				if(ppos >= page.size){
					//Check to make sure this isn't the end of the buffer...
					if((stOff + i + 1) >= mySize) break;
					ppos = 0;
					page = page.getNextPage();
					//We're getting null pages here. Why?
					if(page == null){
						System.err.println("Premature linked list end?");
						System.err.println("stOff = 0x" + Long.toHexString(stOff));
						System.err.println("edOff = 0x" + Long.toHexString(edOff));
						System.err.println("fileSize = 0x" + Long.toHexString(this.getFileSize()));
						System.err.println("Position = 0x" + Long.toHexString(stOff + i + 1));
					}
					
					if(page.getDataBuffer() == null) page.loadToCache();
				}
			}
			
		}
		catch(IOException x){
			x.printStackTrace();
			return null;
		}
		
		return arr;
	}

	/* ----- DELETION ----- */
	  
	public void deleteFromFile(int stOff)
	{
		deleteFromFile(Integer.toUnsignedLong(stOff));
	}

	public void deleteFromFile(long stOff)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		if(stOff == 0)
		{
			//Clear whole thing
			first_page = null;
			last_page = null;
			root_page = null;
			p_add_count = 0;
			current_file_size = 0;
			dirty_offsets = false;
			try{dispose();}
			catch(IOException e){e.printStackTrace(); throw new CacheDiskAccessException("deleteFromFile | File cache modification failed!");}
		}
		
		try
		{
			CachePage page = getWritablePage(stOff);
			
			//Toss everything after stOff in this page.
			long pg_off = stOff - page.getOffset();
			if(pg_off > 0)
			{
				FileBuffer dat = page.getDataBufferForWrite();	
				dat.deleteFromFile(pg_off);
				page.decrementSize((int)pg_off);
			}
			else
			{
				page = page.getPreviousPage();
			}
			last_page = page;
			
			//Toss all pages after
			CachePage tossHead = page.getNextPage();
			page.next = null;
			CachePage p = tossHead;
			while(p != null)
			{
				p.freeMemory();
				freeQueue.remove(p);
				p = p.getNextPage();
			}
			
			//Restructure
			rebuildTree();
		}
		catch(IOException x)
		{
			x.printStackTrace();
			throw new CacheDiskAccessException("deleteFromFile | File cache modification failed!");
		}
		
	}
  
	public void deleteFromFile(int stOff, int edOff)
	{
		deleteFromFile(Integer.toUnsignedLong(stOff), Integer.toUnsignedLong(edOff));
	}

	public void deleteFromFile(long stOff, long edOff)
	{
		if(edOff >= getFileSize()){deleteFromFile(stOff); return;}
		if(readOnly()) throw new UnsupportedOperationException();
		
		try
		{
			CachePage page = getWritablePage(stOff);
			CachePage head = page.getNextPage();
			long fpos = stOff;
			long ppos = stOff - page.getOffset();
			
			while(fpos < edOff)
			{
				ppos = fpos - page.getOffset();
				boolean endInPage = (page.onPage(edOff));
				if(ppos != 0)
				{
					//See if edpos is within this page.
					if(endInPage)
					{
						FileBuffer dat = page.getDataBufferForWrite();	
						long ped = edOff - page.getOffset();
						int amt = (int)(ped - ppos);
						dat.deleteFromFile(ppos, (edOff-page.getOffset()));
						page.decrementSize((int)amt);
						fpos += amt;
						break;
					}
					else
					{
						FileBuffer dat = page.getDataBufferForWrite();	
						dat.deleteFromFile(ppos);
						page.decrementSize((int)ppos);
					}
				}
				else
				{
					//Either remove first part of page, or whole page.
					if(endInPage)
					{
						if(!page.dataLoaded())
						{
							if(freeQueue.size() >= page_count)
							{
								CachePage old = freeQueue.pop();
								old.freeMemory();
							}
							page.loadData();
						}
						else
						{
							freeQueue.remove(page);
							freeQueue.add(page);
						}
						
						FileBuffer dat = page.getDataBufferForWrite();	
						long ped = edOff - page.getOffset();
						int amt = (int)ped;
						dat.deleteFromFile(0, (edOff-page.getOffset()));
						page.decrementSize((int)amt);
						fpos += amt;
						break;
					}
					else
					{
						//Remove whole page.
						if(page == last_page) last_page = page.previous;
						if(page == first_page) first_page = head;
						head.previous = page.previous;
						if(page.previous != null) page.previous.next = head;
						page.previous = null;
						page.next = null;
						page.freeMemory();
						freeQueue.remove(page);
						fpos += page.size;
					}
				}
				page = head;
				head = page.getNextPage();
			}
			
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new CacheDiskAccessException("deleteFromFile | File cache modification failed!");
		}
		
		updateDirtyOffsets();
		rebuildTree();
	}
  
  /* ----- REPLACEMENT ----- */
  
	public boolean replaceByte(byte b, int position)
	{
		return replaceByte(b, Integer.toUnsignedLong(position));
	}
  
	public boolean replaceByte(byte b, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		try
		{
			CachePage page = getWritablePage(position);
			long pg_off = position - page.getOffset();

			FileBuffer dat = page.getDataBufferForWrite();
			dat.replaceByte(b, pg_off);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new CacheDiskAccessException("replaceByte | File cache modification failed!");
		}
		
		return true;
	}
  
	public boolean replaceShort(short s, int position)
	{
		return replaceShort(s, Integer.toUnsignedLong(position));
	}
  
	public boolean replaceShort(short s, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		try
		{
			CachePage page = getWritablePage(position);
			long pg_off = position - page.getOffset();

			FileBuffer dat = page.getDataBufferForWrite();
			if(page.onPage(position+1)) dat.replaceShort(s, pg_off);
			else
			{
				byte[] bytes = FileBuffer.numToByStr(s);
				long pos = position;
				if(this.isBigEndian())
				{
					for(int i = 0; i < 2; i++) {replaceByte(bytes[i], pos); pos++;}
				}
				else
				{
					for(int i = 1; i >= 0; i--) {replaceByte(bytes[i], pos); pos++;}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new CacheDiskAccessException("replaceShort | File cache modification failed!");
		}
		
		return true;
	}
  
	public boolean replaceShortish(int s, int position)
	{
		return replaceShortish(s, Integer.toUnsignedLong(position));
	}
	
	public boolean replaceShortish(int s, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		try
		{
			CachePage page = getWritablePage(position);
			long pg_off = position - page.getOffset();

			FileBuffer dat = page.getDataBufferForWrite();
			if(page.onPage(position+2)) dat.replaceShortish(s, pg_off);
			else
			{
				byte[] bytes = FileBuffer.numToByStr(s);
				long pos = position;
				if(this.isBigEndian())
				{
					for(int i = 1; i < 4; i++) {replaceByte(bytes[i], pos); pos++;}
				}
				else
				{
					for(int i = 2; i >= 0; i--) {replaceByte(bytes[i], pos); pos++;}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new CacheDiskAccessException("replaceShortish | File cache modification failed!");
		}
		
		return true;
	}
	
	public boolean replaceInt(int i, int position)
	{
		return replaceInt(i, Integer.toUnsignedLong(position));
	}
  
	public boolean replaceInt(int i, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		try
		{
			CachePage page = getWritablePage(position);
			long pg_off = position - page.getOffset();

			FileBuffer dat = page.getDataBufferForWrite();
			if(page.onPage(position+3)) dat.replaceInt(i, pg_off);
			else
			{
				byte[] bytes = FileBuffer.numToByStr(i);
				long pos = position;
				if(this.isBigEndian())
				{
					for(int j = 0; j < 4; j++) {replaceByte(bytes[j], pos); pos++;}
				}
				else
				{
					for(int j = 3; j >= 0; j--) {replaceByte(bytes[j], pos); pos++;}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new CacheDiskAccessException("replaceInt | File cache modification failed!");
		}
		
		return true;
	}
  
	public boolean replaceLong(long l, int position)
	{
		return replaceLong(l, Integer.toUnsignedLong(position));
	}
  
	public boolean replaceLong(long l, long position)
	{
		if(readOnly()) throw new UnsupportedOperationException();
		
		try
		{
			CachePage page = getWritablePage(position);
			long pg_off = position - page.getOffset();

			FileBuffer dat = page.getDataBufferForWrite();
			if(page.onPage(position+7)) dat.replaceLong(l, pg_off);
			else
			{
				byte[] bytes = FileBuffer.numToByStr(l);
				long pos = position;
				if(this.isBigEndian())
				{
					for(int j = 0; j < 8; j++) {replaceByte(bytes[j], pos); pos++;}
				}
				else
				{
					for(int j = 7; j >= 0; j--) {replaceByte(bytes[j], pos); pos++;}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new CacheDiskAccessException("replaceLong | File cache modification failed!");
		}
		
		return true;
	}
  
	/* ----- WRITING TO DISK ----- */

  	public void writeFile(String path, long stPos, long edPos) throws IOException
  	{
  		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
  		writeToStream(bos, stPos, edPos);
  		bos.close();
  	}

  	public void appendToFile(String path, long stPos, long edPos) throws IOException, NoSuchFileException
  	{
  		long cpos = stPos;
  		CachePage page = getLoadedPage(stPos);
  		Path mypath = Paths.get(path);
  		
  		while(cpos < edPos)
  		{
  			long st = cpos - page.getOffset();
  	  		long ed = page.size;
  			if(page.onPage(edPos))
  			{
  				ed = edPos - page.getOffset();
  			}
  			
  			page.appendToFile(mypath, st, ed);
  			
  			page = page.getNextPage();
  			cpos = page.getOffset() + ed;
  		}
  	}
  	
  	public long writeToStream(OutputStream out) throws IOException
  	{
  		return writeToStream(out, 0, current_file_size);
  	}
  	
  	public long writeToStream(OutputStream out, long stPos, long edPos) throws IOException
  	{
  		long w = 0;
  		long cpos = stPos;
  		CachePage page = getLoadedPage(stPos);
  		//int pidx = 0; //DEBUG
  		
  		while(cpos < edPos)
  		{
  			//System.err.println("Page " + pidx + ": Offset = 0x" + Long.toHexString(page.getOffset()));
  			//System.err.println("\tcpos = 0x" + Long.toHexString(cpos));
  			long st = cpos - page.getOffset();
  	  		long ed = page.size;
  			if(page.onPage(edPos))
  			{
  				ed = edPos - page.getOffset();
  			}
  			
  			page.writeToStream(out, st, ed);
  			w += ed-st;
  			
  			cpos = page.getOffset() + ed;
  			page = page.getNextPage();
  			//pidx++;
  		}
  		
  		return w;
  	}
  	
  	/* ----- STRING SEARCHING ----- */

  	/* ----- STATUS CHECKERS ----- */
    
  	public boolean isEmpty()
  	{
  		return (current_file_size == 0);
  	}
  
  	public boolean offsetValid(int off)
  	{
  		return offsetValid(Integer.toUnsignedLong(off));
  	}
  
  	public boolean offsetValid(long off)
  	{
  		if(off < 0) return false;
  		if(off >= current_file_size) return false;
  		
  		return true;
  	}
  
  	public boolean checkOffsetPair(long stOff, long edOff)
  	{
  		if(!offsetValid(stOff)) return false;
  		if(!offsetValid(edOff-1)) return false;
  		return true;
  	}
  	
  	/* ----- STRING ADDITON/ RETRIEVAL ----- */
    
  	/* ----- CONVERSION ----- */
    
  	public ByteBuffer toByteBuffer()
  	{
  		if(current_file_size > 0x7FFFFFFF) throw new IndexOutOfBoundsException("Cannot store >2GB in byte buffer!");
  		return toByteBuffer(0, current_file_size);
  	}
  
  	public ByteBuffer toByteBuffer(int stPos, int edPos)
  	{
  		return toByteBuffer(Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
  	}
  
  	public ByteBuffer toByteBuffer(long stPos, long edPos)
  	{
  		long sz = edPos - stPos;
  		if(sz > 0x7FFFFFFF) throw new IndexOutOfBoundsException("Cannot store >2GB in byte buffer!");
  		
  		ByteBuffer bb = ByteBuffer.allocate((int)sz);
  		
  		try
  		{
  			long cpos = stPos;
  			CachePage page = getLoadedPage(stPos);

  			while(cpos < edPos)
  			{
  				long st = cpos - page.getOffset();
  				long ed = page.size;
  				if(page.onPage(edPos)) ed = edPos - page.getOffset();
  			
  				bb.put(page.getDataBuffer().getBytes(st, ed));
  			
  				page = page.getNextPage();
  				if(!page.dataLoaded()) page.loadToCache();
  				cpos = page.getOffset() + ed;
  			}
  		}
  		catch(IOException x)
  		{
  			x.printStackTrace();
  			return null;
  		}
  		
  		bb.rewind();
  		return bb;
  	}
  
  	public FileBuffer createCopy(int stPos, int edPos) throws IOException
  	{
  		return createCopy(Integer.toUnsignedLong(stPos), Integer.toUnsignedLong(edPos));
  	}
  
  	public FileBuffer createCopy(long stPos, long edPos) throws IOException
  	{
  		final long COPY_SIZE_THRESHOLD = 0x08000000; //128MB
  		
  		long sz = edPos - stPos;
  		if(sz < COPY_SIZE_THRESHOLD)
  		{
  			//Just make a regular FileBuffer
  			FileBuffer copy = new FileBuffer((int)sz, this.isBigEndian());
  			
  			long cpos = stPos;
  			CachePage page = getLoadedPage(stPos);

  			while(cpos < edPos)
  			{
  				long st = cpos - page.getOffset();
  				long ed = page.size;
  				if(page.onPage(edPos)) ed = edPos - page.getOffset();
  			
  				copy.addToFile(page.getDataBuffer().createReadOnlyCopy(st, ed));
  			
  				page = page.getNextPage();
  				if(!page.dataLoaded()) page.loadToCache();
  				cpos = page.getOffset() + ed;
  			}
  		}
  		else
  		{
  			//Make a new CacheFileBuffer
  			updateDirtyOffsets();
  			CacheFileBuffer copy = new CacheFileBuffer(page_size, page_count, !readOnly());
  			
  			CachePage p_page = null;
  			CachePage page = getPage(stPos);
  			long cpos = stPos;
  			while(cpos < edPos)
  			{
  				long st = cpos - page.getOffset();
  				int psz = page.size;
  				if(page.onPage(edPos))
  				{
  					long ed = edPos - page.getOffset();
  					psz = (int)(ed - st);
  				}
  				CachePage pcopy = page.createWritableCopy(st, psz);
  				
  				//Do linked list thing
  				if(p_page == null) copy.first_page = pcopy;
  				else
  				{
  					pcopy.previous = p_page;
  					p_page.next = pcopy;
  				}
  				
  				if(pcopy.writeFlag()) copy.temp_files.add(pcopy.getSourcePath());
  				
  				//Advance
  				cpos += psz;
  				p_page = pcopy;
  			}
  			copy.last_page = p_page;
  			
  			copy.rebuildTree();
  			return copy;
  		}
  		
  		
  		return null;
  	}
  
  	 /* ----- FILE STATISTICS/ INFO ----- */
  	
  	public String typeString()
  	{
  		return "CacheFileBuffer";
  	}
    
  	/* ----- Cleanup ----- */
  	
  	public void dispose() throws IOException
  	{
  		//Deletes all temp files and clears memory
  		
  		while(!freeQueue.isEmpty())
  		{
  			CachePage page = freeQueue.pop();
  			page.freeMemory();
  		}
  		
  		for(String tpath : temp_files) Files.deleteIfExists(Paths.get(tpath));
  		temp_files.clear();
  		
  		last_read_page = null;
  		last_written_page = null;
  		first_page = null;
  		last_page = null;
  		root_page = null;
  		
  		p_add_count = 0;
  		current_file_size = 0;
  		dirty_offsets = false;
  	}
  	
  	/* ----- Exception ----- */
  	
  	/**
  	 * An exception to throw specifically when there is an issue reading or
  	 * writing a CacheFileBuffer page.
  	 * @author Blythe Hospelhorn
  	 * @version 1.1.0
  	 */
  	public static class CacheDiskAccessException extends RuntimeException
  	{

		private static final long serialVersionUID = 3156914180197673406L;
  		
		private String msg;
		
		/**
		 * Construct a CacheDiskAccessException with the specified error message.
		 * @param message Error message to pass along.
		 */
		public CacheDiskAccessException(String message)
		{
			msg = message;
		}
		
		/**
		 * Retrieve the error message that was set when the exception was thrown.
		 */
		public String getErrorMessage()
		{
			return msg;
		}
		
  	}
  	
}
