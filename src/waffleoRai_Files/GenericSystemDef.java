package waffleoRai_Files;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class GenericSystemDef implements FileTypeDefinition{
	
	private String str;
	private int type_id;
	
	public GenericSystemDef(String defo_eng, int id){
		str = defo_eng;
		type_id = id;
	}

	public Collection<String> getExtensions() {
		List<String> slist = new LinkedList<String>();
		slist.add("bin");
		return slist;
	}

	public String getDescription() {return str;}
	public FileClass getFileClass() {return FileClass.SYSTEM;}
	public int getTypeID() {return type_id;}
	public void setDescriptionString(String s) {str = s;}
	
	public String getDefaultExtension(){return "bin";}

}
