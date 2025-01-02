package waffleoRai_DataContainers;

import java.util.HashMap;
import java.util.Map;

public abstract class EnumStringMapper<T> {
	
	protected T[] allValues;
	protected String[] allStrings;
	
	protected Map<T, String> valueMap;
	protected Map<String, T> stringMap;
	
	protected EnumStringMapper() {}
	
	public EnumStringMapper(T[] values, String[] strings){
		allValues = values;
		allStrings = strings;
	}
	
	protected void populateValueMap() {
		valueMap = new HashMap<T, String>();
		for(int i = 0; i < allValues.length; i++){
			valueMap.put(allValues[i], allStrings[i]);
		}
	}
	
	public String stringFromValue(T value){
		if(value == null) return null;
		if(valueMap == null){
			populateValueMap();
		}
		return valueMap.get(value);
	}
	
	protected void populateStringMap() {
		stringMap = new HashMap<String, T>();
		for(int i = 0; i < allStrings.length; i++){
			stringMap.put(allStrings[i], allValues[i]);
		}
	}
	
	public T valueFromString(String str) {
		if(stringMap == null){
			populateStringMap();
		}
		return stringMap.get(str);
	}
	
	public abstract String stringFromValueFlags(T value);
	public abstract T valueFromStringFlags(String str);
	
	public void dispose() {
		if(valueMap != null) valueMap.clear();
		valueMap = null;
		if(stringMap != null) stringMap.clear();
		stringMap = null;
	}

}
