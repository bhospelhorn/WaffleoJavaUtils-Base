package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Set;

import waffleoRai_Files.tree.FileNode;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.MultiFileBuffer;

public class EBMLExtBlobElement extends EBMLElement{

	private FileNode source;
	
	public EBMLExtBlobElement(int elementId, String path, long startPos, long len) {
		super.UID = elementId;
		source = new FileNode(null, String.format("element_%x", elementId));
		source.setSourcePath(path);
		source.setOffset(startPos);
		source.setLength(len);
	}
	
	public EBMLExtBlobElement(int elementId, FileNode node) {
		super.UID = elementId;
		source = node;
	}
	
	public FileNode getSourceReference() {return source;}

	public long getDataSize() {
		return source.getLength();
	}

	public void dispose() {
		source.dispose();
	}

	public long serializeTo(FileBuffer target) {
		try {
			FileBuffer mydata = source.loadDecompressedData();
			if(mydata != null) {
				FileBuffer hdr = generateHeader();
				target.addToFile(hdr);
				target.addToFile(mydata);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return 0L;
	}
	
	public long serializeTo(OutputStream target) throws IOException {
		FileBuffer mydata = source.loadDecompressedData();
		long sz = 0L;
		if(mydata != null) {
			sz = mydata.getFileSize();
			FileBuffer hdr = generateHeader();
			sz += hdr.getFileSize();
			hdr.writeToStream(target);
			mydata.writeToStream(target);
			
			hdr.dispose();
			mydata.dispose();
		}
		
		return sz;
	}
	
	public FileBuffer serializeMe() {
		MultiFileBuffer buff = new MultiFileBuffer(2);
		
		FileBuffer hdr = generateHeader();
		buff.addToFile(hdr);
		try {
			FileBuffer mydata = source.loadDecompressedData();
			if(mydata != null) {
				buff.addToFile(hdr);
				buff.addToFile(mydata);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
		
		return buff;
	}
	
	public void writeXMLNode(Writer output, EBMLFieldDef def, EBML_XMLWriterState state) throws IOException{
		if(output == null) return;
		if(state == null) return;
		String fmtStr = "REF";
		String elId = null;
		if(def != null) {
			elId = def.stringId;
			if(!def.fmtPref.equals("Defo")) fmtStr = def.fmtPref;
		}
		else elId = String.format("EBML%X", UID);
		
		if(fmtStr.equals("REF")) {
			output.write(state.indent + "<" + elId);
			String fpath = state.filepathLookup.get(source.getSourcePath());
			if(fpath == null) fpath = source.getSourcePath();
			long offset = source.getOffset();
			long len = source.getLength();
			output.write(String.format(" Source=\"%s\"", fpath));
			output.write(String.format(" Position=\"0x%x\"", offset));
			output.write(String.format(" End=\"0x%x\"", (offset + len)));
			output.write(String.format(" Length=\"0x%x\"", len));
			output.write("/>\n");
		}
		else if(fmtStr.equals("BIN")) {
			output.write(state.indent + "<" + elId + ">\"");
			//Raw data
			boolean first = true;
			FileBuffer data = source.loadDecompressedData();
			BufferReference ref = data.getReferenceAt(0L);
			while(ref.hasRemaining()) {
				if(!first) output.write(String.format(" %02x", ref.nextByte()));
				else {
					first = false;
					output.write(String.format("%02x", ref.nextByte()));
				}
			}
			output.write("\"</" + elId + ">\n");
		}
	}
	
	public void getAllFilePaths(Set<String> pathSet) {
		pathSet.add(source.getSourcePath());
	}
	
}
