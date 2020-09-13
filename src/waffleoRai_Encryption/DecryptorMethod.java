package waffleoRai_Encryption;

public interface DecryptorMethod {
	
	public byte[] decrypt(byte[] input, long offval);
	public void adjustOffsetBy(long value);

}
