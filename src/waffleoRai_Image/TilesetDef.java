package waffleoRai_Image;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Utils.FileNode;

public abstract class TilesetDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.IMG_TILE;}
	public abstract Tileset getTileset(FileNode src);
	public abstract int countTiles(FileNode src);
	
}
