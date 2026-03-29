package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLRawElement;

public class MatroskaBlockMore {
	
	public static final int EBML_BASE_ID = 0x26;
	
	private static final int EBML_ID_DATA = 0x25;
	private static final int EBML_ID_ADDID = 0x6e;
	
	private byte[] blockAddData;
	private int blockAddID;

	/*----- Getters -----*/

	public byte[] getBlockAddData(){return blockAddData;}
	public int getBlockAddID(){return blockAddID;}

	/*----- Setters -----*/

	public void setBlockAddData(byte[] value){blockAddData = value;}
	public void setBlockAddID(int value){blockAddID = value;}

	/*----- Read -----*/
	
	public static MatroskaBlockMore fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaBlockMore item = new MatroskaBlockMore();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_DATA);
		item.blockAddData = EBMLCommon.loadBlobElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_ADDID);
		item.blockAddID = EBMLCommon.readIntElement(e);
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(blockAddData == null) return null;
		if(blockAddID < 0) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		
		me.addChild(new EBMLIntElement(EBML_ID_DATA, blockAddID, false));
		if(blockAddData != null) {
			me.addChild(new EBMLRawElement(EBML_ID_ADDID, blockAddData));
		}
		
		return me;
	}
	
}
