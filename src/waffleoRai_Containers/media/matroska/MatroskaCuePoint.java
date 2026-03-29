package waffleoRai_Containers.media.matroska;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import waffleoRai_Containers.ebml.EBMLBigIntElement;
import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;

public class MatroskaCuePoint {
	
	public static final int EBML_BASE_ID = 0x3b;
	
	private static final int EBML_ID_TIME = 0x33;
	private static final int EBML_ID_POS = 0x37;
	
	private long time;
	//private List<MatroskaCuePosition> trackPositions;
	private Map<Integer, MatroskaCuePosition> trackPositions;
	
	public MatroskaCuePoint() {
		trackPositions = new HashMap<Integer, MatroskaCuePosition>();
	}
	
	/*----- Getters -----*/

	public long getTime(){return time;}

	/*----- Setters -----*/

	public void setTime(long value){time = value;}

	/*----- Read -----*/
	
	public static MatroskaCuePoint fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaCuePoint item = new MatroskaCuePoint();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_TIME);
		if(e == null) return null;
		item.time = EBMLCommon.readIntElement(e);
		
		List<EBMLElement> elist = me.getChildrenWithId(EBML_ID_POS);
		if((elist == null) || elist.isEmpty()) return null;
		for(EBMLElement ce : elist) {
			if(!(ce instanceof EBMLMasterElement)) continue;
			MatroskaCuePosition pos = MatroskaCuePosition.fromEBML(ce);
			if(pos == null) continue;
			item.trackPositions.put(pos.getTrack(), pos);
		}
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(time < 0L) return null;
		if(trackPositions.isEmpty()) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLBigIntElement(EBML_ID_TIME, time, false));
		
		List<Integer> keylist = new ArrayList<Integer>(trackPositions.size());
		keylist.addAll(trackPositions.keySet());
		Collections.sort(keylist);
		for(Integer t : keylist) {
			MatroskaCuePosition p = trackPositions.get(t);
			EBMLElement e = p.toEBML();
			if(e != null) me.addChild(e);
		}

		return me;
	}
	
}
