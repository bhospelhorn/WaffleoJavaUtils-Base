package waffleoRai_AutoCode.typedefs;

import org.w3c.dom.Element;

import waffleoRai_AutoCode.Bingenner;
import waffleoRai_AutoCode.BingennerTarget;

public class ArrayFieldDef extends BasicDataFieldDef{
	
	private static final String ATTR_LEN = "Length";
	
	protected String len_str;
	
	public ArrayFieldDef(Element xml_element){
		super(xml_element);
		if(xml_element == null) return;
		if(xml_element.hasAttribute(ATTR_LEN)) len_str = xml_element.getAttribute(ATTR_LEN);
		super.type_name = super.type_name.replace("[]", "");
	}
	
	public String getLengthString(){return len_str;}
	
	public int lengthAsInt(){
		//Returns 0 if not valid int
		if(len_str == null) return 0;
		try{
			return Integer.parseInt(len_str);
		}
		catch(NumberFormatException ex){return 0;}
	}
	
	public void setLengthString(String value){len_str = value;}
	
	public void addTo(BingennerTarget target_doc){
		for(String ptype : Bingenner.PRIM_TYPES){
			if(type_name.equals(ptype)){
				target_doc.addPrimitiveArray(this);
				return;
			}
		}
		//Some other type
		if(type_name.endsWith("*")){
			target_doc.addStructArray(this, type_name.replace("*", ""), true);
		}
		else target_doc.addStructArray(this, type_name, false);
	}
	
	public void debug_printToStderr(int tabs){
		for(int i = 0; i < tabs; i++) System.err.print("\t");
		System.err.print("-> [DATAFIELD] " + type_name + "[] " + super.name + "\n");
	}

}

