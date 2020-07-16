package waffleoRai_Files;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import waffleoRai_Compression.definitions.AbstractCompDef;
import waffleoRai_Compression.definitions.CompressionInfoNode;
import waffleoRai_Utils.DirectoryNode;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBufferStreamer;
import waffleoRai_Utils.FileNode;
import waffleoRai_Utils.MultiFileBuffer;

public class FragFileNode extends FileNode{

	/*----- Instance Variables -----*/
	
	private List<long[]> block_locs;
	
	/*----- Construction -----*/
	
	public FragFileNode(DirectoryNode parent, String name) {
		super(parent, name);
		super.setOffset(0); super.setLength(0);
		block_locs = new LinkedList<long[]>();
	}
	
	/* --- Location Management --- */
	
	public void addBlock(long offset, long length){
		if(block_locs.isEmpty()) super.setOffset(offset);
		block_locs.add(new long[]{offset, length});
		super.setLength(super.getLength() + length);
	}
	
	public void clearBlocks(){
		block_locs.clear();
		super.setOffset(0); super.setLength(0);
	}
	
	public void setOffset(long off){}
	public void setLength(long len){}
	
	public List<long[]> getBlocks(){
		List<long[]> list = new ArrayList<long[]>(block_locs.size()+1);
		list.addAll(block_locs);
		return list;
	}
	
	/* --- Load --- */
	
	public FileBuffer loadData(long stpos, long len) throws IOException{
		String path = getSourcePath();
		
		if(super.getCompressionChain() != null)
		{
			//System.err.println("Non-null compression chain!");
			for(CompressionInfoNode comp : super.getCompressionChain())
			{
				FileBuffer file = null;
				
				long stoff = comp.getStartOffset();
				if(comp.getLength() > 0)
				{
					long edoff = stoff + comp.getLength();
					file = FileBuffer.createBuffer(path, stoff, edoff);	
					//System.err.println("Source compressed region: 0x" + Long.toHexString(stoff) + " - 0x" + Long.toHexString(edoff));
				}
				else file = FileBuffer.createBuffer(path, stoff);
				FileBufferStreamer streamer = new FileBufferStreamer(file);
				AbstractCompDef def = comp.getDefinition();
				if(def == null) return null;
				path = def.decompressToDiskBuffer(streamer);
				//System.err.println("Decompressed to: " + path);
				stoff = this.getOffset();
				//long edoff = stoff + this.getLength();
			}
		}
		
		//Chain together the blocks...
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
			FileBuffer blockdat = FileBuffer.createBuffer(path, start, end);
			file.addToFile(blockdat);
			
			//Advance position
			cpos += block[1];
		}
		
		return file;
	}

	/* --- Other --- */
	
	protected void copyDataTo(FragFileNode copy){
		super.copyDataTo(copy);
		
		copy.block_locs = getBlocks();
	}
	
	public FileNode copy(DirectoryNode parent_copy)
	{
		FileNode copy = new FragFileNode(parent_copy, this.getFileName());
		copyDataTo(copy);
		
		return copy;
	}
	
	public boolean splitNodeAt(long off){
		throw new UnsupportedOperationException(); //Eh I'll do it later
	}
	
	public String getLocationString(){
		StringBuilder sb = new StringBuilder(4096);
		
		if(sourceDataCompressed()){
			for(CompressionInfoNode c : super.getCompressionChain()){
				sb.append("Decomp From: 0x" + Long.toHexString(c.getStartOffset()) + " -> ");
			}
		}
		
		for(long[] block : block_locs){
			String stoff = "0x" + Long.toHexString(block[0]);
			String edoff = "0x" + Long.toHexString(block[0]+block[1]);
			sb.append("[" + stoff + "-" + edoff + "]");
		}
	
		return sb.toString();
	}
	
	/* --- Debug --- */
	
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
