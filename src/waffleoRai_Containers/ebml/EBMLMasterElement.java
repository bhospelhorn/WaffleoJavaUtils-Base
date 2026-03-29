package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import waffleoRai_DataContainers.MultiValMap;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.MultiFileBuffer;

public class EBMLMasterElement extends EBMLElement{
	
	private List<EBMLElement> children;
	private MultiValMap<Integer, EBMLElement> childrenMapped;
	
	public EBMLMasterElement(int uid) {
		super.UID = uid;
		childrenMapped = new MultiValMap<Integer, EBMLElement>();
		children = new LinkedList<EBMLElement>();
	}
	
	public boolean isLeaf() {return children.isEmpty();}
	
	public long getDataSize() { 
		long val = 0L;
		for(EBMLElement child : children) {
			val += child.getSerializedTotalSize();
		}
		return val;
	}
	
	public List<EBMLElement> clearChildren(){
		List<EBMLElement> old = children;
		children = new LinkedList<EBMLElement>();
		childrenMapped.clearValues();
		return old;
	}
	
	public List<EBMLElement> getChildrenWithId(int elementId){
		return childrenMapped.getValues(elementId);
	}
	
	public EBMLElement getFirstChildWithId(int elementId) {
		return childrenMapped.getFirstValueWithKey(elementId);
	}
	
	public void addChild(EBMLElement child) {
		if(child == null) return;
		children.add(child);
		childrenMapped.addValue(child.getUID(), child);
	}
	
	public long serializeTo(FileBuffer target) {
		long stSize = target.getFileSize();
		
		FileBuffer hdr = generateHeader();
		target.addToFile(hdr);
		
		for(EBMLElement child : children) {
			FileBuffer cbuff = child.serializeMe();
			target.addToFile(cbuff);
		}
		
		return target.getFileSize() - stSize;
	}
	
	public long serializeTo(OutputStream target) throws IOException {
		FileBuffer hdr = generateHeader();
		hdr.writeToStream(target);
		long sz = hdr.getFileSize();
		hdr.dispose();
		
		for(EBMLElement child : children) {
			sz += child.serializeTo(target);
		}
		
		return sz;
	}
	
	public FileBuffer serializeMe() {
		int childCount = children.size();
		MultiFileBuffer buff = new MultiFileBuffer(childCount+1);
		
		//Header
		FileBuffer hdr = generateHeader();
		buff.addToFile(hdr);
		
		for(EBMLElement child : children) {
			FileBuffer cbuff = child.serializeMe();
			buff.addToFile(cbuff);
		}
		
		return buff;
	}
	
	public void dispose() {
		List<EBMLElement> old = clearChildren();
		for(EBMLElement e : old) {
			e.dispose();
		}
	}
	
	public void writeXMLNode(Writer output, EBMLFieldDef def, EBML_XMLWriterState state) throws IOException{
		if(output == null) return;
		if(state == null) return;
		
		String elId = null;
		if(def != null) elId = def.stringId;
		else elId = String.format("EBML%X", UID);
		
		String myIndent = state.indent;
		state.indent += "\t";
		
		output.write(myIndent + "<" + elId + ">\n");
		for(EBMLElement child : children) {
			EBMLFieldDef cdef = null;
			if(state.defMap != null) cdef = state.defMap.get(child.getUID());
			child.writeXMLNode(output, cdef, state);
		}
		output.write(myIndent + "</" + elId + ">\n");
		state.indent = myIndent;
	}
	
	public void getAllFilePaths(Set<String> pathSet) {
		for(EBMLElement child : children) {
			child.getAllFilePaths(pathSet);
		}
	}

}
