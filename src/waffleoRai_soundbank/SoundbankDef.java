package waffleoRai_soundbank;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_SoundSynth.SynthBank;
import waffleoRai_Utils.FileNode;

public abstract class SoundbankDef implements FileTypeDefinition{
	
	public FileClass getFileClass(){return FileClass.SOUNDBANK;}
	public abstract SynthBank getPlayableBank(FileNode file);
	public abstract String getBankIDKey(FileNode file);

}
