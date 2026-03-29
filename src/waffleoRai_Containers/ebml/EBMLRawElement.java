package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.MultiFileBuffer;

public class EBMLRawElement extends EBMLElement{
	
	private FileBuffer blob;
	private long srcDataPos = -1L; //bookkeeping
	
 	private EBMLRawElement(int elementId) {
		super.UID = elementId;
	}
 	
 	public EBMLRawElement(int elementId, byte[] data) {
 		super.UID = elementId;
 		blob = FileBuffer.wrap(data);
 	}
	
	public long getDataSize() {
		if(blob == null) return 0L;
		return blob.getFileSize();
	}
	
	public static EBMLRawElement readRawElement(BufferReference data) {
		int id = EBMLCommon.parseNextVLQ(data);
		long sz = EBMLCommon.parseNextLongVLQ(data);
		EBMLRawElement e = new EBMLRawElement(id);
		FileBuffer backingBuffer = data.getBuffer();
		long mypos = data.getBufferPosition();
		e.blob = backingBuffer.createReadOnlyCopy(mypos, mypos + sz);
		e.srcDataPos = mypos;
		data.add(sz);
		return e;
	}
	
	public long serializeTo(FileBuffer target) {
		FileBuffer hdr = generateHeader();
		long sz = hdr.getFileSize();
		target.addToFile(hdr);
		
		if(blob != null) {
			target.addToFile(blob);
			sz += blob.getFileSize();
		}
		
		return sz;
	}
	
	public long serializeTo(OutputStream target) throws IOException {
		FileBuffer hdr = generateHeader();
		long sz = hdr.getFileSize();
		hdr.writeToStream(target);
		
		if(blob != null) sz += blob.writeToStream(target);
		
		return sz;
	}
	
	public FileBuffer serializeMe() {
		MultiFileBuffer buff = new MultiFileBuffer(2);
		
		FileBuffer hdr = generateHeader();
		buff.addToFile(hdr);
		if(blob != null) buff.addToFile(blob);
		
		return buff;
	}
	
	public void dispose() {
		if(blob != null) {
			try {blob.dispose();} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public EBMLIntElement asUnsignedIntElement() {
		int value = 0;
		if(blob != null) {
			int sz = (int)blob.getFileSize();
			switch(sz){
			case 1:value = Byte.toUnsignedInt(blob.getByte(0L)); break;
			case 2: value = Short.toUnsignedInt(blob.shortFromFile(0L)); break;
			case 3: value = blob.shortishFromFile(0L); break;
			case 4: value = blob.intFromFile(0L); break;
			default:
				return null;
			}
		}
		return new EBMLIntElement(UID, value, false);
	}
	
	public EBMLIntElement asSignedIntElement() {
		int value = 0;
		if(blob != null) {
			int sz = (int)blob.getFileSize();
			switch(sz){
			case 1:value = (int)blob.getByte(0L); break;
			case 2: value = (int)blob.shortFromFile(0L); break;
			case 3: 
				value = blob.shortishFromFile(0L);
				value <<= 8;
				value >>= 8;
				break;
			case 4: value = blob.intFromFile(0L); break;
			default:
				return null;
			}
		}
		return new EBMLIntElement(UID, value, true);
	}
	
	public EBMLBigIntElement asUnsignedLongElement() {
		long value = 0;
		if(blob != null) {
			blob.setCurrentPosition(0L);
			int sz = (int)blob.getFileSize();
			for(int i = 0; i < sz; i++) {
				value <<= 8;
				value |= Byte.toUnsignedLong(blob.nextByte());
			}
		}
		return new EBMLBigIntElement(UID, value, false);
	}
	
	public EBMLBigIntElement asSignedLongElement() {
		long value = 0;
		if(blob != null) {
			byte b0 = blob.getByte(0L);
			blob.setCurrentPosition(0L);
			int sz = (int)blob.getFileSize();
			if(b0 < 0) value = ~0L;
			for(int i = 0; i < sz; i++) {
				value <<= 8;
				value |= Byte.toUnsignedLong(blob.nextByte());
			}
		}
		return new EBMLBigIntElement(UID, value, false);
	}

	public EBMLFloatElement asFloatElement() {
		double value = Double.NaN;
		boolean isdbl = false;
		if(blob != null) {
			isdbl = blob.getFileSize() > 4;
			if(isdbl) value = Double.longBitsToDouble(blob.longFromFile(0L));
			else value = (double)Float.intBitsToFloat(blob.intFromFile(0L));
		}
		return new EBMLFloatElement(UID, value, isdbl);
	}
	
	public EBMLStringElement asAsciiElement() {
		String value = "";
		if(blob != null) {
			value = blob.getASCII_string(0L, (int)blob.getFileSize());
		}
		return new EBMLStringElement(UID, value, false);
	}
	
	public EBMLStringElement asUnicodeElement() {
		String value = "";
		if(blob != null) {
			value = blob.readEncoded_string("UTF8", 0L, blob.getFileSize());
		}
		return new EBMLStringElement(UID, value, true);
	}
	
	public EBMLDateElement asDateElement() {
		long rawVal = 0L;
		if(blob != null) {
			rawVal = blob.longFromFile(0L);
		}
		return new EBMLDateElement(UID, rawVal);
	}
	
	public EBMLMasterElement asMasterElement() {
		//Parse blob as a list of raw elements
		EBMLMasterElement me = new EBMLMasterElement(super.UID);
		if(blob != null) {
			BufferReference blobPos = blob.getReferenceAt(0L);
			while(blobPos.hasRemaining()) {
				EBMLRawElement raw = EBMLRawElement.readRawElement(blobPos);
				if(raw != null) {
					raw.srcDataPos += this.srcDataPos;
					me.addChild(raw);
				}
			}
		}
		return me;
	}
	
	public EBMLExtBlobElement externalize() {
		String filepath = null;
		if(blob != null) {
			filepath = blob.getPath();
		}
		return externalize(filepath);
	}
	
	public EBMLExtBlobElement externalize(String srcFilePath) {
		EBMLExtBlobElement ext = new EBMLExtBlobElement(super.UID, srcFilePath, srcDataPos, getDataSize());
		return ext;
	}
	
	public void writeXMLNode(Writer output, EBMLFieldDef def, EBML_XMLWriterState state) throws IOException{
		if(output == null) return;
		if(state == null) return;
		String fmtStr = "BIN";
		String elId = null;
		if(def != null) {
			elId = def.stringId;
			if(!def.fmtPref.equals("Defo")) fmtStr = def.fmtPref;
		}
		else elId = String.format("EBML%X", UID);
		
		if(fmtStr.equals("BIN")) {
			output.write(state.indent + "<" + elId + ">\"");
			//Raw data
			if(blob != null) {
				boolean first = true;
				BufferReference ref = blob.getReferenceAt(0L);
				while(ref.hasRemaining()) {
					if(!first) output.write(String.format(" %02x", ref.nextByte()));
					else {
						first = false;
						output.write(String.format("%02x", ref.nextByte()));
					}
				}	
			}
			output.write("\"</" + elId + ">\n");
		}
	}
	
}
