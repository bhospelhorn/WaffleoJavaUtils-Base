package waffleoRai_Files;

import waffleoRai_Utils.DirectoryNode;
import waffleoRai_Utils.FileTreeSaver;

public class TestFileTreeSaver {

	public static void main(String[] args) {

		String treepath = "C:\\Users\\Blythe\\AppData\\Local\\waffleorai\\NTDExplorer\\projects\\SLPM_87176\\customtree.bin";
		
		try{
			DirectoryNode tree = FileTreeSaver.loadTree(treepath);
			tree.printMeToStdErr(0);
		}
		catch(Exception x){
			x.printStackTrace();
			System.exit(1);
		}
		

	}

}
