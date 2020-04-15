package waffleoRai_SeqSound;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;

public abstract class SoundSeqDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.SOUND_SEQ;}

}
