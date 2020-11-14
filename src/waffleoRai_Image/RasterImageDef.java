package waffleoRai_Image;

import java.awt.image.BufferedImage;
import java.util.Collection;

import waffleoRai_Files.FileTypeDefinition;
import waffleoRai_Files.tree.FileNode;

public abstract class RasterImageDef implements FileTypeDefinition{

	public abstract BufferedImage renderImage(FileNode src);

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
