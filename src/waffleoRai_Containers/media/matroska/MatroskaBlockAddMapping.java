package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLRawElement;
import waffleoRai_Containers.ebml.EBMLStringElement;

public class MatroskaBlockAddMapping {
	
	public static final int EBML_BASE_ID = 0x1e4;
	
	private static final int EBML_ID_IDVAL = 0x1f0;
	private static final int EBML_ID_IDNAME = 0x1a4;
	private static final int EBML_ID_IDTYPE = 0x1e7;
	private static final int EBML_ID_IDEXTRA = 0x1ed;
	
	private int blockAddIDValue;
	private String blockAddIDName;
	private int blockAddIDType;
	private byte[] blockAddIDExtra;
	
	/*----- Getters -----*/

	public int getBlockAddIDValue(){return blockAddIDValue;}
	public String getBlockAddIDName(){return blockAddIDName;}
	public int getBlockAddIDType(){return blockAddIDType;}
	public byte[] getBlockAddIDExtra(){return blockAddIDExtra;}

	/*----- Setters -----*/

	public void setBlockAddIDValue(int value){blockAddIDValue = value;}
	public void setBlockAddIDName(String value){blockAddIDName = value;}
	public void setBlockAddIDType(int value){blockAddIDType = value;}
	public void setBlockAddIDExtra(byte[] value){blockAddIDExtra = value;}

	/*----- Read -----*/
	
	public static MatroskaBlockAddMapping fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaBlockAddMapping item = new MatroskaBlockAddMapping();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_IDVAL);
		item.blockAddIDValue = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_IDNAME);
		item.blockAddIDName = EBMLCommon.readStringElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_IDTYPE);
		item.blockAddIDType = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_IDEXTRA);
		item.blockAddIDExtra = EBMLCommon.loadBlobElement(e);
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {	
		if(blockAddIDType < 0) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		
		if(blockAddIDValue >= 2) {
			me.addChild(new EBMLIntElement(EBML_ID_IDVAL, blockAddIDValue, false));
		}
		if(blockAddIDName != null) {
			me.addChild(new EBMLStringElement(EBML_ID_IDNAME, blockAddIDName, false));
		}
		me.addChild(new EBMLIntElement(EBML_ID_IDTYPE, blockAddIDType, false));
		if(blockAddIDExtra != null) {
			me.addChild(new EBMLRawElement(EBML_ID_IDEXTRA, blockAddIDExtra));
		}
		
		return me;
	}
	
}
