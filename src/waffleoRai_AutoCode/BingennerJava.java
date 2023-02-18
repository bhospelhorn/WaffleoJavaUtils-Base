package waffleoRai_AutoCode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import waffleoRai_AutoCode.java.BingenJavaTarget;
import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.DataFieldDef;
import waffleoRai_AutoCode.typedefs.EnumDef;
import waffleoRai_AutoCode.typedefs.EnumVal;

public class BingennerJava extends Bingenner{
	
	private static final boolean DEBUG_MODE = true;

	public BingennerJava(){}
	
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

	protected void outputEnumType(EnumDef def) throws IOException{
		String e_name = def.getName().replace("Enum:", ""); //Shouldn't have that, but just in case.
		BingennerPackage pkg = def.getPackage();
		String desc = def.getDescription();
		
		//Figure out type
		String e_type = def.getTypeString();
		String putType = null;
		boolean sixtyfour = false;
		if(e_type.equalsIgnoreCase("u8") || e_type.equalsIgnoreCase("s8")){
			putType = "byte";
		}
		else if(e_type.equalsIgnoreCase("u16") || e_type.equalsIgnoreCase("s16")){
			putType = "short";
		}
		else if(e_type.equalsIgnoreCase("u32") || e_type.equalsIgnoreCase("s32")){
			putType = "int";
		}
		else if(e_type.equalsIgnoreCase("u64") || e_type.equalsIgnoreCase("s64")){
			putType = "long";
			sixtyfour = true;
		}
		else{
			System.err.println("BingennerJava.outputEnumType || Type \"" + e_type + "\" not currently supported for enums. Skipping.");
			return;
		}
		
		String outpath = super.current_output_dir + File.separator + e_name + ".java";
		BufferedWriter bw = new BufferedWriter(new FileWriter(outpath));
		bw.write("package " + pkg.getFullPackageString() + ";\n\n");
		if(desc != null){
			bw.write("/*" + desc + "*/\n");
		}
		bw.write("public class " + e_name + "{\n\n");
		
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
	
	protected void outputType(BingennerTypedef def) throws IOException {
		BingenJavaTarget javawriter = new BingenJavaTarget();
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

}
