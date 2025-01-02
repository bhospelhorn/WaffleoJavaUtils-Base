package waffleoRai_DataContainers;

public class EnumStringMapper16 extends EnumStringMapper<Short>{
	
	public EnumStringMapper16(short[] values, String[] strings) {
		super.allStrings = strings;
		int vcount = values.length;
		Short[] objArr = new Short[vcount];
		for(int i = 0; i < vcount; i++) objArr[i] = values[i];
		super.allValues = objArr;
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
		
		if(out.isEmpty()) out = null;
		return out;
	}

	public Short valueFromStringFlags(String str) {
		if(str == null) return null;
		if(stringMap == null) super.populateStringMap();
		
		Short intObj = stringMap.get(str);
		if(intObj != null) return intObj;
		
		int output = 0;
		String[] strFlags = str.split("|");
		for(String substr : strFlags) {
			substr = substr.trim();
			intObj = stringMap.get(substr);
			if(intObj != null) {
				output |= Short.toUnsignedInt(intObj);
			}
		}
		
		return (short)output;
	}

}
