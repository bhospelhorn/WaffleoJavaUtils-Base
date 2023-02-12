package waffleoRai_AutoCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import waffleoRai_AutoCode.java.BingenJavaTarget;
import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.DataFieldDef;
import waffleoRai_AutoCode.typedefs.EnumDef;

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
		//TODO
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
