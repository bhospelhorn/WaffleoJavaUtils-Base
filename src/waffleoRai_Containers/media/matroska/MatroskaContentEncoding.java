package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLRawElement;

public class MatroskaContentEncoding {
	
	public static final int EBML_BASE_ID = 0x2240;
	
	private static final int EBML_ID_ORDER = 0x1031;
	private static final int EBML_ID_SCOPE = 0x1032;
	private static final int EBML_ID_TYPE = 0x1033;
	private static final int EBML_ID_COMPRESSION = 0x1034;
	private static final int EBML_ID_ENCRYPTION = 0x1035;
	
	private static final int EBML_ID_C_ALGO = 0x254;
	private static final int EBML_ID_C_SETTINGS = 0x255;
	
	private static final int EBML_ID_E_ALGO = 0x7e1;
	private static final int EBML_ID_E_KEYID = 0x7e2;
	private static final int EBML_ID_E_AES_SETTINGS = 0x7e7;
	private static final int EBML_ID_E_AES_CMODE = 0x7e8;
	
	private int order = -1;
	private int scope = -1;
	private int type = -1;
	
	//Compression
	private int compressionAlgo = -1;
	private byte[] compressionSettings;
	
	//Encryption
	private int encryptionAlgo = -1;
	private byte[] encryptionKeyID;
	private int aesCipherMode = -1;
	
	/*----- Getters -----*/

	public int getOrder(){return order;}
	public int getScope(){return scope;}
	public int getType(){return type;}
	public int getCompressionAlgo(){return compressionAlgo;}
	public byte[] getCompressionSettings(){return compressionSettings;}
	public int getEncryptionAlgo(){return encryptionAlgo;}
	public byte[] getEncryptionKeyID(){return encryptionKeyID;}
	public int getAesCipherMode(){return aesCipherMode;}

	/*----- Setters -----*/

	public void setOrder(int value){order = value;}
	public void setScope(int value){scope = value;}
	public void setType(int value){type = value;}
	public void setCompressionAlgo(int value){compressionAlgo = value;}
	public void setCompressionSettings(byte[] value){compressionSettings = value;}
	public void setEncryptionAlgo(int value){encryptionAlgo = value;}
	public void setEncryptionKeyID(byte[] value){encryptionKeyID = value;}
	public void setAesCipherMode(int value){aesCipherMode = value;}
	
	/*----- Read -----*/
	
	public static MatroskaContentEncoding fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaContentEncoding item = new MatroskaContentEncoding();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_ORDER);
		if(e == null) return null;
		item.order = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_SCOPE);
		if(e == null) return null;
		item.scope = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_TYPE);
		if(e == null) return null;
		item.type = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_COMPRESSION);
		if((e != null) && (e instanceof EBMLMasterElement)) {
			EBMLMasterElement cme = (EBMLMasterElement) e;
			e = cme.getFirstChildWithId(EBML_ID_C_ALGO);
			if(e != null) {
				item.compressionAlgo = EBMLCommon.readIntElement(e);
				
				e = cme.getFirstChildWithId(EBML_ID_C_SETTINGS);
				item.compressionSettings = EBMLCommon.loadBlobElement(e);
			}
		}
		
		e = me.getFirstChildWithId(EBML_ID_ENCRYPTION);
		if((e != null) && (e instanceof EBMLMasterElement)) {
			EBMLMasterElement cme = (EBMLMasterElement) e;
			e = cme.getFirstChildWithId(EBML_ID_E_ALGO);
			if(e != null) {
				item.encryptionAlgo = EBMLCommon.readIntElement(e);
				
				e = cme.getFirstChildWithId(EBML_ID_E_KEYID);
				item.encryptionKeyID = EBMLCommon.loadBlobElement(e);
				
				e = cme.getFirstChildWithId(EBML_ID_E_AES_SETTINGS);
				if((e != null) && (e instanceof EBMLMasterElement)) {
					EBMLMasterElement gcme = (EBMLMasterElement) e;
					e = gcme.getFirstChildWithId(EBML_ID_E_AES_CMODE);
					item.aesCipherMode = EBMLCommon.readIntElement(e);
				}
			}
		}
		
		return item;
	}

	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(order < 0) return null;
		if(scope < 0) return null;
		if(type < 0) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLIntElement(EBML_ID_ORDER, order, false));
		me.addChild(new EBMLIntElement(EBML_ID_SCOPE, scope, false));
		me.addChild(new EBMLIntElement(EBML_ID_TYPE, type, false));
		
		if(compressionAlgo >= 0) {
			EBMLMasterElement cme = new EBMLMasterElement(EBML_ID_COMPRESSION);
			cme.addChild(new EBMLIntElement(EBML_ID_C_ALGO, compressionAlgo, false));
			if(compressionSettings != null) {
				cme.addChild(new EBMLRawElement(EBML_ID_C_SETTINGS, compressionSettings));
			}
			me.addChild(cme);
		}
		
		if(encryptionAlgo >= 0) {
			EBMLMasterElement cme = new EBMLMasterElement(EBML_ID_ENCRYPTION);
			cme.addChild(new EBMLIntElement(EBML_ID_E_ALGO, encryptionAlgo, false));
			if(encryptionKeyID != null) {
				cme.addChild(new EBMLRawElement(EBML_ID_E_KEYID, encryptionKeyID));
			}
			if(aesCipherMode >= 0) {
				EBMLMasterElement gcme = new EBMLMasterElement(EBML_ID_E_AES_SETTINGS);
				gcme.addChild(new EBMLIntElement(EBML_ID_E_AES_CMODE, aesCipherMode, false));
				cme.addChild(gcme);
			}
			me.addChild(cme);
		}

		return me;
	}
	
}
