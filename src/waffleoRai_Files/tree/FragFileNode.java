package waffleoRai_Files.tree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
 * 2023.11.08 | 2.1.0
 * 	Update to FileNode 3.6.1 compatibility
 * 
 */

/**
 * A <code>FileNode</code> referencing a chunk of data made up of multiple 
 * non-sequential chunks of data from the same source. In other words, a virtual
 * file definition referencing a fragmented file.
 * @author Blythe Hospelhorn
 * @version 2.1.0
 * @since November 8, 2023
 *
 */
public class FragFileNode extends FileNode{

	/*----- Constants -----*/
	
	public static final int CACHEBUFF_PGSIZE = 0x1000;
	public static final int CACHEBUFF_PGNUM = 256;
	
	/*----- Instance Variables -----*/
	
	private List<long[]> block_locs;
	
	/*----- Construction -----*/
	
	/**
	 * Create a new <code>FragFileNode</code> with the provided parent and
	 * node name.
	 * @param parent <code>DirectoryNode</code> to set as the initial parent of
	 * this node. Parameter may be left <code>null</code> and set later.
	 * @param name Node name to initialize node with. May be left <code>null</code>
	 * or empty and set later.
	 */
	public FragFileNode(DirectoryNode parent, String name) {
		super(parent, name);
		super.setOffset(0); super.setLength(0);
		block_locs = new LinkedList<long[]>();
	}
	
	/* --- Location Management --- */
	
	/**
	 * Add a fragment of data to the end of the virtual file definition.
	 * @param offset Offset of data fragment, relative to the source container.
	 * @param length Length of data fragment.
	 */
	public void addBlock(long offset, long length){
		if(block_locs.isEmpty()) super.setOffset(offset);
		block_locs.add(new long[]{offset, length});
		super.setLength(super.getLength() + length);
	}
	
	/**
	 * Clear the fragment definition list.
	 */
	public void clearBlocks(){
		block_locs.clear();
		super.setOffset(0); super.setLength(0);
	}
	
	public void setOffset(long off){}
	public void setLength(long len){}
	
	/**
	 * Get all fragment definitions as a list of offset/length pairs. For each
	 * <code>long[]</code> array in the list, the offset will by <code>ARR[0]</code> and the length will be
	 * <code>ARR[1]</code>.
	 * @return Linked list of block definitions. List is a copy, but block definition arrays are references.
	 */
	public List<long[]> getBlocks(){
		List<long[]> list = new ArrayList<long[]>(block_locs.size()+1);
		list.addAll(block_locs);
		return list;
	}
	
	/* --- Load --- */
	
	protected FileBuffer loadDirect(long stpos, long len, int options) throws IOException{

		MultiFileBuffer file = new MultiFileBuffer(block_locs.size());
		long cpos = 0;
		long edpos = stpos + len;
		for(long[] block : block_locs){
			
			if(edpos < cpos) break; //End is before start of this block.
			long blockend = cpos + block[1];
			
			//get start
			long blockst = 0;
			if(stpos > cpos){
				//Chunk to grab starts in or after this block...
				if(stpos >= blockend) continue; //Starts after this block
				blockst = stpos - cpos;
			}
			
			//get end
			long blocked = block[1];
			if(edpos < blockend){
				//End is inside block
				blocked = edpos - cpos;
			}
			
			//Copy portion of block to buffer
			long start = blockst + block[0];
			long end = blocked + block[0];
			FileBuffer blockdat = null;
			if(hasVirtualSource()){
				blockdat = getVirtualSource().loadData(start, end-start, options);
			}
			else{
				if((options & FileNode.LOADOP_FORCE_CACHE) != 0) {
					blockdat = CacheFileBuffer.getReadOnlyCacheBuffer(getSourcePath(), CACHEBUFF_PGSIZE, CACHEBUFF_PGNUM, start, end);
				}
				else blockdat = FileBuffer.createBuffer(getSourcePath(), start, end);
			}
			file.addToFile(blockdat);
			
			//Advance position
			cpos += block[1];
		}
		
		return file;
	}
	
	/* --- Other --- */
	
	protected void copyDataTo(FileNode copy){
		super.copyDataTo(copy);
		
		if(copy instanceof FragFileNode){
			FragFileNode fcopy = (FragFileNode)copy;
			fcopy.block_locs = getBlocks();
		}
	}
	
	public FileNode copy(DirectoryNode parent_copy){
		FileNode copy = new FragFileNode(parent_copy, this.getFileName());
		copyDataTo(copy);
		
		return copy;
	}
	
	public boolean splitNodeAt(long off){
		throw new UnsupportedOperationException(); //Eh I'll do it later
	}
	
	public FileNode getSubFile(long stoff, long len){

		FragFileNode copy = new FragFileNode(null, getFileName() + "-copy");
		copyDataTo(copy);
		
		//Blocks
		long edoff = stoff + len;
		long cpos = 0L;
		
		copy.clearBlocks();
		for(long[] block : this.block_locs){
			
			//Skip if not in range
			if(cpos >= edoff) break;
			long b_ed = cpos + block[1];
			if(b_ed < stoff){
				cpos += block[1];
				continue;
			}
			
			//Get block-relative start and end...
			long st = 0L;
			long ed = block[1];
			if(stoff > cpos){
				//new file starts after beginning of this chunk...
				st = stoff - cpos;
			}
			if(b_ed > edoff){
				//This chunk goes past end of new file...
				ed = edoff - cpos;
			}
			
			//Save
			copy.addBlock(st + block[0], ed-st);
			cpos += block[1];
		}
		
		//Encryregions
		copy.subsetEncryptionRegions(stoff, len);
		
		return copy;
	}
	
	public String getLocationString(){
		StringBuilder sb = new StringBuilder(4096);
		
		/*if(sourceDataCompressed()){
			for(CompressionInfoNode c : super.getCompressionChain()){
				sb.append("Decomp From: 0x" + Long.toHexString(c.getStartOffset()) + " -> ");
			}
		}*/
		
		for(long[] block : block_locs){
			String stoff = "0x" + Long.toHexString(block[0]);
			String edoff = "0x" + Long.toHexString(block[0]+block[1]);
			sb.append("[" + stoff + "-" + edoff + "]");
		}
	
		return sb.toString();
	}
	
	/* --- Debug --- */
	
	protected String getTypeString(){return "FragFileNode";}
	
	protected String getOffsetString(){
		String s = "";
		for(long[] block : block_locs){
			s+="[";
			s+="0x" + Long.toHexString(block[0]);
			s+=" - 0x";
			s+= Long.toHexString(block[0] + block[1]);
			s+="]";
		}
		return s;
	}
	
	public void printMeToStdErr(int indents)
	{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();

		System.err.print(tabs + "->" + super.getFileName() + " (");
		for(long[] block : block_locs){
			String stoff = "0x" + Long.toHexString(block[0]);
			String edoff = "0x" + Long.toHexString(block[0]+block[1]);
			System.err.print("[" + stoff + "-" + edoff + "]");
		}
		System.err.print(")");
		System.err.println();
	}
	

}
