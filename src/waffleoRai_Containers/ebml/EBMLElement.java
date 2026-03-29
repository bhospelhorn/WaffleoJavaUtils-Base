package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Set;

import waffleoRai_Utils.FileBuffer;

public abstract class EBMLElement {
	protected int UID; //WITHOUT VLQ flags
	
	public int getUID() {return UID;}
	public abstract long getDataSize();
	public void dispose() {}
	
	public boolean isLeaf() {return true;}
	
	public int getSerializedIdSize() {
		return EBMLCommon.calcVLQLength(UID);
	}
	
	public int getSerializedSizeFieldSize() {
		long dataSize = getDataSize();
		return EBMLCommon.calcVLQLength(dataSize);
	}
	
	public long getSerializedTotalSize() {
		long dataSize = getDataSize();
		return dataSize + EBMLCommon.calcVLQLength(UID) + EBMLCommon.calcVLQLength(dataSize);
	}
	
	protected int serializeUIDTo(FileBuffer target) {
		byte[] idser = EBMLCommon.encodeVLQ(UID);
		for(int i = 0; i < idser.length; i++) target.addToFile(idser[i]);
		return idser.length;
	}
	
	protected FileBuffer generateHeader() {
		long dataSize = getDataSize();
		FileBuffer hdr = new FileBuffer(EBMLCommon.calcVLQLength(UID) + EBMLCommon.calcVLQLength(dataSize), true);
		serializeUIDTo(hdr);
		byte[] szser = EBMLCommon.encodeVLQ(dataSize);
		for(int i = 0; i < szser.length; i++) hdr.addToFile(szser[i]);
		return hdr;
	}
	
	public abstract long serializeTo(FileBuffer target);
	
	public long serializeTo(OutputStream target) throws IOException {
		FileBuffer buff = serializeMe();
		long sz = buff.getFileSize();
		buff.writeToStream(target);
		buff.dispose();
		return sz;
	}
	
	public FileBuffer serializeMe() {
		int alloc = (int)getSerializedTotalSize();
		FileBuffer buff = new FileBuffer(alloc, true);
		this.serializeTo(buff);
		return buff;
	}
	
	public abstract void writeXMLNode(Writer output, EBMLFieldDef def, EBML_XMLWriterState state) throws IOException;
	
	public void getAllFilePaths(Set<String> pathSet) {}
	
}
