package waffleoRai_Containers.media;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLFieldDef;

public class MatroskaCommon {
	
	public static final int BLOCKLACE_NONE = 0;
	public static final int BLOCKLACE_XIPH = 1;
	public static final int BLOCKLACE_FIXED = 2;
	public static final int BLOCKLACE_EBML = 3;
	
	public static void loadMatroskaDefs(Map<Integer, EBMLFieldDef> target) throws IOException {
		if(target == null) return;
		
		InputStream input = MatroskaCommon.class.getResourceAsStream("/waffleoRai_Containers/media/matroska/matroska_elements.csv");
		BufferedReader br = new BufferedReader(new InputStreamReader(input));
		EBMLCommon.loadDefinitionCSV(br, target);
		br.close();
	}

}
