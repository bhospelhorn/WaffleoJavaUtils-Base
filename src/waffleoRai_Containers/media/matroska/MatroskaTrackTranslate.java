package waffleoRai_Containers.media.matroska;

import java.util.LinkedList;
import java.util.List;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLRawElement;

public class MatroskaTrackTranslate {
	
	public static final int EBML_BASE_ID = 0x2624;
	
	private static final int EBML_ID_TRANSLATE_ID = 0x26a5;
	private static final int EBML_ID_TRANSLATE_CODEC = 0x26bf;
	private static final int EBML_ID_TRANSLATE_EDITION = 0x26fc;
	
	private byte[] trackID;
	private int codec;
	private List<Integer> editionUIDs;
	
	public MatroskaTrackTranslate() {
		editionUIDs = new LinkedList<Integer>();
	}
	
	/*----- Getters -----*/

	public byte[] getTrackID(){return trackID;}
	public int getCodec(){return codec;}
	public List<Integer> getEditionUIDs(){return editionUIDs;}

	/*----- Setters -----*/

	public void setTrackID(byte[] value){trackID = value;}
	public void setCodec(int value){codec = value;}
	public void setEditionUIDs(List<Integer> value){editionUIDs = value;}
	
	/*----- Read -----*/
	
	public static MatroskaTrackTranslate fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaTrackTranslate item = new MatroskaTrackTranslate();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_TRANSLATE_ID);
		if(e == null) return null;
		item.trackID = EBMLCommon.loadBlobElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_TRANSLATE_CODEC);
		item.codec = EBMLCommon.readIntElement(e);
		
		List<EBMLElement> elist = me.getChildrenWithId(EBML_ID_TRANSLATE_EDITION);
		if((elist != null) && !elist.isEmpty()) {
			for(EBMLElement ce : elist) {
				item.editionUIDs.add(EBMLCommon.readIntElement(ce));
			}
		}
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(trackID == null) return null;
		if(codec < 0) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLRawElement(EBML_ID_TRANSLATE_ID, trackID));
		me.addChild(new EBMLIntElement(EBML_ID_TRANSLATE_CODEC, codec, false));
		if(!editionUIDs.isEmpty()) {
			for(Integer i : editionUIDs) {
				me.addChild(new EBMLIntElement(EBML_ID_TRANSLATE_EDITION, i, false));
			}
		}
		
		return me;
	}

}
