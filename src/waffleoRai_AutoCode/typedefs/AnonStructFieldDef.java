package waffleoRai_AutoCode.typedefs;

import java.util.LinkedList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import waffleoRai_AutoCode.BingennerTarget;

public class AnonStructFieldDef extends DataFieldDef{
	
	private static final String ATTR_NAME = "Name";
	private static final String ATTR_DESC = "Description";

	protected DataFieldDef[] record_structure;
	
	public AnonStructFieldDef(Element xml_element){
		if(xml_element == null) return;
		if(xml_element.hasAttribute(ATTR_NAME)) super.name = xml_element.getAttribute(ATTR_NAME);
		if(xml_element.hasAttribute(ATTR_DESC)) super.description = xml_element.getAttribute(ATTR_DESC);
		
		LinkedList<DataFieldDef> templist = new LinkedList<DataFieldDef>();
		
		NodeList child_list = xml_element.getChildNodes();
		int ccount = child_list.getLength();
		for(int i = 0; i < ccount; i++){
			Node cn = child_list.item(i);
			if(cn.getNodeType() == Node.ELEMENT_NODE){
				Element child = (Element)cn;
				String cname = child.getNodeName();
				if(cname.equals("DataField")){
					if(!child.hasAttribute("Type")) continue;
					String dtype = child.getAttribute("Type");
					if(dtype.endsWith("[]")){
						//record_structure[i] = new ArrayFieldDef(child);
						templist.add(new ArrayFieldDef(child));
					}
					else{
						//record_structure[i] = new BasicDataFieldDef(child);
						if(dtype.equals("Struct")){
							templist.add(new AnonStructFieldDef(child));
						}
						else templist.add(new BasicDataFieldDef(child));
					}
				}
				else if(cname.equals("BinData")){
					ArrayFieldDef ndef = new ArrayFieldDef(child);
					ndef.setTypeName("u8");
					//record_structure[i] = ndef;
					templist.add(ndef);
				}
				else if(cname.equals("BitField")){
					//record_structure[i] = new BitFieldDef(child);
					templist.add(new BitFieldDef(child));
				}
				else if(cname.equals("List")){
					//record_structure[i] = new ListDef(child);
					templist.add(new ListDef(child));
				}
			}
		}
		
		if(templist.isEmpty()) return;
		
		ccount = templist.size();
		int i = 0;
		record_structure = new DataFieldDef[ccount];
		for(DataFieldDef ndef : templist) record_structure[i++] = ndef;
		templist.clear();
	}
	
	public int getFieldCount(){
		return (record_structure != null)?record_structure.length:0;
	}
	
	public DataFieldDef getFieldDef(int index){
		if(index < 0) return null;
		if(record_structure == null) return null;
		if(index >= record_structure.length) return null;
		return record_structure[index];
	}
	
	public void addTo(BingennerTarget target_doc) {
		if(target_doc == null) return;
		target_doc.addAnonStruct(this);
	}
	public void debug_printToStderr(int tabs) {
		for(int i = 0; i < tabs; i++) System.err.print("\t");
		System.err.print("-> [ANON STRUCT] " + super.name + "\n");
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
