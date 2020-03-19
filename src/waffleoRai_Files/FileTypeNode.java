package waffleoRai_Files;

public interface FileTypeNode {

	public boolean isCompression();
	public FileTypeNode getChild();
	public int getTypeID();
	
	public void setChild(FileTypeNode node);
	public FileClass getFileClass();
	
}
