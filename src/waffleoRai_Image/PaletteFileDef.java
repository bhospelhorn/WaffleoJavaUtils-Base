package waffleoRai_Image;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Utils.FileNode;

public abstract class PaletteFileDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.IMG_PALETTE;}
	public abstract Palette getPalette(FileNode src, int pidx);

}
