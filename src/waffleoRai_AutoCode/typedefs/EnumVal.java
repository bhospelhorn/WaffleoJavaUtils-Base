package waffleoRai_AutoCode.typedefs;

import org.w3c.dom.Element;

public class EnumVal {
	
	private static final String ATTR_NAME = "Name";
	private static final String ATTR_VALUE = "Value";
	private static final String ATTR_DESC = "Description";
	
	private String name;
	private String value;
	private String description;

	public EnumVal(Element xml_element){
		if(xml_element == null) return;
		if(xml_element.hasAttribute(ATTR_NAME)) name = xml_element.getAttribute(ATTR_NAME);
		if(xml_element.hasAttribute(ATTR_VALUE)) value = xml_element.getAttribute(ATTR_VALUE);
		if(xml_element.hasAttribute(ATTR_DESC)) description = xml_element.getAttribute(ATTR_DESC);
	}
	
	public String getName(){return name;}
	public String getValueString(){return value;}
	public String getDescription(){return description;}
	
}
