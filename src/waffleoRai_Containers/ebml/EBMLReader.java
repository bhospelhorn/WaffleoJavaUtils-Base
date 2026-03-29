package waffleoRai_Containers.ebml;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class EBMLReader {
	
	private Map<Integer, EBMLFieldDef> knownIds;
	
	public EBMLReader() {
		knownIds = new HashMap<Integer, EBMLFieldDef>();
		EBMLCommon.loadStandardDefinitions(knownIds);
	}
	
	public void addElementDef(EBMLFieldDef def) {
		if(def == null) return;
		knownIds.put(def.baseId, def);
	}
	
	public Map<Integer, EBMLFieldDef> getDefMapReference() {return knownIds;}
	
	private EBMLElement processElement(String filePath, EBMLRawElement raw) {
		EBMLElement e = null;
		EBMLFieldDef def = knownIds.get(raw.getUID());
		if(def != null) {
			switch(def.type) {
			case EBMLCommon.TYPE_ASCII:
				e = raw.asAsciiElement();
				break;
			case EBMLCommon.TYPE_UTF8:
				e = raw.asUnicodeElement();
				break;
			case EBMLCommon.TYPE_INT:
				if(raw.getDataSize() > 4) e = raw.asSignedLongElement();
				else e = raw.asSignedIntElement();
				break;
			case EBMLCommon.TYPE_UINT:
				if(raw.getDataSize() > 3) e = raw.asUnsignedLongElement();
				else e = raw.asUnsignedIntElement();
				break;
			case EBMLCommon.TYPE_FLOAT:
				e = raw.asFloatElement();
				break;
			case EBMLCommon.TYPE_DATE:
				e = raw.asDateElement();
				break;
			case EBMLCommon.TYPE_MASTER:
				EBMLMasterElement me = raw.asMasterElement();
				//Process children...
				List<EBMLElement> mchildren = me.clearChildren();
				for(EBMLElement child : mchildren) {
					if(child instanceof EBMLRawElement) {
						EBMLRawElement rawChild = (EBMLRawElement)child;
						child = processElement(filePath, rawChild);
						rawChild.dispose();
					}
					me.addChild(child);
				}
				e = me;
				break;
			default:
				//Blob
				e = raw.externalize(filePath);
				break;
			}
		}
		else {
			//Treat as a blob (external)
			e = raw.externalize(filePath);
		}
		return e;
	}
	
	private void readEBMLNode(String filePath, BufferReference openFile, List<EBMLElement> outlist) {
		//long filePos = openFile.getBufferPosition();
		EBMLRawElement raw = EBMLRawElement.readRawElement(openFile);
		EBMLElement e = processElement(filePath, raw);
		if(e != null) {
			outlist.add(e);
			raw.dispose();
		}
		else outlist.add(raw);
	}
	
	public List<EBMLElement> readEBMLFile(String filePath) throws IOException{
		List<EBMLElement> topElements = new LinkedList<EBMLElement>();
		FileBuffer myFile = FileBuffer.createBuffer(filePath, true);
		BufferReference nowPos = myFile.getReferenceAt(0L);
		while(nowPos.hasRemaining()) {
			readEBMLNode(filePath, nowPos, topElements);
		}
		return topElements;
	}

}
