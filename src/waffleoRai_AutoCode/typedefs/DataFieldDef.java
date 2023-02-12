package waffleoRai_AutoCode.typedefs;

import waffleoRai_AutoCode.BingennerTarget;

public abstract class DataFieldDef {
	protected String name;
	protected String description;
	public String getName(){return name;}
	public String getDescription(){return description;}
	public void setName(String value){name = value;}
	public void setDescription(String value){description = value;}
	
	public abstract void addTo(BingennerTarget target_doc);
	
	
	/*----- Debug -----*/
	
	public abstract void debug_printToStderr(int tabs);
	
}
