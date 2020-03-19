package waffleoRai_Files;

import java.util.Collection;

public interface FileTypeDefinition {
	
	public Collection<String> getExtensions();
	public String getDescription();
	public FileClass getFileClass();
	public int getTypeID();
	
	public void setDescriptionString(String s);

}
