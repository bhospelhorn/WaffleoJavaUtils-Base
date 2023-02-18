package waffleoRai_AutoCode.java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import waffleoRai_AutoCode.BingennerPackage;
import waffleoRai_AutoCode.BingennerTarget;
import waffleoRai_AutoCode.typedefs.AnonStructFieldDef;
import waffleoRai_AutoCode.typedefs.ArrayFieldDef;
import waffleoRai_AutoCode.typedefs.BasicDataFieldDef;
import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.BitFieldDef;
import waffleoRai_AutoCode.typedefs.DataFieldDef;
import waffleoRai_AutoCode.typedefs.EnumDef;
import waffleoRai_AutoCode.typedefs.ListDef;
import waffleoRai_AutoCode.typedefs.BitFieldDef.BitField;
import waffleoRai_Utils.StringUtils;

public class BingenJavaTarget implements BingennerTarget{
	
	private static final boolean DEBUG_MODE = true;
	
	/*----- Constants -----*/
	
	public static final String IMPORT_FILEBUFFER = "waffleoRai_Utils.FileBuffer";
	public static final String IMPORT_BUFFERREF = "waffleoRai_Utils.BufferReference";
	public static final String IMPORT_MULTIBUFF = "waffleoRai_Utils.MultiFileBuffer";
	
	public static final String WRITER_BUFFERNAME = "output";
	public static final String VARNAME_INDATA = "input";
	public static final String VARNAME_SIZECALC = "size";
	public static final String VARNAME_READER_COUNT = "bytesRead";
	public static final String VARNAME_WRITER_COUNT = "wsize";
	
	public static final String FUNCNAME_READER = "readDataIn";
	public static final String FUNCNAME_WRITER = "writeDataOut";
	public static final String FUNCNAME_SIZECALC = "estAllocSize";
	public static final String FUNCSIG_READER = "public long " + FUNCNAME_READER + "(BufferReference input)";
	public static final String FUNCSIG_WRITER_A = "public FileBuffer " + FUNCNAME_WRITER + "()";
	public static final String FUNCSIG_WRITER_B = "public long " + FUNCNAME_WRITER + "(FileBuffer output)";
	public static final String FUNCSIG_SIZECALC = "public int " + FUNCNAME_SIZECALC + "()";
	
	/*----- Instance Variables -----*/
	
	private boolean output_byteorder = false; //Defaults to LE
	protected List<BlockInfo> ser_blocks;
	protected BlockInfo current_block; //Added as soon as it is created.
	
	private BingenJavaTarget parent = null; //outer class, if inner class
	private List<BingenJavaTarget> children; //inner classes
	
	//Open doc
	private String out_dir;
	private String current_pkg_string;
	private String typename;
	private String desc;
	private List<String> imports;
	
	private Set<String> unk_types; //Strings of names of non-primitives to remember to import
	
	protected List<String> lines_constr;
	protected List<String> lines_instvars;
	protected List<String> lines_getters;
	protected List<String> lines_setters;
	protected List<String> lines_rfunc;
	
	/*----- Inner Structs -----*/
	
	protected static class PrimType{
		public char type_a;
		public int type_b;
	}
	
	protected static class VarName{
		public String name_lowerstart;
		public String name_upperstart;
	}
	
	protected static class BlockInfo{
		public int base_alloc = 0;
		public List<String> calc_func_lines;
		public List<String> writr_func_lines; //For just this block, not including sub-buffer alloc
		public boolean incl_structs = false;
		public boolean dirty = false;
		public String single_struct_iname = null;
		
		private Map<String, Integer> enum_fields;//Counts occ of enums
		
		public BlockInfo(){
			calc_func_lines = new LinkedList<String>();
			writr_func_lines = new LinkedList<String>();
		}
		
		public void addEnumOcc(String enum_type){
			if(enum_fields == null) enum_fields = new HashMap<String, Integer>();
			Integer ct = enum_fields.get(enum_type);
			if(ct != null) enum_fields.put(enum_type, ct+1);
			else enum_fields.put(enum_type, 0);
		}
		
		public int getEnumOccCount(String enum_type){
			if(enum_fields == null) return 0;
			Integer ct = enum_fields.get(enum_type);
			if(ct != null) return ct;
			return 0;
		}
	}
	
	/*----- Init -----*/
	
	public BingenJavaTarget(){
		imports = new LinkedList<String>();
		unk_types = new HashSet<String>();
		lines_instvars = new LinkedList<String>();
		lines_getters = new LinkedList<String>();
		lines_setters = new LinkedList<String>();
		lines_rfunc = new LinkedList<String>();
		lines_constr = new LinkedList<String>();
		children = new LinkedList<BingenJavaTarget>();
		ser_blocks = new LinkedList<BlockInfo>();
	}
	
	/*----- Getters -----*/
	
	/*----- Setters -----*/
	
	public void setOutputBigEndian(boolean value){output_byteorder = value;}
	
	/*----- For Subclasses -----*/
	
	protected void addToConstantsSectionCallback(BufferedWriter writer, int indent) throws IOException{}
	protected void resolveImportCallback(BingennerTypedef tdef){}
	protected void resolveEnumImportCallback(EnumDef def){}
	
	protected boolean acceptPrimitive(BasicDataFieldDef data_field){return true;}
	protected boolean acceptPrimitiveArray(ArrayFieldDef data_field){return true;}
	protected boolean acceptBitfield(BitFieldDef def){return true;}
	protected boolean acceptEnum(BasicDataFieldDef data_field){return true;}
	protected boolean acceptTable(ListDef def, String entry_type_name){return true;}
	protected boolean acceptAnonStruct(AnonStructFieldDef struct_info, String entry_type_name){return true;}
	protected boolean acceptStruct(DataFieldDef data_field, String typename, boolean asPointer){return true;}
	protected boolean acceptStructArray(ArrayFieldDef data_field, String typename, boolean asPointer){return true;}
	
	protected void writePrimitiveReadline(BasicDataFieldDef data_field, String base_line){
		lines_rfunc.add(base_line);
	}
	
	protected void writePrimitiveArrayReadlines(ArrayFieldDef data_field, String[] base_lines){
		for(String line : base_lines) lines_rfunc.add(line);
	}
	
	protected void writeBitfieldReadline(BitFieldDef def, String base_line){
		lines_rfunc.add(base_line);
	}
	
	protected void writeEnumReadline(BasicDataFieldDef data_field, String base_line){
		lines_rfunc.add(base_line);
	}
	
	protected void writeListReadlines(ListDef def, String[] base_lines){
		for(String line : base_lines) lines_rfunc.add(line);
	}
	
	protected void writeAnonStructReadlines(AnonStructFieldDef struct_info, String[] base_lines){
		for(String line : base_lines) lines_rfunc.add(line);
	}
	
	protected void writeStructReadlines(DataFieldDef def, String[] base_lines){
		for(String line : base_lines) lines_rfunc.add(line);
	}
	
	protected void writeStructArrayReadlines(ArrayFieldDef data_field, String[] base_lines){
		for(String line : base_lines) lines_rfunc.add(line);
	}
	
	/*----- Internal -----*/
	
	private void newBlock(){
		if(current_block != null && !current_block.dirty) return;
		current_block = new BlockInfo();
		ser_blocks.add(current_block);
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.newBlock || New block!");
		}
	}
	
	protected PrimType parsePrimitiveType(String typestr){
		if(typestr == null) return null;
		PrimType prim = new PrimType();
		prim.type_a = typestr.charAt(0);
		prim.type_b = Integer.parseInt(typestr.substring(1));
		return prim;
	}
	
	protected VarName processVariableName(String varname){
		VarName v = new VarName();
		String varname_body = varname.substring(1);
		String var0u = varname.substring(0, 1).toUpperCase();
		String var0l = varname.substring(0, 1).toLowerCase();
		v.name_upperstart = var0u + varname_body;
		v.name_lowerstart = var0l + varname_body;
		return v;
	}
	
	protected void newline(BufferedWriter bw) throws IOException{bw.write('\n');}
	
	protected void outputLine(String line, BufferedWriter bw, int indent)throws IOException{
		for(int i = 0; i < indent; i++) bw.write('\t');
		bw.write(line);
		bw.write('\n');
	}
	
	protected void writeToDoc(BufferedWriter bw) throws IOException{
		//Class description
		int extra_indent = (parent != null)?1:0;
		if(desc != null && !desc.isEmpty()){
			outputLine("/*", bw, extra_indent);
			outputLine("* " + desc, bw, extra_indent);
			outputLine("*/", bw, extra_indent);
		}
		
		//Declaration
		if(parent != null){
			outputLine("public static class " + typename + "{", bw, 1);
			newline(bw);
		}
		else{
			outputLine("public class " + typename + "{", bw, 0);
			newline(bw);
		}
		
		//Constants
		outputLine("/*----- Constants -----*/", bw, 1 + extra_indent);
		newline(bw);
		int i = 0;
		for(BlockInfo block : ser_blocks){
			if(block.base_alloc > 0){
				outputLine("protected static final int BASE_SIZE_BLOCK_" + 
						String.format("%03d = %d;", i, block.base_alloc), bw, 1 + extra_indent);
			}
			i++;
		}
		addToConstantsSectionCallback(bw, 1 + extra_indent);
		newline(bw);
		
		//Inner Classes
		if(parent == null && !children.isEmpty()){
			outputLine("/*----- Inner Structs -----*/", bw, 1);
			newline(bw);
			
			for(BingenJavaTarget child : children){
				child.writeToDoc(bw);
			}
		}
		
		//Instance Variables
		outputLine("/*----- Instance Variables -----*/", bw, 1 + extra_indent);
		newline(bw);
		for(String line : lines_instvars){
			outputLine(line, bw, 1 + extra_indent);
		}
		newline(bw);
		
		//Constructor
		outputLine("/*----- Init -----*/", bw, 1 + extra_indent);
		newline(bw);
		outputLine("public " + typename + "(){", bw, 1 + extra_indent);
		for(String line : lines_constr){
			outputLine(line, bw, 2 + extra_indent);
		}
		outputLine("}", bw, 1 + extra_indent);
		newline(bw);
		
		//Readers
		outputLine("/*----- Readers -----*/", bw, 1 + extra_indent);
		newline(bw);
		writeReaderFunction(bw, extra_indent);
		
		//Writers
		outputLine("/*----- Writers -----*/", bw, 1 + extra_indent);
		newline(bw);
		writeSizeCalcFuncs(bw, extra_indent);
		writeWriterFunctions(bw, FUNCNAME_WRITER, extra_indent);
		
		//Getters
		outputLine("/*----- Getters -----*/", bw, 1 + extra_indent);
		newline(bw);
		for(String line : lines_getters){
			outputLine(line, bw, 1 + extra_indent);
		}
		newline(bw);
		
		//Setters
		outputLine("/*----- Setters -----*/", bw, 1 + extra_indent);
		newline(bw);
		for(String line : lines_setters){
			outputLine(line, bw, 1 + extra_indent);
		}
		newline(bw);
		
		//Class close bracket
		outputLine("}", bw, extra_indent);
		newline(bw);
	}
	
	protected void writeSizeCalcFuncs(BufferedWriter bw, int extra_indent) throws IOException{
		int block_idx = 0;
		for(BlockInfo block : ser_blocks){
			String calc_func_name = String.format(FUNCNAME_SIZECALC + "_block_%03d", block_idx);
			if(!block.calc_func_lines.isEmpty()){
				outputLine("private int " + calc_func_name + "(){", bw, 1 + extra_indent);
				
				//Determine whether it already has a return statement...
				boolean has_ret = false;
				for(String line : block.calc_func_lines){
					if(line.startsWith("return")){
						has_ret = true;
						break;
					}
				}
				
				if(!has_ret) outputLine("int " + VARNAME_SIZECALC + " = " + block.base_alloc + ";", bw, 2 + extra_indent);
				for(String line : block.calc_func_lines){
					outputLine(line, bw, 2 + extra_indent);
					if(line.startsWith("return")) break;
				}
				if(!has_ret) outputLine("return " + VARNAME_SIZECALC + ";", bw, 2 + extra_indent);
				outputLine("}", bw, 1 + extra_indent);
				newline(bw);
			}
			block_idx++;
		}
	}
	
	protected void writeReaderFunction(BufferedWriter bw, int extra_indent) throws IOException{
		outputLine(FUNCSIG_READER + "{", bw, 1 + extra_indent);
		outputLine("long stpos = " + VARNAME_INDATA + ".getBufferPosition();", bw, 2 + extra_indent);
		outputLine(VARNAME_INDATA + ".setByteOrder(" + output_byteorder + ");", bw, 2 + extra_indent);
		for(String line : lines_rfunc){
			outputLine(line, bw, 2 + extra_indent);
		}
		//return
		outputLine("return (" + VARNAME_INDATA + ".getBufferPosition() - stpos);", bw, 2 + extra_indent);
		outputLine("}", bw, 1 + extra_indent);
		newline(bw);
	}
	
	protected void writeWriterFunctions(BufferedWriter bw, String wfunc_inner_name, int extra_indent) throws IOException{
		//The main writer function writes to whatever FileBuffer it is given
		//	That one is called when a structure is interior to another.
		//The outer writer function allocates the FileBuffer
		final String VARNAME_SPLIT_BOOL = "buffer_by_block";
		final String VARNAME_MAIN_BUFFER = "mainbuff";
		final String VARNAME_NOWSIZE = "wpos";
		
		//Main writer that writes to an existing FileBuffer
		outputLine(FUNCSIG_WRITER_B + "{", bw, 1 + extra_indent);
		outputLine("long " + VARNAME_WRITER_COUNT + " = 0;", bw, 2 + extra_indent);
		outputLine("boolean " + VARNAME_SPLIT_BOOL + " = (" + WRITER_BUFFERNAME + " instanceof MultiFileBuffer);", bw, 2 + extra_indent);
		outputLine("FileBuffer " + VARNAME_MAIN_BUFFER + " = null;", bw, 2 + extra_indent);
		outputLine("if (" + VARNAME_SPLIT_BOOL + ") " + VARNAME_MAIN_BUFFER + " = " + WRITER_BUFFERNAME + ";", bw, 2 + extra_indent);
		newline(bw);
		
		int block_idx = 0;
		boolean decl_pos_var = false;
		for(BlockInfo block : ser_blocks){
			String alloc_const_name = String.format("BASE_SIZE_BLOCK_%03d", block_idx);
			if(block.incl_structs){
				if(block.single_struct_iname != null){
					outputLine("if (" + VARNAME_SPLIT_BOOL + "){", bw, 2 + extra_indent);
					outputLine(WRITER_BUFFERNAME + " = " + block.single_struct_iname + "." + wfunc_inner_name + "();", bw, 3 + extra_indent);
					outputLine(VARNAME_WRITER_COUNT + " += " + WRITER_BUFFERNAME + ".getFileSize();", bw, 3 + extra_indent);
					outputLine(VARNAME_MAIN_BUFFER + ".addToFile(" + WRITER_BUFFERNAME + ");", bw, 3 + extra_indent);
					outputLine("}", bw, 2 + extra_indent);
					outputLine("else {", bw, 2 + extra_indent);
					outputLine(VARNAME_WRITER_COUNT + " += " + block.single_struct_iname + "." + wfunc_inner_name + "(" + WRITER_BUFFERNAME + ");", bw, 3 + extra_indent);
					outputLine("}", bw, 2 + extra_indent);
					newline(bw);
				}
				else{
					String calc_func_name = String.format(FUNCNAME_SIZECALC + "_block_%03d", block_idx);
					outputLine("if (" + VARNAME_SPLIT_BOOL + ") " + WRITER_BUFFERNAME + " = new MultiFileBuffer(" + calc_func_name + "());", bw, 2 + extra_indent);
					
					for(String line : block.writr_func_lines){
						outputLine(line, bw, 2 + extra_indent);
					}
					outputLine("if (" + VARNAME_SPLIT_BOOL + ") " + VARNAME_MAIN_BUFFER + ".addToFile(" + WRITER_BUFFERNAME + ");", bw, 2 + extra_indent);	
				}
			}
			else{
				if(!block.calc_func_lines.isEmpty()){
					//Calc function
					String calc_func_name = String.format(FUNCNAME_SIZECALC + "_block_%03d", block_idx);
					outputLine("if (" + VARNAME_SPLIT_BOOL + ") " + WRITER_BUFFERNAME + " = new FileBuffer(" + calc_func_name + "(), " + output_byteorder + ");", bw, 2 + extra_indent);
					if(!decl_pos_var){
						outputLine("long " + VARNAME_NOWSIZE + " = " + WRITER_BUFFERNAME + ".getFileSize();", bw, 2 + extra_indent);
						decl_pos_var = true;
					}
					else{
						outputLine(VARNAME_NOWSIZE + " = " + WRITER_BUFFERNAME + ".getFileSize();", bw, 2 + extra_indent);
					}
					for(String line : block.writr_func_lines){
						outputLine(line, bw, 2 + extra_indent);
					}
					outputLine(VARNAME_WRITER_COUNT + " += " + WRITER_BUFFERNAME + ".getFileSize() - " + VARNAME_NOWSIZE + ";", bw, 2 + extra_indent);
					outputLine("if (" + VARNAME_SPLIT_BOOL + ") " + VARNAME_MAIN_BUFFER + ".addToFile(" + WRITER_BUFFERNAME + ");", bw, 2 + extra_indent);
				}
				else{
					//Fixed size
					outputLine("if (" + VARNAME_SPLIT_BOOL + ") " + WRITER_BUFFERNAME + " = new FileBuffer(" + alloc_const_name + ", " + output_byteorder + ");", bw, 2 + extra_indent);
					for(String line : block.writr_func_lines){
						outputLine(line, bw, 2 + extra_indent);
					}
					outputLine(VARNAME_WRITER_COUNT + " += " + alloc_const_name + ";", bw, 2 + extra_indent);
					outputLine("if (" + VARNAME_SPLIT_BOOL + ") " + VARNAME_MAIN_BUFFER + ".addToFile(" + WRITER_BUFFERNAME + ");", bw, 2 + extra_indent);
				}
			}
			newline(bw);
			block_idx++;
		}
		
		outputLine("return wsize;", bw, 2 + extra_indent);
		outputLine("}", bw, 1 + extra_indent);
		newline(bw);

		//Writer that allocs a FileBuffer to write to
		outputLine(FUNCSIG_WRITER_A + "{", bw, 1 + extra_indent);
		int bcount = ser_blocks.size();
		if(bcount > 1){
			outputLine("FileBuffer out = new MultiFileBuffer(" + bcount + ");", bw, 2 + extra_indent);
		}
		else{
			if(current_block.incl_structs){
				outputLine("FileBuffer out = new MultiFileBuffer(1);", bw, 2 + extra_indent);
			}
			else{
				if(!current_block.calc_func_lines.isEmpty()){
					String calc_func_name = FUNCNAME_SIZECALC + "_block_000";
					outputLine("FileBuffer out = new FileBuffer(" + calc_func_name + "(), " + output_byteorder + ");", bw, 2 + extra_indent);
				}
				else{
					outputLine("FileBuffer out = new FileBuffer(BASE_SIZE_BLOCK_000, " + output_byteorder + ");", bw, 2 + extra_indent);
				}
			}
		}
		outputLine(FUNCNAME_WRITER + "(out);", bw, 2 + extra_indent);
		outputLine("return out;", bw, 2 + extra_indent);
		outputLine("}", bw, 1 + extra_indent);
		newline(bw);
	}
	
	private void addPrimLines(BasicDataFieldDef data_field, VarName varname, String type, String infunc, String float_read_func, String float_write_func){
		String instline = "private " + type + " " + varname.name_lowerstart;
		String defo_val = data_field.getDefaultValue();
		String desc = data_field.getDescription();
		if(defo_val != null) instline += " = " + defo_val;
		instline += ";";
		
		if(desc != null) instline += " //" + desc;
		lines_instvars.add(instline);
		
		lines_getters.add("public " + type + " get" + varname.name_upperstart + "(){return " + varname.name_lowerstart + ";}");
		lines_setters.add("public void set" + varname.name_upperstart + "(" + type + " value){" + varname.name_lowerstart + " = value;}");
		
		if(float_read_func != null && !float_read_func.isEmpty()){
			writePrimitiveReadline(data_field, varname.name_lowerstart + " = "+ float_read_func + "(input." + infunc + "());");
		}
		else{
			writePrimitiveReadline(data_field, varname.name_lowerstart + " = input." + infunc + "();");
		}
		
		if(float_write_func != null && !float_write_func.isEmpty()){
			current_block.writr_func_lines.add(WRITER_BUFFERNAME + ".addToFile(" + float_write_func + "(" + varname.name_lowerstart + "));");
		}
		else{
			current_block.writr_func_lines.add(WRITER_BUFFERNAME + ".addToFile(" + varname.name_lowerstart + ");");
		}
		current_block.dirty = true;
	}
	
	private void addPrimArrayLines_FixedSize(ArrayFieldDef data_field, VarName varname, String type, String infunc, String float_read_func, String float_write_func){
		int len = data_field.lengthAsInt();
		String desc = data_field.getDescription();
		lines_constr.add(varname.name_lowerstart + " = new " + type + "[" + len + "];");
		
		
		if(desc != null){
			lines_instvars.add("private " + type + "[] " + varname.name_lowerstart + "; //" + desc);
		}
		else{
			lines_instvars.add("private " + type + "[] " + varname.name_lowerstart + ";");
		}
		
		lines_getters.add("public " + type + "[] get" + varname.name_upperstart + "(){return " + varname.name_lowerstart + ";}");
		lines_setters.add("public void set" + varname.name_upperstart + "(int index, " + type + " value){" + varname.name_lowerstart + "[index] = value;}");
		
		String[] rfunc_lines = new String[1];
		if(float_read_func != null && !float_read_func.isEmpty()){
			rfunc_lines[0] = "for(int i = 0; i < " + len + "; i++){" + varname.name_lowerstart + "[i] = "+ float_read_func + "(input." + infunc + "());}";
		}
		else{
			rfunc_lines[0] = "for(int i = 0; i < " + len + "; i++){" + varname.name_lowerstart + "[i] = input." + infunc + "();}";
		}
		writePrimitiveArrayReadlines(data_field, rfunc_lines);
		
		if(float_write_func != null && !float_write_func.isEmpty()){
			current_block.writr_func_lines.add("for(int i = 0; i < " + len + "; i++){" + WRITER_BUFFERNAME + ".addToFile(" + float_write_func + "(" + varname.name_lowerstart + "[i]));}");
		}
		else{
			current_block.writr_func_lines.add("for(int i = 0; i < " + len + "; i++){" + WRITER_BUFFERNAME + ".addToFile(" + varname.name_lowerstart + "[i]);}");
		}
		current_block.dirty = true;
	}
	
	private void addPrimArrayLines_VarSize(ArrayFieldDef data_field, VarName varname, String len, String type, String infunc, String float_read_func, String float_write_func){
		String desc = data_field.getDescription();
		
		if(desc != null){
			lines_instvars.add("private " + type + "[] " + varname.name_lowerstart + "; //" + desc);
		}
		else{
			lines_instvars.add("private " + type + "[] " + varname.name_lowerstart + ";");
		}
		
		lines_getters.add("public " + type + "[] get" + varname.name_upperstart + "(){return " + varname.name_lowerstart + ";}");
		
		lines_setters.add("public void alloc" + varname.name_upperstart + "(int size){" + varname.name_lowerstart + " = new " + type + "[size]; " + len + " = size;}");
		lines_setters.add("public void set" + varname.name_upperstart + "(int index, " + type + " value){" + varname.name_lowerstart + "[index] = value;}");
		
		String[] rfunc_lines = new String[2];
		rfunc_lines[0] = varname.name_lowerstart + " = new " + type + "[" + len + "];";
		if(float_read_func != null && !float_read_func.isEmpty()){
			rfunc_lines[1] = "for(int i = 0; i < " + len + "; i++){" + varname.name_lowerstart + "[i] = "+ float_read_func + "(input." + infunc + "());}";
		}
		else{
			rfunc_lines[1] = "for(int i = 0; i < " + len + "; i++){" + varname.name_lowerstart + "[i] = input." + infunc + "();}";
		}
		writePrimitiveArrayReadlines(data_field, rfunc_lines);
		
		if(float_write_func != null && !float_write_func.isEmpty()){
			current_block.writr_func_lines.add("for(int i = 0; i < " + len + "; i++){" + WRITER_BUFFERNAME + ".addToFile(" + float_write_func + "(" + varname.name_lowerstart + "[i]));}");
		}
		else{
			current_block.writr_func_lines.add("for(int i = 0; i < " + len + "; i++){" + WRITER_BUFFERNAME + ".addToFile(" + varname.name_lowerstart + "[i]);}");
		}
		current_block.dirty = true;
	}
	
	private void addPrimArray_FixedSize(ArrayFieldDef data_field, int len){
		PrimType primtype = parsePrimitiveType(data_field.getTypeName());
		VarName vn = processVariableName(data_field.getName());
		
		switch(primtype.type_b){
		case 8:
			addPrimArrayLines_FixedSize(data_field, vn, "byte", "nextByte", null, null);
			current_block.base_alloc+=len;
			break;
		case 16:
			addPrimArrayLines_FixedSize(data_field, vn, "short", "nextShort", null, null);
			current_block.base_alloc+=(len<<1);
			break;
		case 32:
			if(primtype.type_a == 'f'){
				addPrimArrayLines_FixedSize(data_field, vn, "float", "nextInt", "Float.intBitsToFloat", "Float.floatToRawIntBits");
			}
			else{
				addPrimArrayLines_FixedSize(data_field, vn, "int", "nextInt", null, null);
			}
			current_block.base_alloc+=(len<<2);
			break;
		case 64:
			if(primtype.type_a == 'f'){
				addPrimArrayLines_FixedSize(data_field, vn, "double", "nextLong", "Double.longBitsToDouble", "Double.doubleToRawLongBits");
			}
			else{
				addPrimArrayLines_FixedSize(data_field, vn, "long", "nextLong", null, null);
			}
			current_block.base_alloc+=(len<<3);
			break;
		}
		
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.addPrimArray_FixedSize || Primitive array (fixed size) added: " + data_field.getName());
		}
	}
	
	private void addPrimArray_VarSize(ArrayFieldDef data_field, VarName lenVar){
		PrimType primtype = parsePrimitiveType(data_field.getTypeName());
		VarName vn = processVariableName(data_field.getName());
		String len = lenVar.name_lowerstart;
		
		switch(primtype.type_b){
		case 8:
			addPrimArrayLines_VarSize(data_field, vn, lenVar.name_lowerstart, "byte", "nextByte", null, null);
			current_block.calc_func_lines.add(VARNAME_SIZECALC + " += " + len + ";");
			break;
		case 16:
			addPrimArrayLines_VarSize(data_field, vn, lenVar.name_lowerstart, "short", "nextShort", null, null);
			current_block.calc_func_lines.add(VARNAME_SIZECALC + " += (" + len + " << 1);");
			break;
		case 32:
			if(primtype.type_a == 'f'){
				addPrimArrayLines_VarSize(data_field, vn, lenVar.name_lowerstart, "float", "nextInt", "Float.intBitsToFloat", "Float.floatToRawIntBits");
			}
			else{
				addPrimArrayLines_VarSize(data_field, vn, lenVar.name_lowerstart, "int", "nextInt", null, null);
			}
			current_block.calc_func_lines.add(VARNAME_SIZECALC + " += (" + lenVar.name_lowerstart + " << 2);");
			break;
		case 64:
			if(primtype.type_a == 'f'){
				addPrimArrayLines_VarSize(data_field, vn, lenVar.name_lowerstart, "double", "nextLong", "Double.longBitsToDouble", "Double.doubleToRawLongBits");
			}
			else{
				addPrimArrayLines_VarSize(data_field, vn, lenVar.name_lowerstart, "long", "nextLong", null, null);
			}
			current_block.calc_func_lines.add(VARNAME_SIZECALC + " += (" + lenVar.name_lowerstart + " << 3);");
			break;
		}
		
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.addPrimArray_FixedSize || Primitive array (var size) added: " + data_field.getName());
		}
	}
	
	private void addBitGetter(BitField field, int bit_offset, BitFieldDef parent, String parent_var){
		VarName vn = processVariableName(field.getName());
		if(field.getWidth() == 1){
			long mask = 1L << bit_offset;
			if(parent.getMaxWidth() > 32){
				lines_getters.add("public boolean get" + vn.name_upperstart + "(){return (" + parent_var + " & 0x" + Long.toHexString(mask) + "L) != 0;}");
			}
			else{
				lines_getters.add("public boolean get" + vn.name_upperstart + "(){return (" + parent_var + " & 0x" + Long.toHexString(mask) + ") != 0;}");
			}
		}
		else{
			if(parent.getMaxWidth() > 32){
				long mask = ~0L;
				mask = ~(mask << field.getWidth());
				lines_getters.add("public long get" + vn.name_upperstart + "(){return (" + parent_var + " >>> " + bit_offset + ") & 0x" + Long.toHexString(mask) + "L;}");
			}
			else{
				int mask = ~0;
				mask = ~(mask << field.getWidth());
				lines_getters.add("public int get" + vn.name_upperstart + "(){return (" + parent_var + " >>> " + bit_offset + ") & 0x" + Integer.toHexString(mask) + ";}");
			}
		}
	}
	
	private void addBitSetter(BitField field, int bit_offset, BitFieldDef parent, String parent_var){
		VarName vn = processVariableName(field.getName());
		if(field.getWidth() == 1){
			long mask = 1L << bit_offset;
			if(parent.getMaxWidth() > 32){
				lines_setters.add("public void set" + vn.name_upperstart + "(){" + parent_var + " |= 0x" + Long.toHexString(mask) + "L;}");
				lines_setters.add("public void clear" + vn.name_upperstart + "(){" + parent_var + " &= ~0x" + Long.toHexString(mask) + "L;}");
			}
			else{
				lines_setters.add("public void set" + vn.name_upperstart + "(){" + parent_var + " |= 0x" + Long.toHexString(mask) + ";}");
				lines_setters.add("public void clear" + vn.name_upperstart + "(){" + parent_var + " &= ~0x" + Long.toHexString(mask) + ";}");
			}
		}
		else{
			long mask1 = ~0L;
			mask1 = ~(mask1 << field.getWidth());
			long mask2 = mask1 << bit_offset;
			if(parent.getMaxWidth() > 32){
				lines_setters.add("public void set" + vn.name_upperstart + "(long value){" + parent_var + " &= ~0x" + Long.toHexString(mask2) + "L;" 
							+ parent_var + " |= ((value & 0x" + Long.toHexString(mask1) + "L) << " + bit_offset + ");}");
			}
			else{
				lines_setters.add("public void set" + vn.name_upperstart + "(int value){" + parent_var + " &= ~0x" + Long.toHexString(mask2) + ";" 
						+ parent_var + " |= ((value & 0x" + Long.toHexString(mask1) + ") << " + bit_offset + ");}");
			}
		}
	}
	
	private void addBitField_asOne(BitFieldDef def){
		VarName vn = processVariableName(def.getName());
		int mywidth = def.getMaxWidth();
		String bftype = "int";
		if(mywidth > 32) bftype = "long";
		
		String desc = def.getDescription();
		if(desc != null){
			lines_instvars.add("private " + bftype + " " + vn.name_lowerstart + "; //" + desc);
		}
		else{
			lines_instvars.add("private " + bftype + " " + vn.name_lowerstart + ";");
		}
		
		//Getters
		lines_getters.add("public " + bftype + " get" + vn.name_upperstart + "(){return " + vn.name_lowerstart + ";}");
		BitField[] fields = def.getFieldsDirect();
		int bpos = 0;
		for(BitField bf : fields){
			addBitGetter(bf, bpos, def, vn.name_lowerstart);
			bpos += bf.getWidth();
		}
		
		//Setters
		bpos = 0;
		for(BitField bf : fields){
			addBitSetter(bf, bpos, def, vn.name_lowerstart);
			bpos += bf.getWidth();
		}
		
		//Remainder
		switch(mywidth){
		case 8:
			writeBitfieldReadline(def, vn.name_lowerstart + " = Byte.toUnsignedInt(input.nextByte());");
			current_block.writr_func_lines.add(WRITER_BUFFERNAME + ".addToFile((byte)" + vn.name_lowerstart + ");");
			current_block.base_alloc++;
			break;
		case 16:
			writeBitfieldReadline(def, vn.name_lowerstart + " = Short.toUnsignedInt(input.nextShort());");
			current_block.writr_func_lines.add(WRITER_BUFFERNAME + ".addToFile((short)" + vn.name_lowerstart + ");");
			current_block.base_alloc += 2;
			break;
		case 24:
			writeBitfieldReadline(def, vn.name_lowerstart + " = input.next24Bits();");
			current_block.writr_func_lines.add(WRITER_BUFFERNAME + ".add24ToFile(" + vn.name_lowerstart + ");");
			current_block.base_alloc += 3;
			break;
		case 32:
			writeBitfieldReadline(def, vn.name_lowerstart + " = input.nextInt();");
			current_block.writr_func_lines.add(WRITER_BUFFERNAME + ".addToFile(" + vn.name_lowerstart + ");");
			current_block.base_alloc += 4;
			break;
		case 64:
			writeBitfieldReadline(def, vn.name_lowerstart + " = input.nextLong();");
			current_block.writr_func_lines.add(WRITER_BUFFERNAME + ".addToFile(" + vn.name_lowerstart + ");");
			current_block.base_alloc += 8;
			break;
		}
		
		current_block.dirty = true;
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.addBitField_asOne || Bitfield added: " + def.getName());
		}
	}
	
	private BingenJavaTarget newSibling(){
		if(parent == null) return null;
		return parent.newInnerClass();
	}
	
	private BingenJavaTarget newInnerClass(){
		BingenJavaTarget child = new BingenJavaTarget();
		child.parent = this;
		this.children.add(child);
		child.newBlock();
		child.output_byteorder = this.output_byteorder;
		return child;
	}
	
	/*----- Interface -----*/
	
	public boolean openDoc(String dir, BingennerTypedef def){
		if(def == null) return false;
		out_dir = dir;
		typename = def.getName().substring(0,1).toUpperCase() + def.getName().substring(1);
		desc = def.getDescription();
		BingennerPackage pkg = def.getPackage();
		if(pkg != null) this.current_pkg_string = pkg.getFullPackageString();
		newBlock();
		return true;
	}
	
	public void closeCurrentDoc() throws IOException{
		//Output the java file
		String out_path = out_dir + File.separator + typename + ".java";
		BufferedWriter bw = new BufferedWriter(new FileWriter(out_path));
		
		if(current_pkg_string != null) bw.write("package " + current_pkg_string + ";\n\n");
		bw.write("import " + IMPORT_FILEBUFFER + ";\n");
		bw.write("import " + IMPORT_BUFFERREF + ";\n");
		bw.write("import " + IMPORT_MULTIBUFF + ";\n\n");
		
		if(!imports.isEmpty()){
			for(String str : imports){
				bw.write("import " + str + ";\n");
			}
			bw.write("\n");
		}
		writeToDoc(bw);
		bw.close();
		
		out_dir = null;
		current_pkg_string = null;
		typename = null;
		
		imports.clear();
		unk_types.clear();
		lines_instvars.clear();
		lines_getters.clear();
		lines_setters.clear();
		lines_rfunc.clear();
		lines_constr.clear();
		ser_blocks.clear();
		current_block = null;
	}

	public void addPrimitive(BasicDataFieldDef data_field) {
		//Figure out type first.
		if(data_field == null) return;
		if(!this.acceptPrimitive(data_field)) return;
		if(current_block.incl_structs) newBlock();
		
		PrimType primtype = parsePrimitiveType(data_field.getTypeName());
		VarName vn = processVariableName(data_field.getName());
		
		switch(primtype.type_b){
		case 8:
			addPrimLines(data_field, vn, "byte", "nextByte", null, null);
			current_block.base_alloc++;
			break;
		case 16:
			addPrimLines(data_field, vn, "short", "nextShort", null, null);
			current_block.base_alloc+=2;
			break;
		case 32:
			if(primtype.type_a == 'f'){
				addPrimLines(data_field, vn, "float", "nextInt", "Float.intBitsToFloat", "Float.floatToRawIntBits");
			}
			else{
				addPrimLines(data_field, vn, "int", "nextInt", null, null);
			}
			current_block.base_alloc+=4;
			break;
		case 64:
			if(primtype.type_a == 'f'){
				addPrimLines(data_field, vn, "double", "nextLong", "Double.longBitsToDouble", "Double.doubleToRawLongBits");
			}
			else{
				addPrimLines(data_field, vn, "long", "nextLong", null, null);
			}
			current_block.base_alloc+=8;
			break;
		}
		
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.addPrimitive || Primitive added: " + data_field.getName());
		}
	}

	public void addPrimitiveArray(ArrayFieldDef data_field) {
		if(data_field == null) return;
		if(!this.acceptPrimitiveArray(data_field)) return;
		if(current_block.incl_structs) newBlock();
		
		int len = data_field.lengthAsInt();
		if(len > 0){
			addPrimArray_FixedSize(data_field, len);
		}
		else{
			addPrimArray_VarSize(data_field, processVariableName(data_field.getLengthString()));
		}
	}

	public void addBitfield(BitFieldDef def) {
		if(!this.acceptBitfield(def)) return;
		if(current_block.incl_structs) newBlock();
		//In case I ever decide to implement field separation...
		addBitField_asOne(def);
	}

	public void addEnum(BasicDataFieldDef data_field){
		//Treated as a primitive here, but the type isn't known until import resolution
		
		if(data_field == null) return;
		if(!this.acceptEnum(data_field)) return;
		if(current_block.incl_structs) newBlock();
		
		unk_types.add(data_field.getTypeName());
		String enum_type_name = data_field.getTypeName().replace("Enum:", "");
		VarName varname = processVariableName(data_field.getName());
		String defo_val = data_field.getDefaultValue();
		String desc = data_field.getDescription();
		
		String type_name_placeholder = "${" + enum_type_name.toUpperCase() + "}";
		String instline = "private " + type_name_placeholder + " " + varname.name_lowerstart;
		if(defo_val != null) instline += " = " + defo_val;
		instline += ";";
		
		if(desc != null) instline += " //" + desc;
		lines_instvars.add(instline);
		
		lines_getters.add("public " + type_name_placeholder + " get" + varname.name_upperstart + "(){return " + varname.name_lowerstart + ";}");
		lines_setters.add("public void set" + varname.name_upperstart + "(" + type_name_placeholder + " value){" + varname.name_lowerstart + " = value;}");
		
		writeEnumReadline(data_field, varname.name_lowerstart + " = input." + type_name_placeholder + "();");
		current_block.writr_func_lines.add(WRITER_BUFFERNAME + ".addToFile(" + varname.name_lowerstart + ");");
		current_block.addEnumOcc(enum_type_name);
		
		current_block.dirty = true;
	}
	
	public void addTable(ListDef def) {
		BingenJavaTarget listclass = null;
		
		//List struct info (name, desc etc.)
		String subclass_name = StringUtils.capitalize(def.getListEntryName());
		if(subclass_name == null){
			subclass_name = StringUtils.capitalize(def.getName());
			if(subclass_name != null) subclass_name = subclass_name + "Entry";
			else{
				subclass_name = String.format("TblEntry%08x", def.hashCode());
			}
		}
		if(!this.acceptTable(def, subclass_name)) return;
		
		if(parent != null) listclass = newSibling();
		else listclass = newInnerClass();
		//listclass.desc = def.getDescription();
		
		listclass.typename = subclass_name;
		
		//Add its fields to itself
		int fcount = def.getFieldCount();
		for(int i = 0; i < fcount; i++){
			DataFieldDef cdef = def.getFieldDef(i);
			if(cdef != null){
				cdef.addTo(listclass);
			}
		}
		
		//Add list instance to this object
		this.newBlock();
		current_block.incl_structs = true;
		
		VarName vn = processVariableName(def.getName());
		String desc = def.getDescription();
		if(desc != null){
			lines_instvars.add("private " + listclass.typename + "[] " + vn.name_lowerstart + "; //" + desc);
		}
		else{
			lines_instvars.add("private " + listclass.typename + "[] " + vn.name_lowerstart + ";");
		}
		
		lines_getters.add("public " + listclass.typename + "[] get" + vn.name_upperstart + "(){return " + vn.name_lowerstart + ";}");
		
		List<String> rfunc_lines_list = new ArrayList<String>(5);
		VarName lenn = processVariableName(def.getLengthString());
		String len = lenn.name_lowerstart;
		try{
			int ilen = Integer.parseInt(len);
			lines_constr.add(vn.name_lowerstart + " = new " + listclass.typename + "[" + ilen + "];");
			lines_setters.add("public void set" + vn.name_upperstart + "(int index, " + listclass.typename + " value){" + vn.name_lowerstart + "[index] = value;}");
			rfunc_lines_list.add("for(int i = 0; i < " + ilen + "; i++){");
		}
		catch(NumberFormatException ex){
			lines_setters.add("public void alloc" + vn.name_upperstart + "(int size){" + vn.name_lowerstart + " = new " + listclass.typename + "[size]; " + len + " = size;}");
			lines_setters.add("public void set" + vn.name_upperstart + "(int index, " + listclass.typename + " value){" + vn.name_lowerstart + "[index] = value;}");
			rfunc_lines_list.add(vn.name_lowerstart + " = new " + listclass.typename + "[" + len + "];");
			rfunc_lines_list.add("for(int i = 0; i < " + len + "; i++){");
		}
		rfunc_lines_list.add("\t" + vn.name_lowerstart + "[i] = new " + listclass.typename + "();");
		rfunc_lines_list.add("\t" + vn.name_lowerstart + "[i]." + FUNCNAME_READER + "(" + VARNAME_INDATA + ");");
		rfunc_lines_list.add("}");
		int rfunc_line_count = rfunc_lines_list.size();
		String[] rfunc_lines = new String[rfunc_line_count];
		int i = 0;
		for(String line : rfunc_lines_list) rfunc_lines[i++] = line;
		writeListReadlines(def, rfunc_lines);
		
		current_block.calc_func_lines.add("return " + len + ";");
		current_block.writr_func_lines.add("for(int i = 0; i < " + len + "; i++){ " + VARNAME_WRITER_COUNT + " += " + vn.name_lowerstart + "." + FUNCNAME_WRITER + "(" + WRITER_BUFFERNAME + ");}");
		
		current_block.dirty = true;
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.addTable || Table added: " + def.getName());
		}
	}

	public void addAnonStruct(AnonStructFieldDef struct_info){
		VarName vn = this.processVariableName(struct_info.getName());
		if(!this.acceptAnonStruct(struct_info, vn.name_upperstart)) return;
		
		BingenJavaTarget subclass = null;
		if(parent != null) subclass = newSibling();
		else subclass = newInnerClass();
		subclass.typename = vn.name_upperstart;
		
		//Add its fields to itself
		int fcount = struct_info.getFieldCount();
		for(int i = 0; i < fcount; i++){
			DataFieldDef cdef = struct_info.getFieldDef(i);
			if(cdef != null){
				cdef.addTo(subclass);
			}
		}
		
		this.newBlock();
		current_block.incl_structs = true;
				
		String desc = struct_info.getDescription();
		if(desc != null){
			lines_instvars.add("private " + subclass.typename + " " + vn.name_lowerstart + "; //" + desc);
		}
		else{
			lines_instvars.add("private " + subclass.typename + " " + vn.name_lowerstart + ";");
		}
		
		lines_getters.add("public " + subclass.typename + " get" + vn.name_upperstart + "(){return " + vn.name_lowerstart + ";}");
		
		String[] rfunc_lines = new String[2];
		rfunc_lines[0] = vn.name_lowerstart + " = new " + subclass.typename + "();";
		rfunc_lines[1] = vn.name_lowerstart + "." + FUNCNAME_READER + "(" + VARNAME_INDATA + ");";
		writeAnonStructReadlines(struct_info, rfunc_lines);
		
		lines_constr.add(vn.name_lowerstart + " = new " + subclass.typename + "();");
		lines_setters.add("public void set" + vn.name_upperstart + "(" + subclass.typename + " value){" + vn.name_lowerstart + " = value;}");
	
		current_block.writr_func_lines.add(VARNAME_WRITER_COUNT + " += " + vn.name_lowerstart + "." + FUNCNAME_WRITER + "(" + WRITER_BUFFERNAME + ");");
		
		current_block.dirty = true;
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.addTable || Table added: " + struct_info.getName());
		}
	}
	
	public void addStruct(DataFieldDef data_field, String typename, boolean asPointer) {
		typename = StringUtils.capitalize(typename);
		if(!this.acceptStruct(data_field, typename, asPointer)) return;
		
		unk_types.add(typename);
		this.newBlock();
		current_block.incl_structs = true;
		
		VarName vn = processVariableName(data_field.getName());
		String desc = data_field.getDescription();
		if(desc != null){
			lines_instvars.add("private " + typename + " " + vn.name_lowerstart + "; //" + desc);
		}
		else{
			lines_instvars.add("private " + typename + " " + vn.name_lowerstart + ";");
		}
		current_block.single_struct_iname = vn.name_lowerstart;
		
		lines_getters.add("public " + typename + " get" + vn.name_upperstart + "(){return " + vn.name_lowerstart + ";}");
		lines_setters.add("public void set" + vn.name_upperstart + "(" + typename + " value){" + vn.name_lowerstart + " = value;}");
		
		String[] rfunc_lines = new String[2];
		rfunc_lines[0] = vn.name_lowerstart + " = new " + typename + "();";
		rfunc_lines[1] = vn.name_lowerstart + "." + FUNCNAME_READER + "(" + VARNAME_INDATA + ");";
		writeStructReadlines(data_field, rfunc_lines);
		
		//current_block.calc_func_lines.add("return 1;");
		current_block.writr_func_lines.add(VARNAME_WRITER_COUNT + " += " + vn.name_lowerstart + "." + FUNCNAME_WRITER + "(" + WRITER_BUFFERNAME + ");");
		
		current_block.dirty = true;
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.addStruct || Struct added: " + data_field.getName());
		}
	}

	public void addStructArray(ArrayFieldDef data_field, String typename, boolean asPointer) {
		typename = StringUtils.capitalize(typename);
		if(!this.acceptStructArray(data_field, typename, asPointer)) return;
		
		this.newBlock();
		current_block.incl_structs = true;
		
		VarName vn = processVariableName(data_field.getName());
		String desc = data_field.getDescription();
		if(desc != null){
			lines_instvars.add("private " + typename + "[] " + vn.name_lowerstart + "; //" + desc);
		}
		else{
			lines_instvars.add("private " + typename + "[] " + vn.name_lowerstart + ";");
		}
		
		lines_getters.add("public " + typename + "[] get" + vn.name_upperstart + "(){return " + vn.name_lowerstart + ";}");
		
		VarName lenn = processVariableName(data_field.getLengthString());
		String len = lenn.name_lowerstart;
		String[] rfunc_lines = null;
		int j = 0;
		try{
			int ilen = Integer.parseInt(len);
			lines_constr.add(vn.name_lowerstart + " = new " + typename + "[" + ilen + "];");
			lines_setters.add("public void set" + vn.name_upperstart + "(int index, " + typename + " value){" + vn.name_lowerstart + "[index] = value;}");
			
			rfunc_lines = new String[4];
			rfunc_lines[j++] = "for(int i = 0; i < " + ilen + "; i++){";
		}
		catch(NumberFormatException ex){
			lines_setters.add("public void alloc" + vn.name_upperstart + "(int size){" + vn.name_lowerstart + " = new " + typename + "[size]; " + len + " = size;}");
			lines_setters.add("public void set" + vn.name_upperstart + "(int index, " + typename + " value){" + vn.name_lowerstart + "[index] = value;}");
			
			rfunc_lines = new String[5];
			rfunc_lines[j++] = vn.name_lowerstart + " = new " + typename + "[" + len + "];";
			rfunc_lines[j++] = "for(int i = 0; i < " + len + "; i++){";
		}
		rfunc_lines[j++] = "\t" + vn.name_lowerstart + " = new " + typename + "();";
		rfunc_lines[j++] = "\t" + vn.name_lowerstart + "[i]." + FUNCNAME_READER + "(" + VARNAME_INDATA + ");";
		rfunc_lines[j++] = "}";
		writeStructArrayReadlines(data_field, rfunc_lines);
		
		current_block.calc_func_lines.add("return " + len + ";");
		current_block.writr_func_lines.add("for(int i = 0; i < " + len + "; i++){ " + VARNAME_WRITER_COUNT + " += " + vn.name_lowerstart + "." + FUNCNAME_WRITER + "(" + WRITER_BUFFERNAME + ");}");
	
		current_block.dirty = true;
		if(DEBUG_MODE){
			System.err.println("BingenJavaTarget.addStructArray || Struct array added: " + data_field.getName());
		}
	}
	
	public void resolveEnumImports(Map<String, EnumDef> enum_map){
		for(String tname : unk_types){
			if(tname.startsWith("Enum:")){
				EnumDef def = enum_map.get(tname);
				String enum_type_name = tname.replace("Enum:", "");
				String enum_sub_str = "${" + enum_type_name.toUpperCase() + "}";
				
				String prim_name = null;
				String infunc = null;
				int primsize = 0;
				
				PrimType primtype = parsePrimitiveType(def.getTypeString());
				switch(primtype.type_b){
				case 8:
					prim_name = "byte";
					infunc = "nextByte";
					primsize = 1;
					break;
				case 16:
					prim_name = "short";
					infunc = "nextShort";
					primsize = 2;
					break;
				case 32:
					prim_name = "int";
					infunc = "nextInt";
					primsize = 4;
					break;
				case 64:
					prim_name = "long";
					infunc = "nextLong";
					primsize = 8;
					break;
				default:
					System.err.println("BingenJavaTarget.resolveEnumImports || Enum type \"" + def.getTypeString() + "\" not recognized!");
					continue;
				}
				
				for(String line : lines_instvars)line.replace(enum_sub_str, prim_name);
				for(String line : lines_getters)line.replace(enum_sub_str, prim_name);
				for(String line : lines_setters)line.replace(enum_sub_str, prim_name);
				for(String line : lines_rfunc)line.replace(enum_sub_str, infunc);
				for(BlockInfo block : ser_blocks){
					int eocc = block.getEnumOccCount(enum_type_name);
					block.base_alloc += (eocc * primsize);
				}
				
				resolveEnumImportCallback(def);
			}
		}
	}
	
	public void resolveImports(Map<String, BingennerTypedef> type_map){
		for(String tname : unk_types){
			if(!tname.startsWith("Enum:")){
				BingennerTypedef tdef = type_map.get(tname);
				BingennerPackage pkg = tdef.getPackage();
				if(pkg != null){
					String pname = pkg.getFullPackageString();
					if(!pname.equals(this.current_pkg_string)){
						imports.add(pname + "." + tdef.getName());
					}
				}
				resolveImportCallback(tdef);
			}
		}

		unk_types.clear();
	}

	/*----- Static Util -----*/
	
	public static String resolveEnumTypeClassName(String input_str){
		if(input_str == null) return null;
		String ename = input_str.replace("Enum:", "");
		String cap = ename.substring(0,1).toUpperCase();
		return cap + ename.substring(1);
	}
	
}
