package waffleoRai_Encryption;

import java.util.Map;
import java.util.TreeMap;

public class StaticDecryption {

	/*private static StaticDecryptor decryptor;
	
	public static void setDecryptorState(StaticDecryptor obj){decryptor = obj;}
	public static StaticDecryptor getDecryptorState(){return decryptor;}*/
	
	private static Map<Long, StaticDecryptor> decryptor_map;
	
	public static void setDecryptorState(long defUID, StaticDecryptor obj){
		if(decryptor_map == null) decryptor_map = new TreeMap<Long, StaticDecryptor>();
		decryptor_map.put(defUID, obj);
	}
	
	public static StaticDecryptor getDecryptorState(long defUID){
		if(decryptor_map == null) return null;
		return decryptor_map.get(defUID);
	}
	
}
