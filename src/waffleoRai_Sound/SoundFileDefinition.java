package waffleoRai_Sound;

import java.util.Collection;

import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Utils.FileNode;

public abstract class SoundFileDefinition implements FileTypeDefinition{

	public abstract Sound readSound(FileNode file);
	
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
