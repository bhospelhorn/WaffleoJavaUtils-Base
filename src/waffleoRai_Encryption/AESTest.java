package waffleoRai_Encryption;

public class AESTest {

	public static void main(String[] args) {
		
		/*byte[] zerokey = new byte[16];
		byte[] dat = new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 
				(byte)0x88, (byte)0x99, (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0xff};
		//byte[] dat = new byte[16];
		
		AES aes = new AES(zerokey);
		aes.setCBC();
		aes.genKeySchedule();
		
		byte[] enc1 = aes.rijndael_enc(dat);
		byte[] dec1 = aes.rijndael_dec(enc1);
		
		System.err.println("Enc1:");
		for(int i = 0; i < 16; i++){
			System.err.print(String.format("%02x ", enc1[i]));
		}
		System.err.println();
		
		System.err.println("Dec1:");
		for(int i = 0; i < 16; i++){
			System.err.print(String.format("%02x ", dec1[i]));
		}
		System.err.println();
		
		byte[] enc2 = aes.encrypt(new byte[16], dat);
		System.err.println("Enc2:");
		for(int i = 0; i < 16; i++){
			System.err.print(String.format("%02x ", enc2[i]));
		}
		System.err.println();
		
		byte[] dec2 = aes.decrypt(new byte[16], enc2);
		System.err.println("Dec2:");
		for(int i = 0; i < 16; i++){
			System.err.print(String.format("%02x ", dec2[i]));
		}
		System.err.println();*/
		
		String key_str = "d6c4cf73c639e025654dd3232fe3aa7138f21bc8922271b4a6c0af999100b6b5e380ec7ec8da88e6816cd7f4f26e7ac0f86e4caac3be55234ebcd4347cda2fa5";
		String iv_str = "041f41fa30b78898040b5e0ecba27d2b";
		String enc_str = "d083f37a6160ac25c3229800ae0721d94bf6a9ff2f73a418544e6c787cbcd34a";
		
		byte[] key = AES.str2Key(key_str);
		byte[] iv = AES.str2Key(iv_str);
		byte[] enc = AES.str2Key(enc_str);
		
		System.err.println("Key: " + AES.bytes2str(key));
		System.err.println("IV: " + AES.bytes2str(iv));
		System.err.println("CT: " + AES.bytes2str(enc));
		
		AESXTS xts = new AESXTS(key);
		byte[] dec = xts.decrypt(iv, enc);
		System.err.println("PT: " + AES.bytes2str(dec));
	}

}
