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
		String str = valueMap.get(value);
		if(str != null) return str;
		return rawStringFromValue(value);
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
		T val = stringMap.get(str);
		if(val != null) return val;
		return valueFromRawString(str);
	}
	
	protected abstract String rawStringFromValue(T value);
	protected abstract T valueFromRawString(String str);
	
	public abstract String stringFromValueFlags(T value);
	public abstract T valueFromStringFlags(String str);
	
	public void dispose() {
		if(valueMap != null) valueMap.clear();
		valueMap = null;
		if(stringMap != null) stringMap.clear();
		stringMap = null;
	}

}
