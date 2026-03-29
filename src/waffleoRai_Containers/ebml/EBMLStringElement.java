package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.io.Writer;

import waffleoRai_Utils.FileBuffer;

public class EBMLStringElement extends EBMLElement{
	
	private boolean isUnicode = true;
	private String sValue;
	private FileBuffer preSerialized;
	
	public EBMLStringElement(int elementId, String value) {
		this(elementId, value, true);
	}
	
	public EBMLStringElement(int elementId, String value, boolean isUtf8) {
		super.UID = elementId;
		sValue = value;
		isUnicode = isUtf8;
	}
	
	public String getValue() {return sValue;}
	public void setValue(String val) {sValue = val; preSerialized = null;}
	
	private void preserializeUTF8() {
		int alloc = sValue.length() << 2;
		FileBuffer buffer = new FileBuffer(alloc, true);
		buffer.addEncoded_string("UTF8", sValue);
	}
	
	public long getDataSize() {
		if(sValue == null) return 0;
		if(!isUnicode) return sValue.length();
		else {
			if(preSerialized == null) preserializeUTF8();
			return preSerialized.getFileSize();
		}
	}
	
	public long serializeTo(FileBuffer target) {
		if(target == null) return 0L;
		long mypos = target.getFileSize();
		super.serializeUIDTo(target);
		long sz = getDataSize();
		byte[] szfield = EBMLCommon.encodeVLQ(sz);
		for(int i = 0; i < szfield.length; i++) target.addToFile(szfield[i]);
		if(isUnicode) {
			for(long i = 0; i < sz; i++) target.addToFile(preSerialized.getByte(i));
		}
		else target.printASCIIToFile(sValue);
		return target.getFileSize() - mypos;
	}
	
	public void dispose() {
		if(preSerialized != null) {
			try {preSerialized.dispose();} 
			catch (IOException e) {e.printStackTrace();}
			preSerialized = null;
		}
	}
	
	public void writeXMLNode(Writer output, EBMLFieldDef def, EBML_XMLWriterState state) throws IOException{
		if(output == null) return;
		if(state == null) return;
		String fmtStr = "%s";
		String elId = null;
		if(def != null) {
			elId = def.stringId;
			if(!def.fmtPref.equals("Defo")) fmtStr = def.fmtPref;
		}
		else elId = String.format("EBML%X", UID);
		
		if(state.stringValAsAttr) {
			output.write(String.format(state.indent + "<%s Value=\"" + fmtStr + "\"/>\n", elId, sValue));	
		}
		else {
			output.write(String.format(state.indent + "<%s>" + fmtStr + "</%s>\n", elId, sValue, elId));
		}
	}

}
