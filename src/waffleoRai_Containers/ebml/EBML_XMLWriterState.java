package waffleoRai_Containers.ebml;

import java.util.HashMap;
import java.util.Map;

public class EBML_XMLWriterState {
	public String indent = "";
	public Map<String, String> filepathLookup;
	public boolean smallValAsAttr = true;
	public boolean stringValAsAttr = false;
	
	public Map<Integer, EBMLFieldDef> defMap;
	
	public EBML_XMLWriterState() {
		filepathLookup = new HashMap<String, String>();
	}
}
