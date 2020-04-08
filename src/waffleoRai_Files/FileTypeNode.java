package waffleoRai_Files;

import waffleoRai_Compression.definitions.AbstractCompDef;

public interface FileTypeNode {

	public boolean isCompression();
	public FileTypeNode getChild();
	public int getTypeID();
	
	public void setChild(FileTypeNode node);
	public FileClass getFileClass();
	
	public FileTypeDefinition getTypeDefinition();
	public AbstractCompDef getCompressionDefinition();
	
	public FileTypeNode copyChain();
}
