package waffleoRai_AutoCode.blockstyle;

import java.io.IOException;

import waffleoRai_AutoCode.Bingenner;
import waffleoRai_AutoCode.BingennerPackage;
import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.EnumDef;

public class BlockBingenner extends Bingenner{
	
	//TODO Java enum output needs to include a constant defining enum size in bytes.
	
	/*----- Constants -----*/
	
	public static final int TARGET_UNSET = 0;
	public static final int TARGET_JAVA = 1;
	public static final int TARGET_C = 2;
	public static final int TARGET_CSHARP = 3;
	
	/*----- Instance Variables -----*/
	
	/*----- Superclass Interface -----*/

	@Override
	protected void newPackage(BingennerPackage pkg) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void outputType(BingennerTypedef def) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void outputEnumType(EnumDef def) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
