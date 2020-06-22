package waffleoRai_Image;

import java.awt.image.BufferedImage;
import java.util.Collection;

import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Utils.FileNode;

public abstract class PalettedImageDef implements FileTypeDefinition{
	
	public abstract BufferedImage renderData(FileNode src);
	public abstract BufferedImage renderWithPalette(FileNode src, Palette plt);

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
