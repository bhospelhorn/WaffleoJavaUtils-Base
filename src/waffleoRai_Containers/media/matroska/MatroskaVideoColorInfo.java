package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;

public class MatroskaVideoColorInfo {
	
	public static final int EBML_BASE_ID = 0x15b0;
	
	private static final int EBML_ID_MTX_COEFF = 0x15b1;
	private static final int EBML_ID_BITSPERCH = 0x15b2;
	private static final int EBML_ID_CHRSUBH = 0x15b3;
	private static final int EBML_ID_CHRSUBV = 0x15b4;
	private static final int EBML_ID_CBSUBH = 0x15b5;
	private static final int EBML_ID_CBSUBV = 0x15b6;
	private static final int EBML_ID_CHRSITH = 0x15b7;
	private static final int EBML_ID_CHRSITV = 0x15b8;
	private static final int EBML_ID_RANGE = 0x15b9;
	private static final int EBML_ID_TXFR = 0x15ba;
	private static final int EBML_ID_PRIMARIES = 0x15bb;
	private static final int EBML_ID_MAXCLL = 0x15bc;
	private static final int EBML_ID_MAXFALL = 0x15bd;
	
	private int matrixCoeff = -1;
	private int bitsPerChannel = -1;
	private int chromaSubHoriz = -1;
	private int chromaSubVert = -1;
	private int cbSubHoriz = -1;
	private int cbSubVert = -1;
	private int chromaSitingHoriz = -1;
	private int chromaSitingVert = -1;
	private int range = -1;
	private int txferChar = -1;
	private int primaries = -1;
	private int maxCLL = -1;
	private int maxFALL = -1;
	
	private MatroskaVideoMasteringMeta masteringMeta;
	
	/*----- Getters -----*/

	public int getMatrixCoeff(){return matrixCoeff;}
	public int getBitsPerChannel(){return bitsPerChannel;}
	public int getChromaSubHoriz(){return chromaSubHoriz;}
	public int getChromaSubVert(){return chromaSubVert;}
	public int getCbSubHoriz(){return cbSubHoriz;}
	public int getCbSubVert(){return cbSubVert;}
	public int getChromaSitingHoriz(){return chromaSitingHoriz;}
	public int getChromaSitingVert(){return chromaSitingVert;}
	public int getRange(){return range;}
	public int getTxferChar(){return txferChar;}
	public int getPrimaries(){return primaries;}
	public int getMaxCLL(){return maxCLL;}
	public int getMaxFALL(){return maxFALL;}
	public MatroskaVideoMasteringMeta getMasteringMeta(){return masteringMeta;}

	/*----- Setters -----*/

	public void setMatrixCoeff(int value){matrixCoeff = value;}
	public void setBitsPerChannel(int value){bitsPerChannel = value;}
	public void setChromaSubHoriz(int value){chromaSubHoriz = value;}
	public void setChromaSubVert(int value){chromaSubVert = value;}
	public void setCbSubHoriz(int value){cbSubHoriz = value;}
	public void setCbSubVert(int value){cbSubVert = value;}
	public void setChromaSitingHoriz(int value){chromaSitingHoriz = value;}
	public void setChromaSitingVert(int value){chromaSitingVert = value;}
	public void setRange(int value){range = value;}
	public void setTxferChar(int value){txferChar = value;}
	public void setPrimaries(int value){primaries = value;}
	public void setMaxCLL(int value){maxCLL = value;}
	public void setMaxFALL(int value){maxFALL = value;}
	public void setMasteringMeta(MatroskaVideoMasteringMeta value){masteringMeta = value;}
	
/*----- Read -----*/
	
	public static MatroskaVideoColorInfo fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaVideoColorInfo item = new MatroskaVideoColorInfo();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_MTX_COEFF);
		if(e == null) return null;
		item.matrixCoeff = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_BITSPERCH);
		if(e == null) return null;
		item.bitsPerChannel = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_CHRSITH);
		if(e == null) return null;
		item.chromaSitingHoriz = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_CHRSITV);
		if(e == null) return null;
		item.chromaSitingVert = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_RANGE);
		if(e == null) return null;
		item.range = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_TXFR);
		if(e == null) return null;
		item.txferChar = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_PRIMARIES);
		if(e == null) return null;
		item.primaries = EBMLCommon.readIntElement(e);

		e = me.getFirstChildWithId(EBML_ID_CHRSUBH);
		item.chromaSubHoriz = EBMLCommon.readIntElement(e);
		e = me.getFirstChildWithId(EBML_ID_CHRSUBV);
		item.chromaSubVert = EBMLCommon.readIntElement(e);
		e = me.getFirstChildWithId(EBML_ID_CBSUBH);
		item.cbSubHoriz = EBMLCommon.readIntElement(e);
		e = me.getFirstChildWithId(EBML_ID_CBSUBV);
		item.cbSubVert = EBMLCommon.readIntElement(e);
		e = me.getFirstChildWithId(EBML_ID_MAXCLL);
		item.maxCLL = EBMLCommon.readIntElement(e);
		e = me.getFirstChildWithId(EBML_ID_MAXFALL);
		item.maxFALL = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(MatroskaVideoMasteringMeta.EBML_BASE_ID);
		if((e != null) && (e instanceof EBMLMasterElement)) {
			item.masteringMeta = MatroskaVideoMasteringMeta.fromEBML(e);
		}
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(matrixCoeff < 0) return null;
		if(bitsPerChannel < 0) return null;
		if(chromaSitingHoriz < 0) return null;
		if(chromaSitingVert < 0) return null;
		if(range < 0) return null;
		if(txferChar < 0) return null;
		if(primaries < 0) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLIntElement(EBML_ID_MTX_COEFF, matrixCoeff, false));
		me.addChild(new EBMLIntElement(EBML_ID_BITSPERCH, bitsPerChannel, false));
		if(chromaSubHoriz >= 0) me.addChild(new EBMLIntElement(EBML_ID_CHRSUBH, chromaSubHoriz, false));
		if(chromaSubVert >= 0) me.addChild(new EBMLIntElement(EBML_ID_CHRSUBV, chromaSubVert, false));
		if(cbSubHoriz >= 0) me.addChild(new EBMLIntElement(EBML_ID_CBSUBH, cbSubHoriz, false));
		if(cbSubVert >= 0) me.addChild(new EBMLIntElement(EBML_ID_CBSUBV, cbSubVert, false));
		me.addChild(new EBMLIntElement(EBML_ID_CHRSITH, chromaSitingHoriz, false));
		me.addChild(new EBMLIntElement(EBML_ID_CHRSITV, chromaSitingVert, false));
		me.addChild(new EBMLIntElement(EBML_ID_RANGE, range, false));
		me.addChild(new EBMLIntElement(EBML_ID_TXFR, txferChar, false));
		me.addChild(new EBMLIntElement(EBML_ID_PRIMARIES, primaries, false));
		if(maxCLL >= 0) me.addChild(new EBMLIntElement(EBML_ID_MAXCLL, maxCLL, false));
		if(maxFALL >= 0) me.addChild(new EBMLIntElement(EBML_ID_MAXFALL, maxFALL, false));
		
		if(this.masteringMeta != null) {
			EBMLElement cme = masteringMeta.toEBML();
			if(cme != null) me.addChild(cme);
		}
		
		return me;
	}

}
