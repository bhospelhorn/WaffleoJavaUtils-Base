package waffleoRai_Files.tree;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import waffleoRai_Utils.CacheFileBuffer;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.MultiFileBuffer;

/*
 * UPDATES
 * 
 * 2020.08.29 | 2.0.0
 * 	Documentation
 * 	Update to FileNode 3.0.0 compatibility
 * 
 * 2020.09.02 | 2.1.0
 * 	Added debugging method printDetailedTo()
 * 
 * 2020.09.07 | 2.1.1
 * 	Wrap pieces in structure that marks local address for faster lookup
 * 
 */

/**
 * A <code>FileNode</code> referencing a chunk of data made up of multiple 
 * non-sequential chunks of data from different sources. This may be used
 * to represent patched virtual files.
 * @author Blythe Hospelhorn
 * @version 2.1.1
 * @since September 7, 2020
 */
public class PatchworkFileNode extends FileNode{
	
	public static final int CACHEBUFF_PGSIZE = 0x1000;
	public static final int CACHEBUFF_PGNUM = 256;
	
	public static final String CLASSVER = "2.1.1";
	
	/*----- Instance Variables -----*/
	
	private boolean complex_mode; //Uses the nodes as direct sources... (vs. just path/offset data)
	private ArrayList<Piece> pieces;
	
	/*----- Structures -----*/
	
	private static class Piece{
		
		public long st_off; //Local address. For quick lookup.
		
		public FileNode node;
		
		public Piece(long off, FileNode n){
			st_off = off;
			node = n;
		}
	}
	
	/*----- Construction -----*/

	/**
	 * Create a new <code>PatchworkFileNode</code> and initialize it with the
	 * provided parent directory link and node name.
	 * @param parent <code>DirectoryNode</code> to set as the initial parent of
	 * this node. Parameter may be left <code>null</code> and set later.
	 * @param name Node name to initialize node with. May be left <code>null</code>
	 * or empty and set later.
	 */
	public PatchworkFileNode(DirectoryNode parent, String name) {
		super(parent, name);
		pieces = new ArrayList<Piece>();
	}
	
	/**
	 * Create a new <code>PatchworkFileNode</code> and initialize it with the
	 * provided parent directory link and node name.
	 * @param parent <code>DirectoryNode</code> to set as the initial parent of
	 * this node. Parameter may be left <code>null</code> and set later.
	 * @param name Node name to initialize node with. May be left <code>null</code>
	 * or empty and set later.
	 * @param initPieces Number of slots to initially allocate for internal array list.
	 */
	public PatchworkFileNode(DirectoryNode parent, String name, int initPieces) {
		super(parent, name);
		pieces = new ArrayList<Piece>(initPieces);
	}
	
	/* --- Location Management --- */
	
	private int getPieceIndex(long offset){
		//Binary search list to reduce retrieval time
		int pcount = pieces.size();
		int max = pcount;
		int min = 0;
		int idx = max/2;
		
		while(idx >= 0 && idx < pcount){
			//Check this page to see if offset falls within.
			Piece p0 = pieces.get(idx);
			
			if(p0.st_off > offset){
				//Offset is before this page. Go left.
				int space = max-min;
				if(space <= 0) break; //No more nodes to check
				
				max = idx;
				idx = min + (space/2);
				continue;
			}
			
			long edoff = 0L;
			if(idx < (pcount-1)){
				Piece p1 = pieces.get(idx+1);
				edoff = p1.st_off;
			}
			else edoff = super.getLength();
			
			if(offset >= edoff){
				//Offset is after this page. Go right.
				int space = max-min;
				if(space <= 0) break; //No more nodes to check
				
				min = idx+1;
				idx = min + (space/2);
				continue;
			}
			
			//Offset is within this page. Return idx
			return idx;
		}
		
		return -1;
	}
	
	/**
	 * Add a piece to the end of the patchwork virtual file. This piece will
	 * be a basic <code>FileNode</code> sourcing a block of data by disk
	 * location, offset, and length.
	 * @param path Path on local file system to file containing data to reference.
	 * @param offset Offset relative to start of source file where data is located.
	 * @param size Size in bytes of data chunk to reference.
	 */
	public void addBlock(String path, long offset, long size){
		if (super.getSourcePath() == null) super.setSourcePath(path);
		long len = super.getLength();
		super.setLength(len + size);
		
		FileNode dat = new FileNode(null, "");
		dat.setSourcePath(path);
		dat.setOffset(offset);
		dat.setLength(size);
		
		pieces.add(new Piece(len, dat));
	}
	
	/**
	 * Add a piece to the end of the patchwork virtual file.
	 * This piece may be any existing <code>FileNode</code> with all attached
	 * type, container, encryption, and metadata. In complex mode, the node's 
	 * <code>loadData()</code> function will be called when loading data.
	 * @param block Block to add to end of patchwork file.
	 */
	public void addBlock(FileNode block){
		if(block == null) return;
		long len = super.getLength();
		pieces.add(new Piece(len, block));
		super.setLength(len + block.getLength());
	}
	
	/**
	 * Clear all block definitions from this virtual file container.
	 */
	public void clearBlocks(){
		pieces.clear();
		super.setLength(0);
	}
	
	public void setOffset(long off){}
	public void setLength(long len){}
	
	/**
	 * Get all blocks the make up this virtual file. Returned list is a copy,
	 * but <code>FileNode</code> blocks are references.
	 * @return <code>ArrayList</code> containing blocks that make up patchwork file.
	 */
	public ArrayList<FileNode> getBlocks(){
		ArrayList<FileNode> copy = new ArrayList<FileNode>(pieces.size()+1);
		for(Piece p : pieces) copy.add(p.node);
		return copy;
	}
	
	public Set<String> getAllSourcePaths(){
		Set<String> set = new HashSet<String>();
		for(Piece piece : pieces) set.add(piece.node.getSourcePath());
		return set;
	}
	
	/**
	 * Get whether this patchwork virtual file definition is set for complex mode
	 * data loading. When in complex mode, component block <code>loadData()</code>
	 * methods are called when building composite file. In simple mode, only the
	 * path and offset data are used.
	 * @return True if this patchwork node is set to complex mode, false if in simple mode.
	 */
	public boolean complexMode(){return complex_mode;}
	
	/**
	 * Set whether this patchwork virtual file definition is set for complex mode
	 * data loading. When in complex mode, component block <code>loadData()</code>
	 * methods are called when building composite file. In simple mode, only the
	 * path and offset data are used.
	 * @param b True to set node to complex mode for data loading. False to set
	 * to simple mode.
	 */
	public void setComplexMode(boolean b){complex_mode = b;}
	
	/* --- Load --- */
	
	protected FileBuffer loadDirect(long stpos, long len, boolean forceCache, boolean decrypt) throws IOException{
		
		if(len >= FileBuffer.DEFO_SIZE_THRESHOLD) forceCache = true;
		
		MultiFileBuffer file = new MultiFileBuffer(pieces.size()+1); //Overkill but null pages shouldn't be that much of a mem sink.
		//Get the index of the start piece using a binary search.
		int idx = getPieceIndex(stpos);
		long remaining = len;
		while(remaining > 0L){
			Piece p = pieces.get(idx++);
			
			//See if start is after start of piece.
			long l = p.node.getLength();
			long st = 0L;
			if(p.st_off < stpos){
				st = stpos-p.st_off;
				l-=st;
			}
			
			//Get end position...
			if(remaining < l) l = remaining;
			long ed = st+l;
			
			//Load
			FileBuffer pdat = null;
			if(complex_mode){
				pdat = p.node.loadData(st, l, forceCache, decrypt);
			}
			else{
				FileNode piece = p.node;
				long start = st + piece.getOffset();
				long end = ed + piece.getOffset();
				if(forceCache) CacheFileBuffer.getReadOnlyCacheBuffer(piece.getSourcePath(), CACHEBUFF_PGSIZE, CACHEBUFF_PGNUM, start, end);
				else pdat = FileBuffer.createBuffer(piece.getSourcePath(), start, end);
			}
			
			//Next
			file.addToFile(pdat);
			remaining -= l;
		}
		
		return file;
	}
	
	public FileBuffer loadData(long stpos, long len, boolean decrypt) throws IOException{
		boolean forceCache = false;
		if(getLength() >= FileBuffer.DEFO_SIZE_THRESHOLD) forceCache = true;
		return this.loadData(stpos, len, forceCache, decrypt);
	}
	
	public FileBuffer loadData(long stpos, long len) throws IOException{
		boolean forceCache = false;
		if(getLength() >= FileBuffer.DEFO_SIZE_THRESHOLD) forceCache = true;
		return this.loadData(stpos, len, forceCache, true);
	}
	
	public FileBuffer loadData() throws IOException{
		boolean forceCache = false;
		if(getLength() >= FileBuffer.DEFO_SIZE_THRESHOLD) forceCache = true;
		return this.loadData(0L, getLength(), forceCache, true);
	}

	/* --- Other --- */
	
	protected void copyDataTo(PatchworkFileNode copy){
		super.copyDataTo(copy);
		
		copy.pieces = new ArrayList<Piece>(pieces.size()+1);
		copy.pieces.addAll(pieces);
		copy.complex_mode = this.complex_mode;
	}
	
	public FileNode copy(DirectoryNode parent_copy)
	{
		FileNode copy = new PatchworkFileNode(parent_copy, this.getFileName());
		copyDataTo(copy);
		
		return copy;
	}

	public boolean splitNodeAt(long off){
		throw new UnsupportedOperationException(); //Eh I'll do it later
	}
	
	public FileNode getSubFile(long stoff, long len){
		
		PatchworkFileNode copy = new PatchworkFileNode(null, getFileName() + "-copy");
		copyDataTo(copy);
		
		//Blocks
		long edoff = stoff + len;
		long cpos = 0L;
		
		copy.clearBlocks();
		for(Piece p : this.pieces){
			FileNode piece = p.node;
			
			//Skip if not in range
			if(cpos >= edoff) break;
			long b_ed = cpos + piece.getLength();
			if(b_ed < stoff){
				cpos += piece.getLength();
				continue;
			}
			
			//Get block-relative start and end...
			long st = 0L;
			long ed = piece.getLength();
			if(stoff > cpos){
				//new file starts after beginning of this chunk...
				st = stoff - cpos;
			}
			if(b_ed > edoff){
				//This chunk goes past end of new file...
				ed = edoff - cpos;
			}
			
			//Save
			copy.addBlock(piece.getSubFile(st, ed-st));
			cpos += piece.getLength();
		}
		
		//Encryregions
		copy.subsetEncryptionRegions(stoff, len);
		
		return copy;
	}
	
	public String getLocationString(){
		StringBuilder sb = new StringBuilder(1024);
		
		/*if(sourceDataCompressed()){
			for(CompressionInfoNode c : super.getCompressionChain()){
				sb.append("Decomp From: 0x" + Long.toHexString(c.getStartOffset()) + " -> ");
			}
		}*/
		
		sb.append("[MULTIFILE_FRAGMENTED]");
	
		return sb.toString();
	}
	
	/* --- Debug --- */
	
	public void printMeToStdErr(int indents)
	{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();

		System.err.print(tabs + "->" + super.getFileName() + " (");
		for(Piece p : pieces){
			FileNode piece = p.node;
			String path = piece.getSourcePath();
			String stoff = "0x" + Long.toHexString(piece.getOffset());
			String edoff = "0x" + Long.toHexString(piece.getOffset() + piece.getLength());
			System.err.print("[" + path + ":" + stoff + "-" + edoff + "]");
		}
		System.err.print(")");
		System.err.println();
	}

	/**
	 * Print the detailed contents and mapping data for this node
	 * to the specified <code>Writer</code>. This can be used
	 * for debugging.
	 * @param out Output target.
	 * @throws IOException If there is an error writing to the output.
	 */
	public void printDetailedTo(Writer out) throws IOException{

		out.write("----------- PatchworkFileNode Instance (class v." + CLASSVER + ") -----------\n");
		out.write("Node Local Name: " + this.getFileName() + "\n");
		out.write("Node Tree Path: " + this.getFullPath() + "\n");
		out.write("Complex Mode: " + this.complex_mode + "\n");
		
		out.write("Patch Pieces ----\n");
		long pos = 0;
		for(Piece p : pieces){
			FileNode piece = p.node;
			out.write("-> " + piece.getFileName() + "\n");
			out.write("\tSource: ");
			if(piece.hasVirtualSource()){
				out.write("<virtual> ");
				FileNode vsrc = piece.getVirtualSource();
				if(vsrc == null) out.write("[NULL]\n");
				else out.write("NODE" + Long.toHexString(vsrc.getGUID()) + "\n");
			}
			else out.write(piece.getSourcePath() + "\n");
			
			out.write("\tSource Location: " + piece.getLocationString() + "\n");
			out.write("\tLocal Location: 0x" + Long.toHexString(pos) + " - 0x" + Long.toHexString(pos+piece.getLength()) + "\n");
			pos+=piece.getLength();
			
			out.write("\tHas Container: " + piece.hasCompression() + "\n");
			out.write("\tHas Encryption: " + piece.hasEncryption() + "\n");
			
			out.write("\tMetadata: ");
			List<String> keys = piece.getMetadataKeys();
			if(keys == null || keys.isEmpty()) out.write("[None]\n");
			else{
				out.write("\n");
				Collections.sort(keys);
				for(String k : keys){
					out.write("\t\t" + k + "\t" + piece.getMetadataValue(k) + "\n");
				}
			}
		}
		
	}
	
}
