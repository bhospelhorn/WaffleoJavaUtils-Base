package waffleoRai_Files;

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

}
