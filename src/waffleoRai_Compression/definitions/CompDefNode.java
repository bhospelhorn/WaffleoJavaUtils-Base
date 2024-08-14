package waffleoRai_Compression.definitions;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Files.FileTypeNode;

public class CompDefNode implements FileTypeNode{
	
	private AbstractCompDef def;
	private FileTypeNode child;
	
	public CompDefNode(AbstractCompDef type){
		def = type;
	}
	
	@Override
	public boolean isCompression() {return true;}

	@Override
	public FileTypeNode getChild() {return child;}

	@Override
	public int getTypeID() {return def.getDefinitionID();}
	
	public AbstractCompDef getDefinition(){return def;}
	public void setChild(FileTypeNode node){child = node;}

	public String toString(){return def.getDescription();}
	
	public FileClass getFileClass(){return FileClass.COMPRESSED;}
	
	public FileTypeDefinition getTypeDefinition(){return null;}
	public AbstractCompDef getCompressionDefinition(){return getDefinition();}
	
	public FileTypeNode copyChain(){

		CompDefNode copy = new CompDefNode(def);
		if(child != null) {
			copy.child = child.copyChain();
		}
		else copy.child = null;

		return copy;
	}
}
