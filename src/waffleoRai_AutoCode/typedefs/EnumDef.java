package waffleoRai_AutoCode.typedefs;

import java.util.LinkedList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import waffleoRai_AutoCode.BingennerPackage;

public class EnumDef {
	
	private static final String ATTR_NAME = "Name";
	private static final String ATTR_TYPE = "Type";
	private static final String ATTR_DESC = "Description";
	
	private String name;
	private String type_string;
	private String description;
	
	private BingennerPackage parent_pkg;
	
	private EnumVal[] values;
	
	public EnumDef(Element xml_element){
		if(xml_element == null) return;
		if(xml_element.hasAttribute(ATTR_NAME)) name = xml_element.getAttribute(ATTR_NAME);
		if(xml_element.hasAttribute(ATTR_TYPE)) type_string = xml_element.getAttribute(ATTR_TYPE);
		if(xml_element.hasAttribute(ATTR_DESC)) description = xml_element.getAttribute(ATTR_DESC);
		
		LinkedList<EnumVal> templist = new LinkedList<EnumVal>();
		NodeList child_list = xml_element.getChildNodes();
		int ccount = child_list.getLength();
		for(int i = 0; i < ccount; i++){
			Node cn = child_list.item(i);
			if(cn.getNodeType() == Node.ELEMENT_NODE){
				Element child = (Element)cn;
				String cname = child.getNodeName();
				if(cname.equals("EnumValue")){
					EnumVal eval = new EnumVal(child);
					templist.add(eval);
				}
			}
		}
		
		if(!templist.isEmpty()){
			ccount = templist.size();
			values = new EnumVal[ccount];
			int i = 0;
			for(EnumVal eval : templist){
				values[i++] = eval;
			}
			templist.clear();
		}
	}

	public String getName(){return name;}
	public String getTypeString(){return type_string;}
	public String getDescription(){return description;}
	public BingennerPackage getPackage(){return parent_pkg;}

	public int getValueCounts(){
		if(values == null) return 0;
		return values.length;
	}
	
	public EnumVal getValue(int idx){
		if(idx < 0) return null;
		if(values == null) return null;
		if(idx >= values.length) return null;
		return values[idx];
	}
	
	public void setParentPackage(BingennerPackage pkg){parent_pkg = pkg;}
	
}
