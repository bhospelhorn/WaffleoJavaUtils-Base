package waffleoRai_AutoCode.blockstyle;

import java.io.BufferedWriter;
import java.io.IOException;

import waffleoRai_AutoCode.java.BingenJavaTarget;
import waffleoRai_AutoCode.typedefs.AnonStructFieldDef;
import waffleoRai_AutoCode.typedefs.ArrayFieldDef;
import waffleoRai_AutoCode.typedefs.BasicDataFieldDef;
import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.BitFieldDef;
import waffleoRai_AutoCode.typedefs.DataFieldDef;
import waffleoRai_AutoCode.typedefs.EnumDef;
import waffleoRai_AutoCode.typedefs.ListDef;

//TODO Readers also need if statement added if there is a versince
//	That way field not read if trying to read older versions

public class BlockBgJavaTarget extends BingenJavaTarget{

	private static final boolean DEBUG_MODE = true;
	
	/*----- Constants -----*/
	
	public static final String CONSTNAME_ENUMSIZE = "SIZE_BYTES";
	
	public static final String XMLKEY_VER_START = "VersionSince";
	public static final String XMLKEY_VER_END = "VersionUntil";
	public static final String XMLKEY_BLOCKID = "BlockId";
	
	protected static final String KEY_VERMAJ = "VER_MAJOR";
	protected static final String KEY_VERMIN = "VER_MINOR";
	protected static final String KEY_BLOCKID = "BLOCK_ID";
	
	protected static final String IVARNAME_VERMAJ = "versionMajor";
	protected static final String IVARNAME_VERMIN = "versionMinor";
	
	public static final String FUNCNAME_READER = "readBlockIn";
	public static final String FUNCNAME_WRITER = "writeBlockOut";
	public static final String FUNCNAME_SKIP = "skipRead";
	public static final String FUNCSIG_READER = "public void " + FUNCNAME_READER + "(BufferReference input)";
	public static final String FUNCSIG_WRITER = "public FileBuffer " + FUNCNAME_WRITER + "()";
	public static final String FUNCSIG_SKIP = "public static void " + FUNCNAME_SKIP + "(BufferReference input)";
	
	/*----- Instance Variables -----*/
	
	private short ver_major;
	private short ver_minor;
	
	private String blockID;
	private boolean fixed_size = true; //Flip false when encounter anything that makes it variable
	
	/*----- Init -----*/
	
	public BlockBgJavaTarget(){
		super();
	}
	
	/*----- Getters -----*/
	
	/*----- Setters -----*/
	
	public void setBlockInformation(String block_id, short vmaj, short vmin){
		if(blockID != null) return;
		blockID = block_id;
		ver_major = vmaj;
		ver_minor = vmin;
		fixed_size = false;
		super.lines_rfunc.add("short " + IVARNAME_VERMAJ + ";");
		super.lines_rfunc.add("short " + IVARNAME_VERMIN + ";");
		//TODO Add read lines for blockID, size, and version? Or maybe just override that function altogether?
	}
	
	/*----- Util -----*/
	
	protected static class Version{
		public int major = -1;
		public int minor = 0;
		public Version(String vstr){
			if(vstr == null) return;
			int dot = vstr.indexOf('.');
			if(dot >= 0){
				try{
					major = Integer.parseInt(vstr.substring(0, dot));
					minor = Integer.parseInt(vstr.substring(dot+1));
				}
				catch(NumberFormatException ex){}
			}
			else{
				try{major = Integer.parseInt(vstr);}
				catch(NumberFormatException ex){}
			}
		}
	}
	
	private boolean versionUntilOkay(DataFieldDef def){
		String vuntil = def.getAttribute(XMLKEY_VER_END);
		if(vuntil != null){
			Version v = new Version(vuntil);
			if(v.major <= ver_major){
				if(v.minor < ver_minor){
					return false;
				}
			}
		}
		return true;
	}
	
	private void addRFuncIfLessThanVer(Version ver, String[] inner_lines){
		StringBuilder sb = new StringBuilder(1024);
		sb.append("if(("); sb.append(IVARNAME_VERMAJ);
		sb.append(" <= "); sb.append(ver.major);
		sb.append(") && ("); sb.append(IVARNAME_VERMIN);
		sb.append(" < "); sb.append(ver.minor);
		sb.append(")){");
		lines_rfunc.add(sb.toString());
		for(String line : inner_lines){
			lines_rfunc.add("\t" + line);
		}
		lines_rfunc.add("}");
	}
	
	private void addRFuncIfGreaterThanVer(Version ver, String[] inner_lines){
		StringBuilder sb = new StringBuilder(1024);
		sb.append("if(("); sb.append(IVARNAME_VERMAJ);
		sb.append(" >= "); sb.append(ver.major);
		sb.append(") && ("); sb.append(IVARNAME_VERMIN);
		sb.append(" >= "); sb.append(ver.minor);
		sb.append(")){");
		lines_rfunc.add(sb.toString());
		for(String line : inner_lines){
			lines_rfunc.add("\t" + line);
		}
		lines_rfunc.add("}");
	}
	
	/*----- Superclass Callbacks -----*/
	
	private void writeVSinceReadline(DataFieldDef def, String[] base_lines){
		String vsince = def.getAttribute(XMLKEY_VER_START);
		if(vsince != null){
			Version v = new Version(vsince);
			addRFuncIfGreaterThanVer(v, base_lines);
		}
		else{
			for(String line : base_lines) lines_rfunc.add(line);
		}
	}
	
	protected void writePrimitiveReadline(BasicDataFieldDef data_field, String base_line){
		//Insert an if statement if there is a version since in the data field
		String[] read_lines = {base_line};
		writeVSinceReadline(data_field, read_lines);
	}
	
	protected void writePrimitiveArrayReadlines(ArrayFieldDef data_field, String[] base_lines){
		writeVSinceReadline(data_field, base_lines);
	}
	
	protected void writeBitfieldReadline(BitFieldDef def, String base_line){
		String[] read_lines = {base_line};
		writeVSinceReadline(def, read_lines);
	}
	
	protected void writeEnumReadline(BasicDataFieldDef data_field, String base_line){
		String[] read_lines = {base_line};
		writeVSinceReadline(data_field, read_lines);
	}
	
	protected void writeListReadlines(ListDef def, String[] base_lines){
		writeVSinceReadline(def, base_lines);
	}
	
	protected void writeAnonStructReadlines(AnonStructFieldDef struct_info, String[] base_lines){
		writeVSinceReadline(struct_info, base_lines);
	}
	
	protected void writeStructReadlines(DataFieldDef def, String[] base_lines){
		writeVSinceReadline(def, base_lines);
	}
	
	protected void writeStructArrayReadlines(ArrayFieldDef data_field, String[] base_lines){
		writeVSinceReadline(data_field, base_lines);
	}
	
	protected boolean acceptPrimitive(BasicDataFieldDef data_field){
		//Look for a "version until" field
		if(!versionUntilOkay(data_field)){
			//Deprecated. Add reader line to skip it.
			PrimType primtype = super.parsePrimitiveType(data_field.getTypeName());
			String vuntil = data_field.getAttribute(XMLKEY_VER_END);
			Version v = new Version(vuntil);
			String szstr = Integer.toString(primtype.type_b >>> 3);
			String[] readlines = {VARNAME_INDATA + ".add(" + szstr + ");"};
			addRFuncIfLessThanVer(v, readlines);
			fixed_size = false;
			return false;
		}
		else return true;
	}
	
	protected boolean acceptPrimitiveArray(ArrayFieldDef data_field){
		if(!versionUntilOkay(data_field)){
			//Deprecated. Add reader line to skip it.
			PrimType primtype = super.parsePrimitiveType(data_field.getTypeName());
			String vuntil = data_field.getAttribute(XMLKEY_VER_END);
			Version v = new Version(vuntil);
			int leni = data_field.lengthAsInt();
			if(leni > 0){
				int len = (primtype.type_b >>> 3) * leni;
				String szstr = Integer.toString(len);
				String[] readlines = {VARNAME_INDATA + ".add(" + szstr + ");"};
				addRFuncIfLessThanVer(v, readlines);
			}
			else{
				int unit_size = primtype.type_b >>> 3;
				String szstr = data_field.getLengthString() + "*" + unit_size;
				String[] readlines = {VARNAME_INDATA + ".add(" + szstr + ");"};
				addRFuncIfLessThanVer(v, readlines);
			}
			fixed_size = false;
			return false;
		}
		else {
			int leni = data_field.lengthAsInt();
			if(leni <= 0) fixed_size = false;
			return true;
		}
	}
	
	protected boolean acceptBitfield(BitFieldDef def){
		//Right now, doesn't handle versioned subfields. Only the whole thing.
		if(!versionUntilOkay(def)){
			String vuntil = def.getAttribute(XMLKEY_VER_END);
			Version v = new Version(vuntil);
			String szstr = Integer.toString(def.getMaxWidth() >>> 3);
			String[] readlines = {VARNAME_INDATA + ".add(" + szstr + ");"};
			addRFuncIfLessThanVer(v, readlines);
			fixed_size = false;
			return false;
		}
		else return true;
	}
	
	protected boolean acceptEnum(BasicDataFieldDef data_field){
		if(!versionUntilOkay(data_field)){
			String vuntil = data_field.getAttribute(XMLKEY_VER_END);
			Version v = new Version(vuntil);
			String eclassname = BingenJavaTarget.resolveEnumTypeClassName(data_field.getTypeName());
			String szstr = eclassname + "." + CONSTNAME_ENUMSIZE;
			String[] readlines = {VARNAME_INDATA + ".add(" + szstr + ");"};
			addRFuncIfLessThanVer(v, readlines);
			fixed_size = false;
			return false;
		}
		else return true;
	}
	
	protected boolean acceptTable(ListDef def, String entry_type_name){
		if(!versionUntilOkay(def)){
			//Deprecated. Add reader line to skip it.
			String vuntil = def.getAttribute(XMLKEY_VER_END);
			Version v = new Version(vuntil);
			String[] readlines = new String[3];
			readlines[0] = "for (int i = 0; i < " + def.getLengthString() + "; i++){";
			readlines[1] = "\t" + entry_type_name + "." + FUNCNAME_SKIP + "(" + VARNAME_INDATA + ");";
			readlines[2] = "}";
			addRFuncIfLessThanVer(v, readlines);
			fixed_size = false;
			return false;
		}
		else {
			int leni = def.lengthAsInt();
			if(leni <= 0) fixed_size = false;
			return true;
		}
	}
	
	protected boolean acceptAnonStruct(AnonStructFieldDef struct_info, String entry_type_name){
		if(!versionUntilOkay(struct_info)){
			//Deprecated. Add reader line to skip it.
			String vuntil = struct_info.getAttribute(XMLKEY_VER_END);
			Version v = new Version(vuntil);
			String[] readlines = new String[1];
			readlines[0] = entry_type_name + "." + FUNCNAME_SKIP + "(" + VARNAME_INDATA + ");";
			addRFuncIfLessThanVer(v, readlines);
			fixed_size = false;
			return false;
		}
		else return true;
	}
	
	protected boolean acceptStruct(DataFieldDef data_field, String typename, boolean asPointer){
		//What about variants in versions of the structs to skip... ahhhhhh
		//Maybe have a skip read function?
		if(!versionUntilOkay(data_field)){
			//Deprecated. Add reader line to skip it.
			String vuntil = data_field.getAttribute(XMLKEY_VER_END);
			Version v = new Version(vuntil);
			String[] readlines = {typename + "." + FUNCNAME_SKIP + "(" + VARNAME_INDATA + ");"};
			addRFuncIfLessThanVer(v, readlines);
			return false;
		}
		else return true;
	}
	
	protected boolean acceptStructArray(ArrayFieldDef data_field, String typename, boolean asPointer){
		if(!versionUntilOkay(data_field)){
			//Deprecated. Add reader line to skip it.
			String vuntil = data_field.getAttribute(XMLKEY_VER_END);
			Version v = new Version(vuntil);
			String[] readlines = new String[3];
			readlines[0] = "for (int i = 0; i < " + data_field.getLengthString() + "; i++){";
			readlines[1] = "\t" + typename + "." + FUNCNAME_SKIP + "(" + VARNAME_INDATA + ");";
			readlines[2] = "}";
			addRFuncIfLessThanVer(v, readlines);
			fixed_size = false;
			return false;
		}
		else {
			int leni = data_field.lengthAsInt();
			if(leni <= 0) fixed_size = false;
			return true;
		}
	}
	
	protected void addToConstantsSectionCallback(BufferedWriter writer, int indent) throws IOException{
		if(blockID == null) return;
		
		//Version
		newline(writer);
		outputLine("public static final short " + KEY_VERMAJ + " = " + ver_major + ";", writer, indent);
		outputLine("public static final short " + KEY_VERMIN + " = " + ver_minor + ";", writer, indent);
		outputLine("public static final String " + KEY_BLOCKID + " = " + blockID + ";", writer, indent);
		newline(writer);
	}
	
	/*----- Internal -----*/
	
	protected void writeReaderFunction(BufferedWriter bw, int extra_indent) throws IOException{
		//TODO
		//Also adds the skip function here.
	}
	
	protected void writeBlockWriterFunction(BufferedWriter bw, int extra_indent) throws IOException{
		
	}
	
	protected void writeWriterFunctions(BufferedWriter bw, String wfunc_name, int extra_indent) throws IOException{
		//Calls super, but also adds writeBlockWriterFunction
		//Ignores wfunc_name given to it and subsitutes its own
	}
	
	
	/*----- Interface -----*/
	
}
