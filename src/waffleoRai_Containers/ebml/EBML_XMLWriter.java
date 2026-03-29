package waffleoRai_Containers.ebml;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EBML_XMLWriter {
	
	private Map<Integer, EBMLFieldDef> knownIds;
	private EBML_XMLWriterState state;
	
	public EBML_XMLWriter() {
		knownIds = new HashMap<Integer, EBMLFieldDef>();
		EBMLCommon.loadStandardDefinitions(knownIds);
		state = new EBML_XMLWriterState();
		state.defMap = knownIds;
	}
	
	public void addElementDef(EBMLFieldDef def) {
		if(def == null) return;
		knownIds.put(def.baseId, def);
	}
	
	public void writeToXML(List<EBMLElement> topLevel, Writer output) throws IOException {
		if(output == null) return;
		if(topLevel == null) return;
		
		//Generate a filepath table for external blobs
		Set<String> pathSet = new HashSet<String>();
		for(EBMLElement e : topLevel) e.getAllFilePaths(pathSet);
		Map<String, String> pathBackmap = new HashMap<String, String>();
		for(String p : pathSet) {
			String basename = p;
			if(p.contains(File.separator)) {
				basename = p.substring(p.lastIndexOf(File.separatorChar) + 1);
			}
			String pathkey = basename;
			int i = 0;
			while(pathBackmap.containsKey(pathkey)) {
				pathkey = basename + "_" + i++;
			}
			pathBackmap.put(pathkey, p);
			state.filepathLookup.put(p, pathkey);
		}
		
		//Write header
		output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		output.write("<EBMLFile>\n");
		
		//Write EBML data
		output.write("\t<EBMLFileContents>\n");
		state.indent = "\t\t";
		for(EBMLElement element : topLevel) {
			element.writeXMLNode(output, knownIds.get(element.getUID()), state);
		}
		output.write("\t</EBMLFileContents>\n");
		
		//Write reference table, if applicable
		if(!pathBackmap.isEmpty()) {
			output.write("\t<FileReferences>\n");
			List<String> keylist = new ArrayList<String>(pathBackmap.size());
			keylist.addAll(pathBackmap.keySet());
			Collections.sort(keylist);
			for(String key : keylist) {
				String val = pathBackmap.get(key);
				output.write(String.format("\t\t<FileReference Key=\"%s\" FullPath=\"%s\"/>\n", key, val));
			}
			output.write("\t</FileReferences>\n");
		}
		
		output.write("</EBMLFile>\n");
	}

}
