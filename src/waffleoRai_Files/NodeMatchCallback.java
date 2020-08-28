package waffleoRai_Files;

import waffleoRai_Files.tree.FileNode;

public interface NodeMatchCallback {
	
	public boolean meetsCondition(FileNode n);

}
