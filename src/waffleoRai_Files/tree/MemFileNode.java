package waffleoRai_Files.tree;

import java.io.IOException;

import waffleoRai_Utils.FileBuffer;

class MemFileNode extends FileNode{

	/* --- Instance Variables --- */
	
	private FileBuffer data;
	
	/* --- Init --- */
	
	public MemFileNode(DirectoryNode parent, String name) {
		super(parent, name);
	}
	
	/* --- Getters --- */
	
	public FileBuffer getData(){return data;}
	public FileNode getVirtualSource(){return null;}
	public boolean hasVirtualSource(){return false;}
	
	/* --- Setters --- */
	
	public void setData(FileBuffer dat){
		data = dat;
		super.setLength(data.getFileSize());
	}
	
	public void setUseVirtualSource(boolean b){throw new UnsupportedOperationException();}
	public void setVirtualSourceNode(FileNode vsource){throw new UnsupportedOperationException();}
	
	/* --- Loading --- */
	
	protected FileBuffer loadDirect(long stpos, long len, int options) throws IOException{
		if(data == null) return null;
		if(stpos == 0 && len == data.getFileSize()) return data;
		
		return data.createReadOnlyCopy(stpos, stpos + len);
	}
	
	/* --- Cleanup --- */
	
	public void dispose(){
		super.dispose();
		if(data != null){
			try{data.dispose();}
			catch(IOException ex){ex.printStackTrace();}
		}
	}
	
	/* --- Utils --- */
	
	protected void copyDataTo(FileNode copy){
		if(copy == null) return;
		super.copyDataTo(copy);
		if(copy instanceof MemFileNode){
			MemFileNode mcopy = (MemFileNode)copy;
			mcopy.data = this.data;
		}
	}
	
	public FileNode copy(DirectoryNode parent_copy){
		MemFileNode copy = new MemFileNode(parent_copy, this.getFileName());
		copyDataTo(copy);
		return copy;
	}
	
	public FileNode getSubFile(long stoff, long len){
		FileNode n1 = super.getSubFile(stoff, len);
		
		MemFileNode n = new MemFileNode(null, n1.getFileName());
		n1.copyDataTo(n);
		
		if(stoff == 0 && len == super.getLength()){
			return n;
		}
		
		if(data != null){
			n.setData(data.createReadOnlyCopy(stoff, stoff + len));
		}
		
		return n;
	}
	
	/* --- Debug --- */
	
	public String getLocationString(){
		if(data == null) return "NULL MEMORY";
		return "Size 0x" + Long.toHexString(data.getBaseCapacity()) + " memory buffer";
	}
	
	protected String getTypeString(){return "MemFileNode";}

}
