package waffleoRai_Containers.ebml;

public class EBMLFieldDef {
	
	public int type = EBMLCommon.TYPE_UNK;
	public int baseId = -1; //WITHOUT VLQ flags
	public String stringId = null;
	
	public int minVer = -1;
	public boolean multOk = false;
	public boolean optional = true;
	
	public String fmtPref = "Defo";
	
	public EBMLFieldDef() {}
	public EBMLFieldDef(int fieldType, int intId, String strId, int minVersion, boolean multipleOkay, boolean isOptional) {
		type = fieldType;
		baseId = intId;
		stringId = strId;
		minVer = minVersion;
		multOk = multipleOkay;
		optional = isOptional;
	}

}
