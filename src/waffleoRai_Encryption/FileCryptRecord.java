package waffleoRai_Encryption;

public abstract class FileCryptRecord {

	private int key_type;
	private int key_index;
	
	private long file_uid;
	private long crypt_offset; //Offset at which decryption must start for accurate results.
	
	
	public FileCryptRecord(long fileuid){
		file_uid = fileuid;
	}
	
	public int getKeyType(){return key_type;}
	public int getKeyIndex(){return key_index;}
	public long getFileUID(){return file_uid;}
	public long getCryptOffset(){return crypt_offset;}
	
	public void setKeyType(int i){key_type = i;}
	public void setKeyIndex(int i){key_index = i;}
	public void setCryptOffset(long off){crypt_offset = off;}
	
	public abstract int getCryptType();
	public abstract void setCryptType(int type);
	
	public abstract boolean hasIV();
	public abstract byte[] getIV();
	public abstract void setIV(byte[] iv);
	
}
