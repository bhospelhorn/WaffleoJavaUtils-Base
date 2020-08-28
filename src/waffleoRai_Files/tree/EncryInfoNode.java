package waffleoRai_Files.tree;

import waffleoRai_Files.EncryptionDefinition;

class EncryInfoNode {

	public EncryptionDefinition def;
	public long offset;
	public long length;
	
	public EncryInfoNode(EncryptionDefinition d, long off, long len){
		def = d; offset = off; length = len;
	}
	
}
