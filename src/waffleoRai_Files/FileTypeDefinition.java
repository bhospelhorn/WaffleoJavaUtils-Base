package waffleoRai_Files;

import java.util.Collection;

public interface FileTypeDefinition {
	
	public Collection<String> getExtensions();
	public String getDescription();
	public FileClass getFileClass();
	public int getTypeID();
	
	public void setDescriptionString(String s);
	
	public String getDefaultExtension();
	
	public static String stringMe(FileTypeDefinition def){
		StringBuilder sb = new StringBuilder(1024);
		sb.append(def.getDescription());
		
		Collection<String> exts = def.getExtensions();
		if(exts != null && !exts.isEmpty()){
			sb.append(" (");
			boolean first = true;
			for(String ext : exts){
				if(!first) sb.append(", ");
				else first = false;
				
				sb.append("." + ext);
			}
			sb.append(")");
		}

		return sb.toString();
	}

}
