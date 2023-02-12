package waffleoRai_AutoCode.typedefs;

import org.w3c.dom.Element;
import waffleoRai_AutoCode.BingennerTarget;

public class ListDef extends AnonStructFieldDef{
	
	private static final String ATTR_ENTRY = "EntryStructName";
	private static final String ATTR_LEN = "Length";
	
	protected String len_str;
	protected String list_entry_name;
	
	public ListDef(Element xml_element){
		super(xml_element);
		if(xml_element == null) return;
		if(xml_element.hasAttribute(ATTR_LEN)) len_str = xml_element.getAttribute(ATTR_LEN);
		if(xml_element.hasAttribute(ATTR_ENTRY)) list_entry_name = xml_element.getAttribute(ATTR_ENTRY);
	}
	
	public String getListEntryName(){return list_entry_name;}
	
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
	
	public void addTo(BingennerTarget target_doc) {
		if(target_doc == null) return;
		target_doc.addTable(this);
	}
	
	public void debug_printToStderr(int tabs){
		for(int i = 0; i < tabs; i++) System.err.print("\t");
		System.err.print("-> [LIST] " + super.name + "\n");
		if(record_structure != null){
			for(int i = 0; i < record_structure.length; i++){
				if(record_structure[i] != null){
					record_structure[i].debug_printToStderr(tabs+1);
				}
				else{
					for(int j = 0; j < tabs; j++) System.err.print("\t");
					System.err.print("\t-> <NULL>\n");
				}
			}
		}
	}

}
