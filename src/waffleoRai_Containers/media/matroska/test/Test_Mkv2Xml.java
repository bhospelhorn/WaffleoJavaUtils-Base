package waffleoRai_Containers.media.matroska.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLFieldDef;
import waffleoRai_Containers.ebml.EBMLReader;
import waffleoRai_Containers.ebml.EBML_XMLWriter;
import waffleoRai_Containers.media.MatroskaCommon;

public class Test_Mkv2Xml {

	public static void main(String[] args) {
		String inpath = args[0];
		String outdir = args[1];
		
		try {
			Map<Integer, EBMLFieldDef> defMap = new HashMap<Integer, EBMLFieldDef>();
			MatroskaCommon.loadMatroskaDefs(defMap);
			
			EBMLReader reader = new EBMLReader();
			for(EBMLFieldDef def : defMap.values()) reader.addElementDef(def);
			List<EBMLElement> top = reader.readEBMLFile(inpath);
			
			Path inp = Paths.get(inpath);
			String inname = inp.getFileName().toString();
			if(inname.contains(".")) {
				inname = inname.substring(0, inname.lastIndexOf('.'));
			}
			
			//TODO later add ability to export blobs?
			String outxml = outdir + File.separator + inname + ".xml";
			EBML_XMLWriter writer = new EBML_XMLWriter();
			for(EBMLFieldDef def : defMap.values()) writer.addElementDef(def);
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(outxml));
			writer.writeToXML(top, bw);
			bw.close();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		
	}

}
