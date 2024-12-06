package waffleoRai_Utils;

public class StringUtils {
	
	public static String capitalize(String input){
		if(input == null) return null;
		if(input.isEmpty()) return input;
		String cap = input.substring(0,1).toUpperCase();
		if(input.length() < 2) return cap;
		return cap + input.substring(1);
	}
	
	public static String uncapitalize(String input){
		if(input == null) return null;
		if(input.isEmpty()) return input;
		String cap = input.substring(0,1).toLowerCase();
		if(input.length() < 2) return cap;
		return cap + input.substring(1);
	}
	
	public static int parseUnsignedInt(String val) {
		if(val == null) throw new NumberFormatException("Null input");
		if(val.startsWith("0x")) return Integer.parseUnsignedInt(val.substring(2), 16);
		else return Integer.parseUnsignedInt(val);
	}
	
	public static int parseSignedInt(String val) {
		if(val == null) throw new NumberFormatException("Null input");
		if(val.contains("0x")) {
			val = val.trim();
			boolean sign = false;
			if(val.startsWith("-")) sign = true;
			val = val.substring(1).trim();
			int ival = Integer.parseUnsignedInt(val.substring(2), 16);
			if(sign) ival = -ival;
			return ival;
		}
		else return Integer.parseInt(val);
	}
	
	public static long parseUnsignedLong(String val) {
		if(val == null) throw new NumberFormatException("Null input");
		if(val.startsWith("0x")) return Long.parseUnsignedLong(val.substring(2), 16);
		else return Long.parseUnsignedLong(val);
	}
	
	public static long parseSignedLong(String val) {
		if(val == null) throw new NumberFormatException("Null input");
		if(val.contains("0x")) {
			val = val.trim();
			boolean sign = false;
			if(val.startsWith("-")) sign = true;
			val = val.substring(1).trim();
			long ival = Long.parseUnsignedLong(val.substring(2), 16);
			if(sign) ival = -ival;
			return ival;
		}
		else return Long.parseLong(val);
	}

}
