package waffleoRai_Encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import waffleoRai_Utils.FileBuffer;

public class AES {
	
	//Yeah, I don't feel like debugging it.
	//Let's see what the Java libraries have...
	//https://www.novixys.com/blog/java-aes-example/
	
	/* ----- Constants ----- */
	
	//public static final String CIPHER_TRANSFORMATION_CBC = "AES/CBC/NoPadding";
	//public static final String CIPHER_TRANSFORMATION_CBC_PADDING = "AES/CBC/PKCS5Padding";
	
	//https://www.samiam.org/galois.html
	public static final int[] TBL_LOG = {
			0x00, 0xff, 0xc8, 0x08, 0x91, 0x10, 0xd0, 0x36, 
			0x5a, 0x3e, 0xd8, 0x43, 0x99, 0x77, 0xfe, 0x18, 
			0x23, 0x20, 0x07, 0x70, 0xa1, 0x6c, 0x0c, 0x7f, 
			0x62, 0x8b, 0x40, 0x46, 0xc7, 0x4b, 0xe0, 0x0e, 
			0xeb, 0x16, 0xe8, 0xad, 0xcf, 0xcd, 0x39, 0x53, 
			0x6a, 0x27, 0x35, 0x93, 0xd4, 0x4e, 0x48, 0xc3, 
			0x2b, 0x79, 0x54, 0x28, 0x09, 0x78, 0x0f, 0x21, 
			0x90, 0x87, 0x14, 0x2a, 0xa9, 0x9c, 0xd6, 0x74, 
			0xb4, 0x7c, 0xde, 0xed, 0xb1, 0x86, 0x76, 0xa4, 
			0x98, 0xe2, 0x96, 0x8f, 0x02, 0x32, 0x1c, 0xc1, 
			0x33, 0xee, 0xef, 0x81, 0xfd, 0x30, 0x5c, 0x13, 
			0x9d, 0x29, 0x17, 0xc4, 0x11, 0x44, 0x8c, 0x80, 
			0xf3, 0x73, 0x42, 0x1e, 0x1d, 0xb5, 0xf0, 0x12, 
			0xd1, 0x5b, 0x41, 0xa2, 0xd7, 0x2c, 0xe9, 0xd5, 
			0x59, 0xcb, 0x50, 0xa8, 0xdc, 0xfc, 0xf2, 0x56, 
			0x72, 0xa6, 0x65, 0x2f, 0x9f, 0x9b, 0x3d, 0xba, 
			0x7d, 0xc2, 0x45, 0x82, 0xa7, 0x57, 0xb6, 0xa3, 
			0x7a, 0x75, 0x4f, 0xae, 0x3f, 0x37, 0x6d, 0x47, 
			0x61, 0xbe, 0xab, 0xd3, 0x5f, 0xb0, 0x58, 0xaf, 
			0xca, 0x5e, 0xfa, 0x85, 0xe4, 0x4d, 0x8a, 0x05, 
			0xfb, 0x60, 0xb7, 0x7b, 0xb8, 0x26, 0x4a, 0x67, 
			0xc6, 0x1a, 0xf8, 0x69, 0x25, 0xb3, 0xdb, 0xbd, 
			0x66, 0xdd, 0xf1, 0xd2, 0xdf, 0x03, 0x8d, 0x34, 
			0xd9, 0x92, 0x0d, 0x63, 0x55, 0xaa, 0x49, 0xec, 
			0xbc, 0x95, 0x3c, 0x84, 0x0b, 0xf5, 0xe6, 0xe7, 
			0xe5, 0xac, 0x7e, 0x6e, 0xb9, 0xf9, 0xda, 0x8e, 
			0x9a, 0xc9, 0x24, 0xe1, 0x0a, 0x15, 0x6b, 0x3a, 
			0xa0, 0x51, 0xf4, 0xea, 0xb2, 0x97, 0x9e, 0x5d, 
			0x22, 0x88, 0x94, 0xce, 0x19, 0x01, 0x71, 0x4c, 
			0xa5, 0xe3, 0xc5, 0x31, 0xbb, 0xcc, 0x1f, 0x2d, 
			0x3b, 0x52, 0x6f, 0xf6, 0x2e, 0x89, 0xf7, 0xc0, 
			0x68, 0x1b, 0x64, 0x04, 0x06, 0xbf, 0x83, 0x38 };
	
	public static final int[] TBL_ANTILOG = {
			0x01, 0xe5, 0x4c, 0xb5, 0xfb, 0x9f, 0xfc, 0x12, 
			0x03, 0x34, 0xd4, 0xc4, 0x16, 0xba, 0x1f, 0x36, 
			0x05, 0x5c, 0x67, 0x57, 0x3a, 0xd5, 0x21, 0x5a, 
			0x0f, 0xe4, 0xa9, 0xf9, 0x4e, 0x64, 0x63, 0xee, 
			0x11, 0x37, 0xe0, 0x10, 0xd2, 0xac, 0xa5, 0x29, 
			0x33, 0x59, 0x3b, 0x30, 0x6d, 0xef, 0xf4, 0x7b, 
			0x55, 0xeb, 0x4d, 0x50, 0xb7, 0x2a, 0x07, 0x8d, 
			0xff, 0x26, 0xd7, 0xf0, 0xc2, 0x7e, 0x09, 0x8c, 
			0x1a, 0x6a, 0x62, 0x0b, 0x5d, 0x82, 0x1b, 0x8f, 
			0x2e, 0xbe, 0xa6, 0x1d, 0xe7, 0x9d, 0x2d, 0x8a, 
			0x72, 0xd9, 0xf1, 0x27, 0x32, 0xbc, 0x77, 0x85, 
			0x96, 0x70, 0x08, 0x69, 0x56, 0xdf, 0x99, 0x94, 
			0xa1, 0x90, 0x18, 0xbb, 0xfa, 0x7a, 0xb0, 0xa7, 
			0xf8, 0xab, 0x28, 0xd6, 0x15, 0x8e, 0xcb, 0xf2, 
			0x13, 0xe6, 0x78, 0x61, 0x3f, 0x89, 0x46, 0x0d, 
			0x35, 0x31, 0x88, 0xa3, 0x41, 0x80, 0xca, 0x17, 
			0x5f, 0x53, 0x83, 0xfe, 0xc3, 0x9b, 0x45, 0x39, 
			0xe1, 0xf5, 0x9e, 0x19, 0x5e, 0xb6, 0xcf, 0x4b, 
			0x38, 0x04, 0xb9, 0x2b, 0xe2, 0xc1, 0x4a, 0xdd, 
			0x48, 0x0c, 0xd0, 0x7d, 0x3d, 0x58, 0xde, 0x7c, 
			0xd8, 0x14, 0x6b, 0x87, 0x47, 0xe8, 0x79, 0x84, 
			0x73, 0x3c, 0xbd, 0x92, 0xc9, 0x23, 0x8b, 0x97, 
			0x95, 0x44, 0xdc, 0xad, 0x40, 0x65, 0x86, 0xa2, 
			0xa4, 0xcc, 0x7f, 0xec, 0xc0, 0xaf, 0x91, 0xfd, 
			0xf7, 0x4f, 0x81, 0x2f, 0x5b, 0xea, 0xa8, 0x1c, 
			0x02, 0xd1, 0x98, 0x71, 0xed, 0x25, 0xe3, 0x24, 
			0x06, 0x68, 0xb3, 0x93, 0x2c, 0x6f, 0x3e, 0x6c, 
			0x0a, 0xb8, 0xce, 0xae, 0x74, 0xb1, 0x42, 0xb4, 
			0x1e, 0xd3, 0x49, 0xe9, 0x9c, 0xc8, 0xc6, 0xc7, 
			0x22, 0x6e, 0xdb, 0x20, 0xbf, 0x43, 0x51, 0x52, 
			0x66, 0xb2, 0x76, 0x60, 0xda, 0xc5, 0xf3, 0xf6, 
			0xaa, 0xcd, 0x9a, 0xa0, 0x75, 0x54, 0x0e, 0x01 };
	
	public static final int[] SHIFT_ROWS_MAP = {0, 5, 10, 15, 4, 9, 14, 3, 8, 13, 2, 7, 12, 1, 6, 11};
	
	/* ----- Static Variables ----- */
	
	private static int[] rcon;
	private static int[] sbox;
	
	private static int[] sbox_inv;
	
	/* ----- Instance Variables ----- */
	
	private byte[] aes_key;
	private String cipher_str;
	private String padding;
	
	private Cipher cipher;
	private SecretKeySpec skey;
	private IvParameterSpec ivspec;
	
	private byte[][] key_schedule; //Only used for explicit decryption
	
	/* ----- Construction ----- */
	
	public AES(byte[] key)
	{
		aes_key = key;
		cipher_str = "CBC";
	}
	
	/**
	 * Construct an AES encrytor/decryptor with the provided AES key
	 * as an array of bytes 0-extended to ints.
	 * <br><b>!IMPORTANT!</b> The upper 24 bits of every int provided are IGNORED!
	 * The reason this overload exists is because Java refuses to perform bit level
	 * operations on any primitive smaller than an int.
	 * @param key AES key as an array of bytes 0-extended to ints.
	 */
	public AES(int[] key)
	{
		aes_key = new byte[16];
		for(int i = 0; i < key.length; i++) aes_key[i] = (byte)key[i];
		cipher_str = "CBC";
	}
		
	/* ----- Getters ----- */
	
	public String getCipherString()
	{
		String padstr = "NoPadding";
		if(padding != null) padstr = padding;
		return "AES/" + cipher_str + "/" + padstr;
	}
	
	public boolean paddingSet()
	{
		return (padding != null);
	}
	
	/* ----- Setters ----- */
	
	public void setKey(byte[] newkey)
	{
		aes_key = newkey;
	}
	
	public void setCBC()
	{
		cipher_str = "CBC";
	}
	
	public void setCTR()
	{
		cipher_str = "CTR";
	}
	
	public void setCCM(){
		cipher_str = "CCM";
	}
	
	public void setECB(){
		cipher_str = "ECB";
	}
	
	public void setPadding(boolean b)
	{
		if(b) padding = "PKCS5Padding";
		else padding = null;
	}
	
	public void reset()
	{
		cipher = null;
		skey = null;
		ivspec = null;
	}
	
	/* ----- Rijndael ----- */
	
	//For when the built-in Java class cannot be used...
	//https://www.samiam.org/key-schedule.html
	//https://en.wikipedia.org/wiki/Advanced_Encryption_Standard
	
	private static void generateTables(){

		rcon = new int[256];
		sbox = new int[256];
		sbox_inv = new int[256];
		
		for(int i = 0; i < 256; i++){
			rcon[i] = rcon(i);
			sbox[i] = sbox(i);
			sbox_inv[sbox[i]] = i;
		}
		
	}
	
	private static int rcon(int in){
		if(in == 0) return 0;
		
		int c = 1;
		while(in != 1){
			int b = c & 0x80;
			c = (c << 1) & 0xFF;
			if(b == 0x80) c ^= 0x1b;
			in--;
		}
		
		return c;
	}
	
	private static int sbox(int in){
		int s = 0;
		if(in != 0) s = TBL_ANTILOG[255 - TBL_LOG[in]];
		int x = s;
		for(int c = 0; c < 4; c++){
			//s = s ROL 1
			s = ((s << 1) & 0xFF) | (s >>> 7);
			x ^= s;
		}
		x ^= 0x63;
		//System.err.println("sbox[" + in + "] = " + Integer.toHexString(x));
		return x;
	}
	
	protected static int[] xorArr(byte[] x, byte[] y){
		if(x == null){
			if(y == null) return null;
			x = new byte[y.length];
		}
		if(y == null){y = new byte[x.length];}
		int len = x.length;
		if(y.length < len) len = y.length;
		
		int[] out = new int[len];
		for(int i = 0; i < len; i++){
			int bx = Byte.toUnsignedInt(x[i]);
			int by = Byte.toUnsignedInt(y[i]);
			int xor = bx ^ by;
			out[i] = xor;
		}
		
		return out;
	}
	
	private static int[] xorArr(int[] x, byte[] y){
		if(x == null){
			if(y == null) return null;
			x = new int[y.length];
		}
		if(y == null){return x;}
		int len = x.length;
		if(y.length < len) len = y.length;
		
		int[] out = new int[len];
		for(int i = 0; i < len; i++){
			int bx = x[i];
			int by = Byte.toUnsignedInt(y[i]);
			int xor = bx ^ by;
			out[i] = xor;
		}
		
		return out;
	}
	
	private static int gmul(int a, int b){
		
		if(a == 0 || b == 0) return 0;
		int s = (TBL_LOG[a] + TBL_LOG[b]) % 255;
		s = TBL_ANTILOG[s];

		return s;
	}
	
	public void genKeySchedule(){
		if(aes_key == null) return;
		if(rcon == null) generateTables();
		
		int slots = 11;
		int bcount = 176;
		int kbytes = aes_key.length;
		switch(kbytes){
		case 24: slots = 9; bcount = 208; break;
		case 32: slots = 8; bcount = 240; break;
		}
		
		//key_schedule = new byte[slots][kbytes];
		FileBuffer buff = new FileBuffer(slots * kbytes);
		
		//Copy key for first set
		int[] row = new int[kbytes];
		int[] lastrow = new int[kbytes];
		for(int i = 0; i < kbytes; i++){
			//key_schedule[0][i] = aes_key[i];
			buff.addToFile(aes_key[i]);
			lastrow[i] = Byte.toUnsignedInt(aes_key[i]);
		}
		
		int[] temp = new int[4];
		int[] last4 = new int[4];
		for(int i = 0; i < 4; i++) last4[i] = Byte.toUnsignedInt(aes_key[(kbytes-4)+i]);
		
		int words = kbytes >>> 2;
		for(int i = 1; i < slots; i++){
			//Per row
			int c = 0;
			
			//First word
			for(int k = 0; k < 4; k++) temp[k] = last4[k];
			
			//Do the complex thing
			//--- temp ROL 8
			int msb = temp[0];
			for(int k = 0; k < 3; k++) temp[k] = temp[k+1];
			temp[3] = msb;
			//--- sbox
			for(int k = 0; k < 4; k++) temp[k] = sbox[temp[k]];
			//--- rcon
			temp[0] ^= rcon[i];
			
			//Do the regular thing
			for(int k = 0; k < 4; k++){
				row[c] = last4[k] = (lastrow[c] ^ temp[k]);
				c++;
			}
			
			//Other words
			for(int j = 1; j < words; j++){
				//Per word
					for(int k = 0; k < 4; k++){
						//Per byte
						temp[k] = last4[k];
						if(kbytes == 32 && j == 4) temp[k] = sbox[temp[k]];
						row[c] = last4[k] = (lastrow[c] ^ temp[k]);
						c++;
					}
			}
			
			//Copy back to key schedule and lastrow
			for(int j = 0; j < kbytes; j++){
				//key_schedule[i+1][j] = (byte)row[j];
				buff.addToFile((byte)row[j]);
				lastrow[j] = row[j];
			}
		}
		
		//Restructure into rows of 16
		int rows = bcount >>> 4;
		key_schedule = new byte[rows][16];
		buff.setCurrentPosition(0);
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < 16; j++) key_schedule[i][j] = buff.nextByte();
		}
		
	}
	
	public byte[] rijndael_enc(byte[] in){
		if(key_schedule == null) genKeySchedule();
		
		//Determine number of rounds
		int rounds = 9;
		switch(aes_key.length){
		case 24: rounds = 11; break;
		case 32: rounds = 13; break;
		}
		
		//Initial add key
		int kidx = 0;
		int[] temp = xorArr(in, key_schedule[kidx++]);
		
		//Rounds
		for(int i = 0; i < rounds; i++){
			//Sub bytes
			for(int j = 0; j < 16; j++) temp[j] = sbox[temp[j]];
			
			//Shift rows
			int[] temp2 = new int[16];
			for(int j = 0; j < 16; j++) temp2[j] = temp[SHIFT_ROWS_MAP[j]];
			
			//Mix columns
			for(int j = 0; j < 4; j++){
				int base = j << 2;
				int[] a = new int[4];
				int[] b = new int[4];
				for(int k = 0; k < 4; k++){
					a[k] = temp2[base+k];
					b[k] = gmul(temp2[base+k], 2);
				}	
				
				temp[base+0] = b[0] ^ a[3] ^ a[2] ^ b[1] ^ a[1];
				temp[base+1] = b[1] ^ a[0] ^ a[3] ^ b[2] ^ a[2];
				temp[base+2] = b[2] ^ a[1] ^ a[0] ^ b[3] ^ a[3];
				temp[base+3] = b[3] ^ a[2] ^ a[1] ^ b[0] ^ a[0];
			}
			
			//Add round key
			temp = xorArr(temp, key_schedule[kidx++]);
		}
		
		//Final round
		//Sub bytes
		for(int j = 0; j < 16; j++) temp[j] = sbox[temp[j]];
		
		//Shift rows
		int[] temp2 = new int[16];
		for(int j = 0; j < 16; j++) temp2[j] = temp[SHIFT_ROWS_MAP[j]];
		
		//Add round key
		temp = xorArr(temp2, key_schedule[kidx++]);
		
		byte[] out = new byte[16];
		for(int i = 0; i < 16; i++) out[i] = (byte)temp[i];
		
		return out;
	}
	
	public byte[] rijndael_dec(byte[] in){
		if(key_schedule == null) genKeySchedule();
		
		//Determine number of rounds
		int rounds = 9;
		switch(aes_key.length){
		case 24: rounds = 11; break;
		case 32: rounds = 13; break;
		}
		
		//Initial add key
		int kidx = rounds+1;
		int[] temp = xorArr(in, key_schedule[kidx--]);
		
		//Rounds
		for(int i = 0; i < rounds; i++){
			
			//Shift rows (inv)
			int[] temp2 = new int[16];
			for(int j = 0; j < 16; j++) temp2[SHIFT_ROWS_MAP[j]] = temp[j];
			
			//Sub bytes (inv)
			for(int j = 0; j < 16; j++) temp[j] = sbox_inv[temp2[j]];
			
			//Add round key
			temp = xorArr(temp, key_schedule[kidx--]);
			
			//Mix columns
			for(int j = 0; j < 4; j++){
				int base = j << 2;
				int[] a = new int[4];
				for(int k = 0; k < 4; k++){
					a[k] = temp[base+k];
				}	
				
				temp[base+0] = gmul(a[0], 14) ^ gmul(a[3], 9) ^ gmul(a[2], 13) ^ gmul(a[1], 11);
				temp[base+1] = gmul(a[1], 14) ^ gmul(a[0], 9) ^ gmul(a[3], 13) ^ gmul(a[2], 11);
				temp[base+2] = gmul(a[2], 14) ^ gmul(a[1], 9) ^ gmul(a[0], 13) ^ gmul(a[3], 11);
				temp[base+3] = gmul(a[3], 14) ^ gmul(a[2], 9) ^ gmul(a[1], 13) ^ gmul(a[0], 11);
			}
			
		}
		
		//Final round
		//Shift rows (inv)
		int[] temp2 = new int[16];
		for(int j = 0; j < 16; j++) temp2[SHIFT_ROWS_MAP[j]] = temp[j];
		
		//Sub bytes (inv)
		for(int j = 0; j < 16; j++) temp[j] = sbox_inv[temp2[j]];
		
		//Add round key
		temp = xorArr(temp, key_schedule[kidx]);
		
		byte[] out = new byte[16];
		for(int i = 0; i < 16; i++) out[i] = (byte)temp[i];
		
		return out;
	}
	
	public void printKeyScheduleToStdErr(){
		//Debug
		if(key_schedule == null){
			System.err.println("Null Key Schedule!");
			return;
		}
		
		int rounds = key_schedule.length;
		int rbytes = 16;
		
		for(int i = 0; i < rounds; i++){
			for(int j = 0; j < rbytes; j++){
				System.err.print(String.format("%02x ", key_schedule[i][j]));
			}
			System.err.println();
		}
		
	}
	
	/* ----- Decryption ----- */
	
	public byte[] decrypt(byte[] iv, byte[] in)
	{
		try {
			Cipher cipher = Cipher.getInstance(getCipherString());
			SecretKeySpec skey = new SecretKeySpec(aes_key, "AES");
			
			if(cipher_str.equals("ECB")){
				cipher.init(Cipher.DECRYPT_MODE, skey);
			}
			else{
				IvParameterSpec ivspec = new IvParameterSpec(iv);
				cipher.init(Cipher.DECRYPT_MODE, skey, ivspec);
			}
			
			return cipher.doFinal(in);
		} 
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} 
		catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} 
		catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		} 
		catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			return null;
		} 
		catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return null;
		} 
		catch (BadPaddingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean initDecrypt(byte[] iv)
	{
		try 
		{
			cipher = Cipher.getInstance(getCipherString());
			skey = new SecretKeySpec(aes_key, "AES");
			
			if(cipher_str.equals("ECB")){
				cipher.init(Cipher.DECRYPT_MODE, skey);
			}
			else{
				ivspec = new IvParameterSpec(iv);
				cipher.init(Cipher.DECRYPT_MODE, skey, ivspec);	
			}
		} 
		catch (NoSuchAlgorithmException e) 
		{
			e.printStackTrace();
			return false;
		} 
		catch (NoSuchPaddingException e)
		{
			e.printStackTrace();
			return false;
		} 
		catch (InvalidKeyException e) 
		{
			e.printStackTrace();
			return false;
		} 
		catch (InvalidAlgorithmParameterException e) 
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public byte[] decryptBlock(byte[] in, boolean finalBlock)
	{
		if(cipher == null) return null;
		
		if(finalBlock)
		{
			try {return cipher.doFinal(in);} 
			catch (IllegalBlockSizeException e) {e.printStackTrace(); return null;} 
			catch (BadPaddingException e) {e.printStackTrace(); return null;} 
		}
		return cipher.update(in);
		
	}
	
	/* ----- Encryption ----- */
	
	public byte[] encrypt(byte[] iv, byte[] in)
	{
		try 
		{
			Cipher cipher = Cipher.getInstance(getCipherString());
			SecretKeySpec skey = new SecretKeySpec(aes_key, "AES");
			if(cipher_str.equals("ECB")){
				cipher.init(Cipher.ENCRYPT_MODE, skey);	
			}
			else{
				IvParameterSpec ivspec = new IvParameterSpec(iv);
				cipher.init(Cipher.ENCRYPT_MODE, skey, ivspec);	
			}
			return cipher.doFinal(in);
		} 
		catch (NoSuchAlgorithmException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (NoSuchPaddingException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (InvalidKeyException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (InvalidAlgorithmParameterException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (IllegalBlockSizeException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (BadPaddingException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/* ----- Util ----- */
	
	public static byte[] str2Key(String s){
		byte[] arr = new byte[s.length() >>> 1];
		int cpos = 0;
		for(int i = 0; i < arr.length; i++){
			String bstr = s.substring(cpos, cpos+2);
			arr[i] = (byte)Integer.parseInt(bstr, 16);
			cpos+=2;
		}
		
		return arr;
	}
	
	public static String bytes2str(byte[] byte_arr){
		if(byte_arr == null) return "<NULL>";
		
		int chars = byte_arr.length << 1;
		StringBuilder sb = new StringBuilder(chars+2);
		
		for(int i = 0; i < byte_arr.length; i++) sb.append(String.format("%02x", byte_arr[i]));
		
		return sb.toString();
	}
	
}
