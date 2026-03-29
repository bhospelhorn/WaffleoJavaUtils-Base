package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.io.Writer;

import waffleoRai_Utils.FileBuffer;

public class EBMLBigIntElement extends EBMLElement{
	
	private boolean isSigned = false;
	private long iValue;
	
	public EBMLBigIntElement(int elementId, long value) {
		this(elementId, value, false);
	}
	
	public EBMLBigIntElement(int elementId, long value, boolean signed) {
		super.UID = elementId;
		iValue = value;
		isSigned = signed;
	}
	
	public boolean isSigned() {return isSigned;}
	public long getValue() {return iValue;}
	public void setValue(long val) {iValue = val;}
	public void setIsSigned(boolean b) {isSigned = b;}
	
	public long getDataSize() {
		if(!isSigned) {
			long upper = 0x100L;
			for(int i = 1; i < 8; i++) {
				if(iValue < upper) return i;
				upper <<= 8;
			}
		}
		else {
			long bound = 0x80L;
			long upper = bound - 1;
			long lower = -bound;
			for(int i = 1; i < 8; i++) {
				if((iValue <= upper) && (iValue >= lower)) return i;
				bound <<= 8;
				upper = bound - 1;
				lower = -bound;
			}
		}
		return 8L;
	}
	
	public int getSerializedSizeFieldSize() {return 1;}
	
	public long getSerializedTotalSize() {
		return getDataSize() + EBMLCommon.calcVLQLength(UID) + 1;
	}
	
	public long serializeTo(FileBuffer target) {
		if(target == null) return 0L;
		int idlen = super.serializeUIDTo(target);
		int mySize = (int)this.getDataSize();
		target.addToFile((byte)(0x80 | mySize));
		int shamt = (mySize - 1) << 3;
		for(int i = 0; i < mySize; i++) {
			target.addToFile((byte)((iValue >>> shamt) & 0xff));
			shamt -= 8;
		}
		
		return idlen + mySize + 1;
	}
	
	public void writeXMLNode(Writer output, EBMLFieldDef def, EBML_XMLWriterState state) throws IOException{
		if(output == null) return;
		if(state == null) return;
		String fmtStr = "%d";
		String elId = null;
		if(def != null) {
			elId = def.stringId;
			if(!def.fmtPref.equals("Defo")) fmtStr = def.fmtPref;
		}
		else elId = String.format("EBML%X", UID);
		
		if(state.smallValAsAttr) {
			output.write(String.format(state.indent + "<%s Value=\"" + fmtStr + "\"/>\n", elId, iValue));	
		}
		else {
			output.write(String.format(state.indent + "<%s>" + fmtStr + "</%s>\n", elId, iValue, elId));
		}
	}

}
