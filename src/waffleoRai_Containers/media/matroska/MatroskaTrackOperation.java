package waffleoRai_Containers.media.matroska;

import java.util.LinkedList;
import java.util.List;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;

public class MatroskaTrackOperation {

	public static final int EBML_BASE_ID = 0x62;
	
	private static final int EBML_ID_COMBINE_PLANES = 0x63;
	private static final int EBML_ID_JOIN_BLOCKS = 0x69;
	private static final int EBML_ID_JOIN_UID = 0x6d;
	
	//Track combine planes
	private List<MatroskaTrackPlane> combinePlanes;
	
	//Track join blocks
	private List<Integer> joinUIDs;
	
	public MatroskaTrackOperation() {
		combinePlanes = new LinkedList<MatroskaTrackPlane>();
		joinUIDs = new LinkedList<Integer>();
	}

	/*----- Getters -----*/

	public List<MatroskaTrackPlane> getCombinePlanes(){return combinePlanes;}
	public List<Integer> getJoinUIDs(){return joinUIDs;}

	/*----- Setters -----*/

	public void setCombinePlanes(List<MatroskaTrackPlane> value){combinePlanes = value;}
	public void setJoinUIDs(List<Integer> value){joinUIDs = value;}
	
	/*----- Read -----*/
	
	public static MatroskaTrackOperation fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaTrackOperation item = new MatroskaTrackOperation();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_COMBINE_PLANES);
		if((e != null) && (e instanceof EBMLMasterElement)) {
			EBMLMasterElement cme = (EBMLMasterElement) e;
			List<EBMLElement> elist = cme.getChildrenWithId(MatroskaTrackPlane.EBML_BASE_ID);
			if(elist != null) {
				for(EBMLElement ee : elist) {
					MatroskaTrackPlane p = MatroskaTrackPlane.fromEBML(ee);
					if(p != null) item.combinePlanes.add(p);
				}
			}
		}
		
		e = me.getFirstChildWithId(EBML_ID_JOIN_BLOCKS);
		if((e != null) && (e instanceof EBMLMasterElement)) {
			EBMLMasterElement cme = (EBMLMasterElement) e;
			List<EBMLElement> elist = cme.getChildrenWithId(EBML_ID_JOIN_UID);
			if(elist != null) {
				for(EBMLElement ee : elist) {
					int n = EBMLCommon.readIntElement(ee);
					if(n >= 0) item.joinUIDs.add(n);
				}
			}
		}
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		if(!combinePlanes.isEmpty()) {
			EBMLMasterElement cme = new EBMLMasterElement(EBML_ID_COMBINE_PLANES);
			for(MatroskaTrackPlane p : combinePlanes) {
				EBMLElement pe = p.toEBML();
				if(pe != null) cme.addChild(pe);
			}
			me.addChild(cme);
		}

		if(!joinUIDs.isEmpty()) {
			EBMLMasterElement cme = new EBMLMasterElement(EBML_ID_JOIN_BLOCKS);
			for(Integer j : joinUIDs) {
				cme.addChild(new EBMLIntElement(EBML_ID_JOIN_UID, j, false));
			}
			me.addChild(cme);
		}
		
		return me;
	}

	
}
