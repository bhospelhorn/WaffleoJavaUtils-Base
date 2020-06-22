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
	
}
