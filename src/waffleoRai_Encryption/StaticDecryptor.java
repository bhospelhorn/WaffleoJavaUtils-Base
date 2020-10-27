package waffleoRai_Encryption;

import waffleoRai_Files.tree.FileNode;

public interface StaticDecryptor {
	
	public FileNode decrypt(FileNode node);
	public DecryptorMethod generateDecryptor(FileNode node);

	public void dispose();
	
}
