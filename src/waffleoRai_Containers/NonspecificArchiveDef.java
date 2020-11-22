package waffleoRai_Containers;

import java.util.ArrayList;
import java.util.Collection;

import waffleoRai_Files.AbstractTypeDefinition;
import waffleoRai_Files.FileClass;

public class NonspecificArchiveDef extends AbstractTypeDefinition{

	public static final int DEF_ID = 0x00617263;
	public static final String DEFO_ENG_STR = "Binary Archive [Unspecified]";
	
	private static NonspecificArchiveDef stat_def;
	
	private String desc = DEFO_ENG_STR;
	
	public Collection<String> getExtensions() {
		ArrayList<String> list = new ArrayList<String>(1);
		list.add("*");
		return list;
	}

	public String getDescription() {return desc;}
	public FileClass getFileClass() {return FileClass.ARCHIVE;}
	public int getTypeID() {return DEF_ID;}
	public void setDescriptionString(String s) {desc = s;}

	public String getDefaultExtension() {return "";}

	public static NonspecificArchiveDef getDefinition(){
		if(stat_def == null) stat_def = new NonspecificArchiveDef();
		return stat_def;
	}
	
}
