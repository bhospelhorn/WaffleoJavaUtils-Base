package waffleoRai_soundbank.dls;

import waffleoRai_soundbank.DLSFile;

public class Test_ReadDLS {

	public static void main(String[] args) {
		if(args.length < 1){
			System.err.println("Need input file path!");
			System.exit(1);
		}
		
		try{
			String inpath = args[0];
			DLSFile dls = DLSFile.readDLS(inpath);
			
			System.err.println("Hold");
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}

	}

}
