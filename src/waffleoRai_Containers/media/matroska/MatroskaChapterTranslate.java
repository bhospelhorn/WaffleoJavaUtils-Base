package waffleoRai_Containers.media.matroska;

import java.util.LinkedList;
import java.util.List;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLRawElement;

public class MatroskaChapterTranslate {

	public static final int EBML_BASE_ID = 0x2924;
	
	private static final int EBML_ID_TRANSLATE_ID = 0x29a5;
	private static final int EBML_ID_TRANSLATE_CODEC = 0x29bf;
	private static final int EBML_ID_TRANSLATE_EDITION = 0x29fc;
	
	private byte[] id;
	private int codec = -1;
	private List<Integer> editionUIDs;
	
	public MatroskaChapterTranslate() {
		editionUIDs = new LinkedList<Integer>();
	}
	
	/*----- Getters -----*/

	public byte[] getId(){return id;}
	public int getCodec(){return codec;}
	public List<Integer> getEditionUIDs(){return editionUIDs;}

	/*----- Setters -----*/

	public void setId(byte[] value){id = value;}
	public void setCodec(int value){codec = value;}
	public void setEditionUIDs(List<Integer> value){editionUIDs = value;}
	
	/*----- Read -----*/
	
	public static MatroskaChapterTranslate fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaChapterTranslate item = new MatroskaChapterTranslate();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_TRANSLATE_ID);
		if(e == null) return null;
		item.id = EBMLCommon.loadBlobElement(e);
		
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
		if(id == null) return null;
		if(codec < 0) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLRawElement(EBML_ID_TRANSLATE_ID, id));
		me.addChild(new EBMLIntElement(EBML_ID_TRANSLATE_CODEC, codec, false));
		if(!editionUIDs.isEmpty()) {
			for(Integer i : editionUIDs) {
				me.addChild(new EBMLIntElement(EBML_ID_TRANSLATE_EDITION, i, false));
			}
		}
		
		return me;
	}
	
}
