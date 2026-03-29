package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLBigIntElement;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLExtBlobElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLStringElement;
import waffleoRai_Files.tree.FileNode;

public class MatroskaAttachment {
	
	public static final int EBML_BASE_ID = 0x21a7;
	
	private static final int EBML_ID_DESC = 0x67e;
	private static final int EBML_ID_NAME = 0x66e;
	private static final int EBML_ID_TYPE = 0x660;
	private static final int EBML_ID_DATA = 0x65c;
	private static final int EBML_ID_UID = 0x6ae;
	
	private String fileDesc; //Optional
	private String fileName;
	private String fileMediaType;
	private FileNode fileDataRef;
	private long fileUID;
	
	/*----- Getters -----*/

	public String getFileDesc(){return fileDesc;}
	public String getFileName(){return fileName;}
	public String getFileMediaType(){return fileMediaType;}
	public FileNode getFileDataRef(){return fileDataRef;}
	public long getFileUID(){return fileUID;}

	/*----- Setters -----*/

	public void setFileDesc(String value){fileDesc = value;}
	public void setFileName(String value){fileName = value;}
	public void setFileMediaType(String value){fileMediaType = value;}
	public void setFileDataRef(FileNode value){fileDataRef = value;}
	public void setFileUID(long value){fileUID = value;}
	
	/*----- Read -----*/
	
	public static MatroskaAttachment fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaAttachment item = new MatroskaAttachment();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_NAME);
		if(e != null) {
			if(e instanceof EBMLStringElement) {
				EBMLStringElement ee = (EBMLStringElement)e;
				item.fileName = ee.getValue();
			}
		}
		
		e = me.getFirstChildWithId(EBML_ID_TYPE);
		if(e != null) {
			if(e instanceof EBMLStringElement) {
				EBMLStringElement ee = (EBMLStringElement)e;
				item.fileMediaType = ee.getValue();
			}
		}
		
		e = me.getFirstChildWithId(EBML_ID_DESC);
		if(e != null) {
			if(e instanceof EBMLStringElement) {
				EBMLStringElement ee = (EBMLStringElement)e;
				item.fileDesc = ee.getValue();
			}
		}
		
		e = me.getFirstChildWithId(EBML_ID_UID);
		if(e != null) {
			if(e instanceof EBMLIntElement) {
				EBMLIntElement ee = (EBMLIntElement)e;
				item.fileUID = Integer.toUnsignedLong(ee.getValue());
			}
			else if(e instanceof EBMLBigIntElement) {
				EBMLBigIntElement ee = (EBMLBigIntElement)e;
				item.fileUID = ee.getValue();
			}
		}
		
		e = me.getFirstChildWithId(EBML_ID_DATA);
		if(e != null) {
			if(e instanceof EBMLExtBlobElement) {
				EBMLExtBlobElement ee = (EBMLExtBlobElement)e;
				item.fileDataRef = ee.getSourceReference();
			}
		}
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(fileDataRef == null) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		if((fileDesc != null) && !fileDesc.isEmpty()) {
			me.addChild(new EBMLStringElement(EBML_ID_DESC, fileDesc, true));
		}
		
		if(fileName == null) fileName = "<NULL>";
		me.addChild(new EBMLStringElement(EBML_ID_NAME, fileName, true));
		
		if(fileMediaType == null) fileMediaType = "<NULL>";
		me.addChild(new EBMLStringElement(EBML_ID_TYPE, fileMediaType, false));
		
		me.addChild(new EBMLExtBlobElement(EBML_ID_DATA, fileDataRef));
		
		if((fileUID & ~0xffffffffL) != 0L) {
			me.addChild(new EBMLBigIntElement(EBML_ID_UID, fileUID, false));
		}
		else {
			me.addChild(new EBMLIntElement(EBML_ID_UID, (int)fileUID, false));
		}
		
		return me;
	}

}
