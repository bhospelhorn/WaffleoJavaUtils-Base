package waffleoRai_Files;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileDefinitions {

	private static Map<Integer, FileTypeDefinition> def_map;
	
	private static void buildMap(){
		def_map = new ConcurrentHashMap<Integer, FileTypeDefinition>();
	}
	
	public static void registerDefinition(FileTypeDefinition def){
		if(def_map == null) buildMap();
		def_map.put(def.getTypeID(), def);
	}
	
	public static FileTypeDefinition getDefinitionByID(int id){
		if(def_map == null) buildMap();
		return def_map.get(id);
	}
	
	public static Collection<FileTypeDefinition> getAllRegisteredDefinitions(){
		if(def_map == null) buildMap();
		List<FileTypeDefinition> list = new LinkedList<FileTypeDefinition>();
		list.addAll(def_map.values());
		return list;
	}
	
	//Let's put a quick empty file def here
	
	private static EmptyFileDefinition ef_def;
	
	public static class EmptyFileDefinition implements FileTypeDefinition{

		public static final int DEF_ID = 0x01010101;
		public static final String DEFO_ENG_STR = "Empty File";
		
		private String desc = DEFO_ENG_STR;
		
		public Collection<String> getExtensions() {
			LinkedList<String> list = new LinkedList<String>();
			list.add("*");
			return list;
		}

		public String getDescription() {return desc;}
		public FileClass getFileClass() {return FileClass.EMPTY_FILE;}
		public int getTypeID() {return DEF_ID;}
		public void setDescriptionString(String s) {desc = s;}
		public String getDefaultExtension() {return "";}
		
		
	}
	
	public static EmptyFileDefinition getEmptyFileDef(){
		if(ef_def == null) ef_def = new EmptyFileDefinition();
		return ef_def;
	}
	
	
}
