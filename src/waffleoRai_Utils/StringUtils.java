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

}
