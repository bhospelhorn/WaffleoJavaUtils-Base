package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLFloatElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLRawElement;

public class MatroskaProjectionInfo {
	
	public static final int EBML_BASE_ID = 0x3670;
	
	private static final int EBML_ID_TYPE = 0x3671;
	private static final int EBML_ID_PRIV = 0x3672;
	private static final int EBML_ID_YAW = 0x3673;
	private static final int EBML_ID_PITCH = 0x3674;
	private static final int EBML_ID_ROLL = 0x3675;

	private int projType = -1;
	private byte[] internalData;
	private double poseYaw = Double.NaN;
	private double posePitch = Double.NaN;
	private double poseRoll = Double.NaN;
	
	/*----- Getters -----*/

	public int getProjType(){return projType;}
	public byte[] getInternalData(){return internalData;}
	public double getPoseYaw(){return poseYaw;}
	public double getPosePitch(){return posePitch;}
	public double getPoseRoll(){return poseRoll;}

	/*----- Setters -----*/

	public void setProjType(int value){projType = value;}
	public void setInternalData(byte[] value){internalData = value;}
	public void setPoseYaw(double value){poseYaw = value;}
	public void setPosePitch(double value){posePitch = value;}
	public void setPoseRoll(double value){poseRoll = value;}
	
	/*----- Read -----*/
	
	public static MatroskaProjectionInfo fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaProjectionInfo item = new MatroskaProjectionInfo();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_TYPE);
		if(e == null) return null;
		item.projType = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_YAW);
		if(e == null) return null;
		item.poseYaw = EBMLCommon.readFloatElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_PITCH);
		if(e == null) return null;
		item.posePitch = EBMLCommon.readFloatElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_ROLL);
		if(e == null) return null;
		item.poseRoll = EBMLCommon.readFloatElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_PRIV);
		item.internalData = EBMLCommon.loadBlobElement(e);
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(projType < 0) return null;
		if(Double.isNaN(poseYaw)) return null;
		if(Double.isNaN(posePitch)) return null;
		if(Double.isNaN(poseRoll)) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLIntElement(EBML_ID_TYPE, projType, false));
		
		if(internalData != null) {
			me.addChild(new EBMLRawElement(EBML_ID_PRIV, internalData));
		}
		
		me.addChild(new EBMLFloatElement(EBML_ID_YAW, poseYaw, true));
		me.addChild(new EBMLFloatElement(EBML_ID_PITCH, posePitch, true));
		me.addChild(new EBMLFloatElement(EBML_ID_ROLL, poseRoll, true));
		
		return me;
	}
	
}
