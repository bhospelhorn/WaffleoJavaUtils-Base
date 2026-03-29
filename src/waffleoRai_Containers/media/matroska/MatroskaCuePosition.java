package waffleoRai_Containers.media.matroska;

import java.util.LinkedList;
import java.util.List;

import waffleoRai_Containers.ebml.EBMLBigIntElement;
import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;

public class MatroskaCuePosition {
	
	public static final int EBML_BASE_ID = 0x37;
	
	private static final int EBML_ID_TRACK = 0x77;
	private static final int EBML_ID_CLUSTER_POS = 0x71;
	private static final int EBML_ID_REL_POS = 0x70;
	private static final int EBML_ID_DURATION = 0x32;
	private static final int EBML_ID_BLOCK_NO = 0x1378;
	private static final int EBML_ID_CODEC_STATE = 0x6a;
	private static final int EBML_ID_REFR = 0x5b;
	private static final int EBML_ID_REF_NO = 0x16;
	
	private int track = -1;
	private long clusterPosition = -1L;
	private long relativePosition = -1L;
	private long duration = -1L;
	private int blockNumber = -1;
	private long codecState = 0L;
	private List<Integer> references;
	
	public MatroskaCuePosition() {
		references = new LinkedList<Integer>();
	}
	
	/*----- Getters -----*/

	public int getTrack(){return track;}
	public long getClusterPosition(){return clusterPosition;}
	public long getRelativePosition(){return relativePosition;}
	public long getDuration(){return duration;}
	public int getBlockNumber(){return blockNumber;}
	public long getCodecState(){return codecState;}
	public List<Integer> getReferences(){return references;}

	/*----- Setters -----*/

	public void setTrack(int value){track = value;}
	public void setClusterPosition(long value){clusterPosition = value;}
	public void setRelativePosition(long value){relativePosition = value;}
	public void setDuration(long value){duration = value;}
	public void setBlockNumber(int value){blockNumber = value;}
	public void setCodecState(long value){codecState = value;}
	public void setReferences(List<Integer> value){references = value;}
	
	/*----- Read -----*/
	
	public static MatroskaCuePosition fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaCuePosition item = new MatroskaCuePosition();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_TRACK);
		if(e == null) return null;
		item.track = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_CLUSTER_POS);
		if(e == null) return null;
		item.clusterPosition = EBMLCommon.readIntElementLong(e);
		
		e = me.getFirstChildWithId(EBML_ID_CODEC_STATE);
		if(e == null) return null;
		item.codecState = EBMLCommon.readIntElementLong(e);
		
		e = me.getFirstChildWithId(EBML_ID_REL_POS);
		item.relativePosition = EBMLCommon.readIntElementLong(e);
		e = me.getFirstChildWithId(EBML_ID_DURATION);
		item.duration = EBMLCommon.readIntElementLong(e);
		e = me.getFirstChildWithId(EBML_ID_BLOCK_NO);
		item.blockNumber = EBMLCommon.readIntElement(e);
		
		List<EBMLElement> elist = me.getChildrenWithId(EBML_ID_REFR);
		if((elist != null) && !elist.isEmpty()) {
			for(EBMLElement ce : elist) {
				if(ce instanceof EBMLMasterElement) {
					EBMLMasterElement cme = (EBMLMasterElement) ce;
					EBMLElement ee = cme.getFirstChildWithId(EBML_ID_REF_NO);
					int n = EBMLCommon.readIntElement(ee);
					if(n >= 0) item.references.add(n);
				}
			}
		}
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(clusterPosition < 0L) return null;
		if(track < 0) return null;
		if(codecState < 0L) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLIntElement(EBML_ID_TRACK, track, false));
		me.addChild(new EBMLBigIntElement(EBML_ID_CLUSTER_POS, clusterPosition, false));
		if(relativePosition >= 0L) me.addChild(new EBMLBigIntElement(EBML_ID_REL_POS, relativePosition, false));
		if(duration >= 0L) me.addChild(new EBMLBigIntElement(EBML_ID_DURATION, duration, false));
		if(blockNumber >= 0L) me.addChild(new EBMLIntElement(EBML_ID_BLOCK_NO, blockNumber, false));
		me.addChild(new EBMLBigIntElement(EBML_ID_CODEC_STATE, codecState, false));
		
		if(!references.isEmpty()) {
			for(Integer i : references) {
				EBMLMasterElement cme = new EBMLMasterElement(EBML_ID_REFR);
				cme.addChild(new EBMLIntElement(EBML_ID_REF_NO, i, false));
				me.addChild(cme);
			}
		}
		
		return me;
	}

}
