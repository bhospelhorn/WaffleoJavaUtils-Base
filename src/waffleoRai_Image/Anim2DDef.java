package waffleoRai_Image;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;

public abstract class Anim2DDef implements FileTypeDefinition{

	public FileClass getFileClass(){return FileClass.IMG_ANIM_2D;}
	public abstract Animation getAnimation();
	public abstract int countFrames();
	
}
