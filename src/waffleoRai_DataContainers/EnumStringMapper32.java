package waffleoRai_DataContainers;

import waffleoRai_Utils.StringUtils;

public class EnumStringMapper32 extends EnumStringMapper<Integer>{

	public EnumStringMapper32(int[] values, String[] strings) {
		super.allStrings = strings;
		int vcount = values.length;
		Integer[] objArr = new Integer[vcount];
		for(int i = 0; i < vcount; i++) objArr[i] = values[i];
		super.allValues = objArr;
	}
	
	protected String rawStringFromValue(Integer value) {
		if(value == null) return null;
		return String.format("0x%08x", value);
	}
	
	protected Integer valueFromRawString(String str) {
		if(str == null) return null;
		try {
			return StringUtils.parseUnsignedInt(str);
		}
		catch(NumberFormatException ex) {ex.printStackTrace();}
		return null;
	}
	
	public String stringFromValueFlags(Integer value){
		if(value == null) return null;
		if(valueMap == null) super.populateValueMap();
		
		//First check direct
		String s = valueMap.get(value);
		if(s != null) return s;
		
		int mask = 1;
		boolean first = true;
		String out = "";
		for(int i = 0; i < 32; i++) {
			int masked = value & mask;
			if(masked != 0) {
				s = valueMap.get(masked);
				if(s != null) {
					if(!first) out += " | ";
					else first = false;
					out += s;	
				}
			}
			
			mask <<= 1;
		}
		
		if(out.isEmpty()) out = rawStringFromValue(value);
		return out;
	}

	public Integer valueFromStringFlags(String str) {
		if(str == null) return null;
		if(stringMap == null) super.populateStringMap();
		
		Integer intObj = stringMap.get(str);
		if(intObj != null) return intObj;
		
		int output = 0;
		if(str.startsWith("0x")) {
			output = valueFromRawString(str);
		}
		else {
			String[] strFlags = str.split("\\|");
			for(String substr : strFlags) {
				substr = substr.trim();
				intObj = stringMap.get(substr);
				if(intObj != null) {
					output |= intObj;
				}
			}
		}
		
		return output;
	}

}
