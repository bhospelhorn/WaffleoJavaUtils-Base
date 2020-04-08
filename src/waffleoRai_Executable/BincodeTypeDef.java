package waffleoRai_Executable;

import java.util.Collection;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;

public abstract class BincodeTypeDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.CODELIB;}
	public abstract boolean canDisassemble();
	public abstract Collection<BinInst> disassemble();

}
