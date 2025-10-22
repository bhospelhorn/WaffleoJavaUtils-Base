package waffleoRai_DataContainers;

import waffleoRai_Utils.StringUtils;

public class EnumStringMapper64 extends EnumStringMapper<Long>{
	
	public EnumStringMapper64(long[] values, String[] strings) {
		super.allStrings = strings;
		int vcount = values.length;
		Long[] objArr = new Long[vcount];
		for(int i = 0; i < vcount; i++) objArr[i] = values[i];
		super.allValues = objArr;
	}
	
	protected String rawStringFromValue(Long value) {
		if(value == null) return null;
		return String.format("0x%016x", value);
	}
	
	protected Long valueFromRawString(String str) {
		if(str == null) return null;
		try {
			return StringUtils.parseUnsignedLong(str);
		}
		catch(NumberFormatException ex) {ex.printStackTrace();}
		return null;
	}
	
	public String stringFromValueFlags(Long value){
		if(value == null) return null;
		if(valueMap == null) super.populateValueMap();
		
		//First check direct
		String s = valueMap.get(value);
		if(s != null) return s;
		
		long mask = 1L;
		boolean first = true;
		String out = "";
		for(int i = 0; i < 64; i++) {
			long masked = value & mask;
			if(masked != 0) {
				s = valueMap.get(masked);
				if(!first) out += " | ";
				else first = false;
				out += s;
			}
			
			mask <<= 1;
		}
		
		if(out.isEmpty()) out = rawStringFromValue(value);
		return out;
	}

	public Long valueFromStringFlags(String str) {
		if(str == null) return null;
		if(stringMap == null) super.populateStringMap();
		
		Long intObj = stringMap.get(str);
		if(intObj != null) return intObj;
		
		long output = 0;
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
