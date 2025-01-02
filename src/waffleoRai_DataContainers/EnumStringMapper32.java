package waffleoRai_DataContainers;

public class EnumStringMapper32 extends EnumStringMapper<Integer>{

	public EnumStringMapper32(int[] values, String[] strings) {
		super.allStrings = strings;
		int vcount = values.length;
		Integer[] objArr = new Integer[vcount];
		for(int i = 0; i < vcount; i++) objArr[i] = values[i];
		super.allValues = objArr;
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
				if(!first) out += " | ";
				else first = false;
				out += s;
			}
			
			mask <<= 1;
		}
		
		if(out.isEmpty()) out = null;
		return out;
	}

	public Integer valueFromStringFlags(String str) {
		if(str == null) return null;
		if(stringMap == null) super.populateStringMap();
		
		Integer intObj = stringMap.get(str);
		if(intObj != null) return intObj;
		
		int output = 0;
		String[] strFlags = str.split("|");
		for(String substr : strFlags) {
			substr = substr.trim();
			intObj = stringMap.get(substr);
			if(intObj != null) {
				output |= intObj;
			}
		}
		
		return output;
	}

}
