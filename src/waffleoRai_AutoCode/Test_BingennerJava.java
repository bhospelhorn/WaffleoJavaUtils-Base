package waffleoRai_AutoCode;

public class Test_BingennerJava {

	//TODO Needs to recognize "ByteOrder" field
	//TODO Recognize "DefaultValue" field
	//TODO RIFF like block variant genner
	//TODO TS3 (PropertyStream handling) variant genner
	//TODO Handle enums?
	//TODO Handle anonymous inner structs
	
	public static void main(String[] args) {

		String input_path = "C:\\Users\\Blythe\\Documents\\Game Stuff\\TS3\\file_types\\common_dataformats";
		String output_path = "C:\\Users\\Blythe\\Documents\\Game Stuff\\TS3\\file_types\\java";
		
		try{
			BingennerJava gen = new BingennerJava();
			System.err.println("Reading in...\n");
			gen.readFrom(input_path);
			gen.DEBUG_printToStderr();
			System.err.println("\nOutputting...");
			gen.outputTo(output_path);
			
		}catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
		
	}

}
