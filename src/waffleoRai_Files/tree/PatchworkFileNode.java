package waffleoRai_Files.tree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import waffleoRai_Compression.definitions.CompressionInfoNode;
import waffleoRai_Utils.CacheFileBuffer;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.MultiFileBuffer;

public class PatchworkFileNode extends FileNode{
	
	/*----- Instance Variables -----*/
	
	private boolean complex_mode; //Uses the nodes as direct sources... (vs. just path/offset data)
	private ArrayList<FileNode> pieces;
	
	/*----- Construction -----*/

	public PatchworkFileNode(DirectoryNode parent, String name) {
		super(parent, name);
		pieces = new ArrayList<FileNode>();
	}
	
	public PatchworkFileNode(DirectoryNode parent, String name, int initPieces) {
		super(parent, name);
		pieces = new ArrayList<FileNode>(initPieces);
	}
	
	/* --- Location Management --- */
	
	public void addBlock(String path, long offset, long size){
		if (super.getSourcePath() == null) super.setSourcePath(path);
		super.setLength(super.getLength() + size);
		
		FileNode dat = new FileNode(null, "");
		dat.setSourcePath(path);
		dat.setOffset(offset);
		dat.setLength(size);
		
		pieces.add(dat);
	}
	
	public void addBlock(FileNode block){
		if(block == null) return;
		pieces.add(block);
		super.setLength(super.getLength() + block.getLength());
	}
	
	public void clearBlocks(){
		pieces.clear();
		super.setLength(0);
	}
	
	public void setOffset(long off){}
	public void setLength(long len){}
	
	public ArrayList<FileNode> getBlocks(){
		ArrayList<FileNode> copy = new ArrayList<FileNode>(pieces.size()+1);
		copy.addAll(pieces);
		return copy;
	}
	
	public Set<String> getAllSourcePaths(){
		Set<String> set = new HashSet<String>();
		for(FileNode piece : pieces) set.add(piece.getSourcePath());
		return set;
	}
	
	public boolean complexMode(){return complex_mode;}
	public void setComplexMode(boolean b){complex_mode = b;}
	
	/* --- Load --- */
	
	public FileBuffer loadData(long stpos, long len) throws IOException{
		//Build a MultiFileBuffer out of CacheFileBuffers, I think...
		
		long pos = 0;
		long remain = len;
		MultiFileBuffer dat = new MultiFileBuffer(pieces.size()+1);
		for(FileNode piece : pieces){
			long end = pos + piece.getLength();
			if(end <= stpos){
				pos = end;
				continue;
			}
			
			//Determine start of piece
			long st = 0;
			if(stpos > pos) st = stpos - pos;
			
			//Determine end of piece
			long ed = piece.getLength();
			if(ed - st > remain) ed = st + remain;
			
			//Load
			FileBuffer pdat = CacheFileBuffer.getReadOnlyCacheBuffer(piece.getSourcePath(), piece.getOffset() + st, piece.getOffset() + ed);
			dat.addToFile(pdat);
			
			remain -= (ed - st);
			pos = end;
		}
		
		return dat;
	}
	
	/* --- Other --- */
	
	protected void copyDataTo(PatchworkFileNode copy){
		super.copyDataTo(copy);
		
		copy.pieces = getBlocks();
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
	
	public String getLocationString(){
		StringBuilder sb = new StringBuilder(1024);
		
		if(sourceDataCompressed()){
			for(CompressionInfoNode c : super.getCompressionChain()){
				sb.append("Decomp From: 0x" + Long.toHexString(c.getStartOffset()) + " -> ");
			}
		}
		
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
		for(FileNode piece : pieces){
			String path = piece.getSourcePath();
			String stoff = "0x" + Long.toHexString(piece.getOffset());
			String edoff = "0x" + Long.toHexString(piece.getOffset() + piece.getLength());
			System.err.print("[" + path + ":" + stoff + "-" + edoff + "]");
		}
		System.err.print(")");
		System.err.println();
	}

}
