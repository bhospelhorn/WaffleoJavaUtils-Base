package waffleoRai_Sound;

import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Utils.FileNode;

public abstract class SoundFileDefinition implements FileTypeDefinition{

	public abstract Sound readSound(FileNode file);
	
}
