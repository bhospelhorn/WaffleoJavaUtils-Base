package waffleoRai_AutoCode.typedefs;

import java.util.LinkedList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import waffleoRai_AutoCode.BingennerTarget;
import waffleoRai_Files.XMLReader;

public class BitFieldDef extends DataFieldDef{
	
	private static final String ATTR_NAME = "Name";
	private static final String ATTR_DESC = "Description";
	private static final String ATTR_TWID = "Width";
	private static final String ATTR_BITS = "Bits";

	/*----- Inner Classes -----*/
	
	public static class BitField extends DataFieldDef{
		protected int bit_width;
		
		public BitField(Element xml_element){
			if(xml_element == null) return;
			if(xml_element.hasAttribute(ATTR_NAME)) super.name = xml_element.getAttribute(ATTR_NAME);
			if(xml_element.hasAttribute(ATTR_DESC)) super.description = xml_element.getAttribute(ATTR_DESC);
			if(xml_element.hasAttribute(ATTR_BITS)){
				try{
					this.bit_width = Integer.parseInt(xml_element.getAttribute(ATTR_BITS));
				}
				catch(NumberFormatException ex){
					ex.printStackTrace();
					this.bit_width = 1; //Set to 1.
				}
			}
			xml_attr = XMLReader.getAttributes(xml_element);
		}
		
		public int getWidth(){return bit_width;}
		public void setWidth(int value){bit_width = value;}
		
		public void addTo(BingennerTarget target_doc) {
			//Does nothing
		}
		
		public void debug_printToStderr(int tabs){
			for(int i = 0; i < tabs; i++) System.err.print("\t");
			System.err.print("-> ( " + bit_width + ") "+ super.name + "\n");
		}
	}
	
	/*----- Instance Variables -----*/
	
	protected int max_width;
	protected BitField[] fields;
	
	/*----- Methods -----*/
	
	public BitFieldDef(Element xml_element){
		if(xml_element == null) return;
		if(xml_element.hasAttribute(ATTR_NAME)) super.name = xml_element.getAttribute(ATTR_NAME);
		if(xml_element.hasAttribute(ATTR_DESC)) super.description = xml_element.getAttribute(ATTR_DESC);
		if(xml_element.hasAttribute(ATTR_TWID)){
			try{
				this.max_width = Integer.parseInt(xml_element.getAttribute(ATTR_TWID));
			}
			catch(NumberFormatException ex){
				ex.printStackTrace();
				this.max_width = -1; //Auto detect
			}
		}
		
		LinkedList<BitField> templist = new LinkedList<BitField>();
		
		//Read child nodes
		NodeList childlist = xml_element.getChildNodes();
		int ccount = childlist.getLength();
		int minwidth = (ccount + 0x7) & ~0x7;
		if(max_width < minwidth) max_width = minwidth;
		//fields = new BitField[ccount];
		for(int i = 0; i < ccount; i++){
			Node cn = childlist.item(i);
			if(cn.getNodeType() == Node.ELEMENT_NODE){
				Element child = (Element)cn;
				//fields[i] = new BitField(child);
				templist.add(new BitField(child));
			}
		}
		
		if(templist.isEmpty()) return;
		
		ccount = templist.size();
		int i = 0;
		fields = new BitField[ccount];
		for(BitField ndef : templist) fields[i++] = ndef;
		templist.clear();
	}
	
	public int getMaxWidth(){return max_width;}
	public BitField[] getFieldsDirect(){return fields;}

	public void addTo(BingennerTarget target_doc) {
		if(target_doc == null) return;
		target_doc.addBitfield(this);
	}
	
	public void debug_printToStderr(int tabs){
		for(int i = 0; i < tabs; i++) System.err.print("\t");
		System.err.print("-> [BITFIELD] " + super.name + "\n");
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
