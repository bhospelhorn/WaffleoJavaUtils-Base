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
		
		/*String key_str = "";
		String iv_str = "";
		String enc_str = "";
		
		byte[] key = AES.str2Key(key_str);
		byte[] iv = AES.str2Key(iv_str);
		byte[] enc = AES.str2Key(enc_str);
		
		System.err.println("Key: " + AES.bytes2str(key));
		System.err.println("IV: " + AES.bytes2str(iv));
		System.err.println("CT: " + AES.bytes2str(enc));
		
		AESXTS xts = new AESXTS(key);
		byte[] dec = xts.decrypt(iv, enc);
		System.err.println("PT: " + AES.bytes2str(dec));*/

	}

}
