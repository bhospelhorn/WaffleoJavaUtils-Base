package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;

public class MatroskaTrackPlane {
	
	public static final int EBML_BASE_ID = 0x64;
	
	private static final int EBML_ID_UID = 0x65;
	private static final int EBML_ID_TYPE = 0x66;
	
	public static final int PLANE_TYPE_LEFT_EYE = 0;
	public static final int PLANE_TYPE_RIGHT_EYE = 1;
	public static final int PLANE_TYPE_BACKGROUND = 2;
	
	private int planeUID = -1;
	private int planeType = -1;
	
	/*----- Getters -----*/

	public int getPlaneUID(){return planeUID;}
	public int getPlaneType(){return planeType;}

	/*----- Setters -----*/

	public void setPlaneUID(int value){planeUID = value;}
	public void setPlaneType(int value){planeType = value;}
	
	/*----- Read -----*/
	
	public static MatroskaTrackPlane fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaTrackPlane item = new MatroskaTrackPlane();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_UID);
		if(e == null) return null;
		item.planeUID = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_TYPE);
		if(e == null) return null;
		item.planeType = EBMLCommon.readIntElement(e);
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(planeUID < 0) return null;
		if(planeType < 0) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLIntElement(EBML_ID_UID, planeUID, false));
		me.addChild(new EBMLIntElement(EBML_ID_TYPE, planeType, false));

		return me;
	}

}
