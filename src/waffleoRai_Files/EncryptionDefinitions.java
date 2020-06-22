package waffleoRai_Files;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EncryptionDefinitions {
	
	private static Map<Integer, EncryptionDefinition> id_map;
	
	private static void buildMap()
	{
		id_map = new ConcurrentHashMap<Integer, EncryptionDefinition>();
	}
	
	public static void registerDefinition(EncryptionDefinition def)
	{
		if(id_map == null) buildMap();
		id_map.put(def.getID(), def);
	}
	
	public static EncryptionDefinition getByID(int id)
	{
		if(id_map == null) buildMap();
		return id_map.get(id);
	}
	
	public static Collection<EncryptionDefinition> getAllRegisteredDefinitions(){
		if(id_map == null) buildMap();
		List<EncryptionDefinition> list = new LinkedList<EncryptionDefinition>();
		list.addAll(id_map.values());
		return list;
	}

}
