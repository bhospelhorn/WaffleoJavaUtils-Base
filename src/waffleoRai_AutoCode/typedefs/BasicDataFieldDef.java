package waffleoRai_AutoCode.typedefs;

import org.w3c.dom.Element;

import waffleoRai_AutoCode.Bingenner;
import waffleoRai_AutoCode.BingennerTarget;

public class BasicDataFieldDef extends DataFieldDef{
	
	private static final String ATTR_NAME = "Name";
	private static final String ATTR_TYPE = "Type";
	private static final String ATTR_DESC = "Description";
	private static final String ATTR_DEFOVAL = "DefaultValue";
	
	protected String type_name = "s32"; //Default
	
	protected String default_value = null; //Leave as string
	
	public BasicDataFieldDef(String field_name){name = field_name;}
	
	public BasicDataFieldDef(Element xml_element){
		if(xml_element == null) return;
		if(xml_element.hasAttribute(ATTR_NAME)) super.name = xml_element.getAttribute(ATTR_NAME);
		if(xml_element.hasAttribute(ATTR_DESC)) super.description = xml_element.getAttribute(ATTR_DESC);
		if(xml_element.hasAttribute(ATTR_TYPE)) type_name = xml_element.getAttribute(ATTR_TYPE);
		if(xml_element.hasAttribute(ATTR_DEFOVAL)) default_value = xml_element.getAttribute(ATTR_DEFOVAL);
	}
	
	public String getTypeName(){return type_name;}
	public void setTypeName(String value){type_name = value;}
	
	public void addTo(BingennerTarget target_doc){
		for(String ptype : Bingenner.PRIM_TYPES){
			if(type_name.equals(ptype)){
				target_doc.addPrimitive(this);
				return;
			}
		}
		
		//Enum?
		if(type_name.startsWith("Enum:")){
			target_doc.addEnum(this);
			return;
		}
		
		//Some other type
		if(type_name.endsWith("*")){
			target_doc.addStruct(this, type_name.replace("*", ""), true);
		}
		else target_doc.addStruct(this, type_name, false);
	}
	
	public String getDefaultValue(){return default_value;}
	
	public void debug_printToStderr(int tabs){
		for(int i = 0; i < tabs; i++) System.err.print("\t");
		System.err.print("-> [DATAFIELD] " + type_name + " " + super.name + "\n");
	}
	
}
