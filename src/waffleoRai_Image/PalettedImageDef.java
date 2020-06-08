package waffleoRai_Image;

import java.awt.image.BufferedImage;

import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Utils.FileNode;

public abstract class PalettedImageDef implements FileTypeDefinition{
	
	public abstract BufferedImage renderData(FileNode src);
	public abstract BufferedImage renderWithPalette(FileNode src, Palette plt);

}
