package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLFloatElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;

public class MatroskaVideoMasteringMeta {
	
	public static final int EBML_BASE_ID = 0x15d0;
	
	private static final int EBML_ID_PRCX = 0x15d1;
	private static final int EBML_ID_PRCY = 0x15d2;
	private static final int EBML_ID_PGCX = 0x15d3;
	private static final int EBML_ID_PGCY = 0x15d4;
	private static final int EBML_ID_PBCX = 0x15d5;
	private static final int EBML_ID_PBCY = 0x15d6;
	private static final int EBML_ID_WPCX = 0x15d7;
	private static final int EBML_ID_WPCY = 0x15d8;
	private static final int EBML_ID_LUM_MAX = 0x15d9;
	private static final int EBML_ID_LUM_MIN = 0x15da;
	
	private double chromRX = Double.NaN;
	private double chromRY = Double.NaN;
	private double chromGX = Double.NaN;
	private double chromGY = Double.NaN;
	private double chromBX = Double.NaN;
	private double chromBY = Double.NaN;
	private double chromWPX = Double.NaN;
	private double chromWPY = Double.NaN;
	private double lumMax = Double.NaN;
	private double lumMin = Double.NaN;
	
	private boolean dblPrec = true;
	
	/*----- Getters -----*/

	public double getChromRX(){return chromRX;}
	public double getChromRY(){return chromRY;}
	public double getChromGX(){return chromGX;}
	public double getChromGY(){return chromGY;}
	public double getChromBX(){return chromBX;}
	public double getChromBY(){return chromBY;}
	public double getChromWPX(){return chromWPX;}
	public double getChromWPY(){return chromWPY;}
	public double getLumMax(){return lumMax;}
	public double getLumMin(){return lumMin;}

	/*----- Setters -----*/

	public void setChromRX(double value){chromRX = value;}
	public void setChromRY(double value){chromRY = value;}
	public void setChromGX(double value){chromGX = value;}
	public void setChromGY(double value){chromGY = value;}
	public void setChromBX(double value){chromBX = value;}
	public void setChromBY(double value){chromBY = value;}
	public void setChromWPX(double value){chromWPX = value;}
	public void setChromWPY(double value){chromWPY = value;}
	public void setLumMax(double value){lumMax = value;}
	public void setLumMin(double value){lumMin = value;}
	public void setSerializeDouble(boolean b) {dblPrec = b;}
	
	/*----- Read -----*/
	
	public static MatroskaVideoMasteringMeta fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaVideoMasteringMeta item = new MatroskaVideoMasteringMeta();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_PRCX);
		item.chromRX = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_PRCY);
		item.chromRY = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_PGCX);
		item.chromGX = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_PGCY);
		item.chromGY = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_PBCX);
		item.chromBX = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_PBCY);
		item.chromBY = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_WPCX);
		item.chromWPX = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_WPCY);
		item.chromWPY = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_LUM_MAX);
		item.lumMax = EBMLCommon.readFloatElement(e);
		e = me.getFirstChildWithId(EBML_ID_LUM_MIN);
		item.lumMin = EBMLCommon.readFloatElement(e);
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		if(!Double.isNaN(chromRX)) me.addChild(new EBMLFloatElement(EBML_ID_PRCX, chromRX, dblPrec));
		if(!Double.isNaN(chromRY)) me.addChild(new EBMLFloatElement(EBML_ID_PRCY, chromRY, dblPrec));
		if(!Double.isNaN(chromGX)) me.addChild(new EBMLFloatElement(EBML_ID_PGCX, chromGX, dblPrec));
		if(!Double.isNaN(chromGY)) me.addChild(new EBMLFloatElement(EBML_ID_PGCY, chromGY, dblPrec));
		if(!Double.isNaN(chromBX)) me.addChild(new EBMLFloatElement(EBML_ID_PBCX, chromBX, dblPrec));
		if(!Double.isNaN(chromBY)) me.addChild(new EBMLFloatElement(EBML_ID_PBCY, chromBY, dblPrec));
		if(!Double.isNaN(chromWPX)) me.addChild(new EBMLFloatElement(EBML_ID_WPCX, chromWPX, dblPrec));
		if(!Double.isNaN(chromWPY)) me.addChild(new EBMLFloatElement(EBML_ID_WPCY, chromWPY, dblPrec));
		if(!Double.isNaN(lumMax)) me.addChild(new EBMLFloatElement(EBML_ID_LUM_MAX, lumMax, dblPrec));
		if(!Double.isNaN(lumMin)) me.addChild(new EBMLFloatElement(EBML_ID_LUM_MIN, lumMin, dblPrec));

		return me;
	}
	

}
