package waffleoRai_DataContainers;

import waffleoRai_Utils.StringUtils;

public class EnumStringMapper16 extends EnumStringMapper<Short>{
	
	public EnumStringMapper16(short[] values, String[] strings) {
		super.allStrings = strings;
		int vcount = values.length;
		Short[] objArr = new Short[vcount];
		for(int i = 0; i < vcount; i++) objArr[i] = values[i];
		super.allValues = objArr;
	}
	
	protected String rawStringFromValue(Short value) {
		if(value == null) return null;
		return String.format("0x%04x", value);
	}
	
	protected Short valueFromRawString(String str) {
		if(str == null) return null;
		try {
			return (short)StringUtils.parseUnsignedInt(str);
		}
		catch(NumberFormatException ex) {ex.printStackTrace();}
		return null;
	}
	
	public String stringFromValueFlags(Short value){
		if(value == null) return null;
		if(valueMap == null) super.populateValueMap();
		
		//First check direct
		String s = valueMap.get(value);
		if(s != null) return s;
		
		int mask = 1;
		int valExp = Short.toUnsignedInt(value);
		boolean first = true;
		String out = "";
		for(int i = 0; i < 16; i++) {
			short masked = (short)(valExp & mask);
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

	public Short valueFromStringFlags(String str) {
		if(str == null) return null;
		if(stringMap == null) super.populateStringMap();
		
		Short intObj = stringMap.get(str);
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
					output |= Short.toUnsignedInt(intObj);
				}
			}	
		}
		
		return (short)output;
	}

}
