package waffleoRai_Encryption;

public class StaticDecryption {

	private static StaticDecryptor decryptor;
	
	public static void setDecryptorState(StaticDecryptor obj){decryptor = obj;}
	public static StaticDecryptor getDecryptorState(){return decryptor;}
	
}
