package waffleoRai_Containers;

import java.io.IOException;
import java.util.Collection;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Files.tree.DirectoryNode;
import waffleoRai_Files.tree.FileNode;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

public abstract class ArchiveDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.ARCHIVE;}
	public abstract DirectoryNode getContents(FileNode node) throws IOException, UnsupportedFileTypeException;

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
