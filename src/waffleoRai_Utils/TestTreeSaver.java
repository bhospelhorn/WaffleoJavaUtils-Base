package waffleoRai_Utils;

public class TestTreeSaver {

	
	
	public static void main(String[] args) 
	{
		String testpath = "C:\\Users\\Blythe\\AppData\\Local\\waffleorai\\NTDExplorer\\projects\\NTR_ADAE_USA\\customtree.bin";

		try
		{
			DirectoryNode root = FileTreeSaver.loadTree(testpath);
			if(root != null); //This just gets rid of compiler warning >:)
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
	}

}
