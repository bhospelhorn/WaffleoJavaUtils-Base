package waffleoRai_Containers;

import java.io.IOException;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Utils.DirectoryNode;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;
import waffleoRai_Utils.FileNode;

public abstract class ArchiveDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.ARCHIVE;}
	public abstract DirectoryNode getContents(FileNode node) throws IOException, UnsupportedFileTypeException;

}
