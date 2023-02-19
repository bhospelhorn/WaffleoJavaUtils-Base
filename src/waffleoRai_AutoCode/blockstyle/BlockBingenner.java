package waffleoRai_AutoCode.blockstyle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import waffleoRai_AutoCode.Bingenner;
import waffleoRai_AutoCode.BingennerJava;
import waffleoRai_AutoCode.BingennerPackage;
import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.DataFieldDef;
import waffleoRai_AutoCode.typedefs.EnumDef;
import waffleoRai_AutoCode.typedefs.EnumVal;

public class BlockBingenner extends Bingenner{
	
	private static final boolean DEBUG_MODE = true;
	
	/*----- Constants -----*/
	
	public static final int TARGET_UNSET = 0;
	public static final int TARGET_JAVA = 1;
	public static final int TARGET_C = 2;
	public static final int TARGET_CSHARP = 3;
	
	/*----- Instance Variables -----*/
	
	private int target_lan = TARGET_UNSET;
	
	/*----- Getters -----*/
	
	/*----- Setters -----*/
	
	public void setTargetLanguage(int val){target_lan = val;}
	
	/*----- Language Specific -----*/
	
	protected void writeJavaEnumFile(EnumDef def) throws IOException{
		String e_name = BingennerJava.resolveEnumTypeClassName(def.getName());
		BingennerPackage pkg = def.getPackage();
		String desc = def.getDescription();
		
		//Figure out type
		String e_type = def.getTypeString();
		String putType = null;
		int size_b = 0;
		boolean sixtyfour = false;
		if(e_type.equalsIgnoreCase("u8") || e_type.equalsIgnoreCase("s8")){
			putType = "byte";
			size_b = 1;
		}
		else if(e_type.equalsIgnoreCase("u16") || e_type.equalsIgnoreCase("s16")){
			putType = "short";
			size_b = 2;
		}
		else if(e_type.equalsIgnoreCase("u32") || e_type.equalsIgnoreCase("s32")){
			putType = "int";
			size_b = 4;
		}
		else if(e_type.equalsIgnoreCase("u64") || e_type.equalsIgnoreCase("s64")){
			putType = "long";
			sixtyfour = true;
			size_b = 8;
		}
		else{
			System.err.println("BlockBingenner.writeJavaEnumFile || Type \"" + e_type + "\" not currently supported for enums. Skipping.");
			return;
		}
		
		String outpath = super.current_output_dir + File.separator + e_name + ".java";
		BufferedWriter bw = new BufferedWriter(new FileWriter(outpath));
		bw.write("package " + pkg.getFullPackageString() + ";\n\n");
		if(desc != null){
			bw.write("/*" + desc + "*/\n");
		}
		bw.write("public class " + e_name + "{\n\n");
		
		//Size
		bw.write("\tpublic static final int ");
		bw.write(BlockBgJavaTarget.CONSTNAME_ENUMSIZE);
		bw.write(" = " + size_b);
		bw.write(";\n\n");
		
		int valcount = def.getValueCount();
		for(int i = 0; i < valcount; i++){
			EnumVal eval = def.getValue(i);
			String v = eval.getValueString();
			bw.write("\tpublic static final ");
			bw.write(putType);
			bw.write(" ");
			bw.write(eval.getName());
			bw.write(" = ");
			bw.write(v);
			if(sixtyfour && !v.endsWith("L")) bw.write("L");
			bw.write(";");
			desc = eval.getDescription();
			if(desc != null){
				bw.write("//" + desc);
			}
			bw.write("\n");
		}
		
		bw.write("\n}\n");
		bw.close();
	}
	protected void outputTypeJava(BingennerTypedef def) throws IOException{
		BlockBgJavaTarget javawriter = new BlockBgJavaTarget();
		switch(def.getPreferredByteOrder()){
		case Bingenner.BYTEORDER_BIG:
			javawriter.setOutputBigEndian(true);
			break;
		case Bingenner.BYTEORDER_LITTLE:
			javawriter.setOutputBigEndian(false);
			break;
		case Bingenner.BYTEORDER_SYSTEM:
			javawriter.setOutputBigEndian(Bingenner.getTargetSystemByteOrder());
			break;
		case Bingenner.BYTEORDER_PARENT:
			javawriter.setOutputBigEndian(getByteOrder());
			break;
		}
		
		//Get block ID and version info.
		String block_id = def.getMiscAttribute(BlockBgJavaTarget.XMLKEY_BLOCKID);
		if(block_id != null && !block_id.isEmpty()){
			short ver_maj = 0;
			short ver_min = 0;
			String verstr = def.getMiscAttribute(BlockBgJavaTarget.XMLKEY_VER_NOW);
			if(verstr != null){
				if(verstr.contains(".")){
					int dotidx = verstr.indexOf('.');
					String ver_a = verstr.substring(0, dotidx);
					String ver_b = verstr.substring(dotidx+1);
					try{
						ver_maj = Short.parseShort(ver_a);
						ver_min = Short.parseShort(ver_b);
					}
					catch(NumberFormatException ex){
						System.err.println("WARNING: Version string \"" + verstr + "\" for type " + def.getName() + "does not contain a valid number!");
						ex.printStackTrace();
					}
				}
				else{
					//Assumed just major
					try{
						ver_maj = Short.parseShort(verstr);
					}
					catch(NumberFormatException ex){
						System.err.println("WARNING: Version string \"" + verstr + "\" for type " + def.getName() + "does not contain a valid number!");
						ex.printStackTrace();
					}
				}
			}
			
			//Adjust block ID to four characters
			int blockid_len = block_id.length();
			if(blockid_len > 4){
				block_id = block_id.substring(0,4);
			}
			if(blockid_len < 4){
				while(block_id.length() < 4) block_id += " ";
			}
			
			javawriter.setBlockInformation(block_id, ver_maj, ver_min);
		}
		
		
		//Open
		javawriter.openDoc(current_output_dir, def);
		
		//Add top level fields
		DataFieldDef[] fields = def.getFields();
		if(fields != null){
			for(DataFieldDef f : fields){
				f.addTo(javawriter);
			}
		}
		
		//Resolve type imports
		javawriter.resolveEnumImports(loaded_enums);
		javawriter.resolveImports(loaded_types);
		
		//Close
		javawriter.closeCurrentDoc();
	}
	
	/*----- Superclass Interface -----*/

	protected void newPackage(BingennerPackage pkg) throws IOException {
		//Determine file path
		String pkgfull = pkg.getFullPackageString();
		String[] levels = pkgfull.split("\\.");
		if(DEBUG_MODE){
			System.err.println("BingennerJava.newPackage || Package Full Name: " + pkgfull);
		}
		String package_path = output_root.toAbsolutePath().toString();
		for(int i = 0; i < levels.length; i++){
			package_path += File.separator;
			package_path += levels[i];
		}
		
		if(DEBUG_MODE){
			System.err.println("BingennerJava.newPackage || Package Output: " + package_path);
		}
		
		//Create directory
		Path pkgdir = Paths.get(package_path);
		if(!Files.isDirectory(pkgdir)){
			Files.createDirectories(pkgdir);
		}
		
		//Set
		super.current_output_dir = package_path;
	}

	protected void outputType(BingennerTypedef def) throws IOException {
		switch(target_lan){
		case TARGET_JAVA:
			outputTypeJava(def);
			break;
		}
	}

	protected void outputEnumType(EnumDef def) throws IOException {
		switch(target_lan){
		case TARGET_JAVA:
			writeJavaEnumFile(def);
			break;
		}
	}

}
