package waffleoRai_AutoCode.typedefs;

import java.util.HashMap;
import java.util.Map;

import waffleoRai_AutoCode.BingennerTarget;

public abstract class DataFieldDef {
	protected String name;
	protected String description;
	
	protected Map<String, String> xml_attr;
	
	public String getName(){return name;}
	public String getDescription(){return description;}
	public void setName(String value){name = value;}
	public void setDescription(String value){description = value;}
	
	public abstract void addTo(BingennerTarget target_doc);
	
	protected void addAttribute(String key, String value){
		if(xml_attr == null) xml_attr = new HashMap<String, String>();
		xml_attr.put(key, value);
	}
	
	public String getAttribute(String key){
		if(xml_attr == null) return null;
		return xml_attr.get(xml_attr);
	}
	
	/*----- Debug -----*/
	
	public abstract void debug_printToStderr(int tabs);
	
}
