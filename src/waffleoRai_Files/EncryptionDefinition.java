package waffleoRai_Files;

import java.io.OutputStream;
import java.util.List;

import waffleoRai_Utils.StreamWrapper;

public interface EncryptionDefinition {

	public int getID();
	public String getDescription();
	public void setDescription(String s);
	
	public void setStateValue(int key, int value);
	public int getStateValue(int key);
	
	public boolean decrypt(StreamWrapper input, OutputStream output, List<byte[]> keydata);
	
	public boolean encrypt(StreamWrapper input, OutputStream stream, List<byte[]> keydata);
	
	public int[] getExpectedKeydataSizes();
	
	public boolean unevenIOBlocks();
	
}
