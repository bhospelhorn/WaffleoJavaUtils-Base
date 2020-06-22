package waffleoRai_Executable;

import java.util.Collection;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;

public abstract class BincodeTypeDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.CODELIB;}
	public abstract boolean canDisassemble();
	public abstract Collection<BinInst> disassemble();

	public String toString(){
		StringBuilder sb = new StringBuilder(2048);
		Collection<String> exts = this.getExtensions();
		if(exts != null && !exts.isEmpty()){
			sb.append(this.getDescription());
			sb.append(" (");
			boolean first = true;
			for(String ext : exts){
				if(!first) sb.append(", ");
				first = false;
				sb.append("."); sb.append(ext);
			}
			sb.append(")");	
		}
		return sb.toString();
	}
	
}
