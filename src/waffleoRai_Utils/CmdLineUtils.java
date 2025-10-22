package waffleoRai_Utils;

import java.util.HashMap;
import java.util.Map;

public class CmdLineUtils {
	
	public Map<String, String> parseArguments(String[] args, String keyPrefix){
		//Also remove quotes
		if(keyPrefix == null) keyPrefix = "";
		
		int preflen = keyPrefix.length();
		Map<String, String> map = new HashMap<String, String>();
		if(args != null) {
			String lastkey = null;
			for(int i = 0; i < args.length; i++) {
				String arg = args[i].trim();
				if(arg.startsWith("\"")) arg = arg.substring(1);
				if(arg.endsWith("\"")) arg = arg.substring(0, arg.length()-1);
				if(arg.startsWith(keyPrefix)) {
					//key
					if(lastkey != null) map.put(lastkey, "True");
					lastkey = arg.substring(preflen);
				}
				else {
					//value
					if(lastkey != null) {
						map.put(lastkey, arg);
						lastkey = null;
					}
					else {
						System.err.println("[CmdLineUtils.parseArguments] | Value with no key: \"" + arg + "\" - Skipping...");
					}
				}
			}
		}
		
		return map;
	}

}
