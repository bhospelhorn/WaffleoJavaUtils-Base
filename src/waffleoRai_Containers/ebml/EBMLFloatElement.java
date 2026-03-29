package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.io.Writer;

import waffleoRai_Utils.FileBuffer;

public class EBMLFloatElement extends EBMLElement{
	
	private boolean writeDouble = true;
	private double fValue;
	
	public EBMLFloatElement(int elementId, double value) {
		this(elementId, value, true);
	}
	
	public EBMLFloatElement(int elementId, double value, boolean dbl) {
		super.UID = elementId;
		fValue = value;
		writeDouble = dbl;
	}
	
	public double getValue() {return fValue;}
	public void setValue(double val) {fValue = val;}
	
	public long getDataSize() {
		if(writeDouble) return 8L;
		else return 4L;
	}
	
	public int getSerializedSizeFieldSize() {return 1;}
	
	public long getSerializedTotalSize() {
		int dsz = writeDouble ? 8 : 4;
		return dsz + EBMLCommon.calcVLQLength(UID) + 1;
	}
	
	public long serializeTo(FileBuffer target) {
		if(target == null) return 0L;
		int idlen = super.serializeUIDTo(target);
		if(writeDouble) {
			target.addToFile((byte)0x88);
			target.addToFile(Double.doubleToRawLongBits(fValue));
			return idlen + 1 + 8;
		}
		else {
			target.addToFile((byte)0x84);
			target.addToFile(Float.floatToRawIntBits((float)fValue));
			return idlen + 1 + 4;
		}
	}
	
	public FileBuffer serializeMe() {
		int alloc = writeDouble ? 8 : 4;
		alloc += 1 + EBMLCommon.calcVLQLength(UID);
		FileBuffer buff = new FileBuffer(alloc, true);
		this.serializeTo(buff);
		return buff;
	}
	
	public void writeXMLNode(Writer output, EBMLFieldDef def, EBML_XMLWriterState state) throws IOException{
		if(output == null) return;
		if(state == null) return;
		String fmtStr = "%.3f";
		String elId = null;
		if(def != null) {
			elId = def.stringId;
			if(!def.fmtPref.equals("Defo")) fmtStr = def.fmtPref;
		}
		else elId = String.format("EBML%X", UID);
		
		if(state.smallValAsAttr) {
			output.write(String.format(state.indent + "<%s Value=\"" + fmtStr + "\"/>\n", elId, fValue));	
		}
		else {
			output.write(String.format(state.indent + "<%s>" + fmtStr + "</%s>\n", elId, fValue, elId));
		}
	}

}
