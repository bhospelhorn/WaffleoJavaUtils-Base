package waffleoRai_AutoCode.typedefs;

import java.util.LinkedList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import waffleoRai_AutoCode.Bingenner;
import waffleoRai_AutoCode.BingennerPackage;

public class BingennerTypedef {
	
	private static final String ATTR_NAME = "Name";
	private static final String ATTR_DESC = "Description";
	private static final String ATTR_BYTEORDER = "ByteOrder";

	/*----- Instance Variables -----*/
	
	protected BingennerPackage parent_pack;
	
	protected String type_name;
	protected String description;
	protected DataFieldDef[] fields;
	
	protected int byte_order = Bingenner.BYTEORDER_PARENT;
	
	/*----- Init -----*/
	
	public BingennerTypedef(BingennerPackage parent, Element xml_element){
		parent_pack = parent;
		if(parent_pack != null){
			parent_pack.addChildType(this);
		}
		
		if(xml_element == null) return;
		if(xml_element.hasAttribute(ATTR_NAME)) type_name = xml_element.getAttribute(ATTR_NAME);
		if(xml_element.hasAttribute(ATTR_DESC)) description = xml_element.getAttribute(ATTR_DESC);
		if(xml_element.hasAttribute(ATTR_BYTEORDER)){
			String val = xml_element.getAttribute(ATTR_BYTEORDER);
			val = val.toLowerCase();
			if(val.equals("big")){
				byte_order = Bingenner.BYTEORDER_BIG;
			}
			else if(val.equals("little")){
				byte_order = Bingenner.BYTEORDER_LITTLE;
			}
			else if(val.equals("system")){
				byte_order = Bingenner.BYTEORDER_SYSTEM;
			}
		}
		
		LinkedList<DataFieldDef> templist = new LinkedList<DataFieldDef>();
		
		NodeList child_list = xml_element.getChildNodes();
		int ccount = child_list.getLength();
		//fields = new DataFieldDef[ccount];
		for(int i = 0; i < ccount; i++){
			Node cn = child_list.item(i);
			if(cn.getNodeType() == Node.ELEMENT_NODE){
				Element child = (Element)cn;
				String cname = child.getNodeName();
				if(cname.equals("DataField")){
					if(!child.hasAttribute("Type")) continue;
					String dtype = child.getAttribute("Type");
					if(dtype.endsWith("[]")){
						//fields[i] = new ArrayFieldDef(child);
						templist.add(new ArrayFieldDef(child));
					}
					else{
						//fields[i] = new BasicDataFieldDef(child);
						if(dtype.equals("Struct")){
							templist.add(new AnonStructFieldDef(child));
						}
						else templist.add(new BasicDataFieldDef(child));
					}
				}
				else if(cname.equals("BinData")){
					ArrayFieldDef ndef = new ArrayFieldDef(child);
					ndef.setTypeName("u8");
					//fields[i] = ndef;
					templist.add(ndef);
				}
				else if(cname.equals("BitField")){
					//fields[i] = new BitFieldDef(child);
					templist.add(new BitFieldDef(child));
				}
				else if(cname.equals("List")){
					//fields[i] = new ListDef(child);
					templist.add(new ListDef(child));
				}
			}
		}
		
		if(templist.isEmpty()) return;
		
		ccount = templist.size();
		int i = 0;
		fields = new DataFieldDef[ccount];
		for(DataFieldDef ndef : templist) fields[i++] = ndef;
		templist.clear();
	}
	
	/*----- Getters -----*/
	
	public BingennerPackage getPackage(){return parent_pack;}
	public String getName(){return type_name;}
	public String getDescription(){return description;}
	public int getPreferredByteOrder(){return byte_order;}
	
	public DataFieldDef[] getFields(){return fields;}
	
	/*----- Setters -----*/
	
	/*----- Debug -----*/
	
	public void debug_printToStderr(int tabs){
		for(int i = 0; i < tabs; i++) System.err.print("\t");
		System.err.print("-> [TYPE] " + type_name + "\n");
		if(fields != null){
			for(int i = 0; i < fields.length; i++){
				if(fields[i] != null){
					fields[i].debug_printToStderr(tabs+1);
				}
				else{
					for(int j = 0; j < tabs; j++) System.err.print("\t");
					System.err.print("\t-> <NULL>\n");
				}
			}
		}
	}
	
}
