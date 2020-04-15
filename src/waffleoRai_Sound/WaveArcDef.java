package waffleoRai_Sound;

import java.util.List;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Utils.FileNode;

public abstract class WaveArcDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.SOUND_WAVEARC;}
	public abstract List<Sound> getContents(FileNode file);
	
}
