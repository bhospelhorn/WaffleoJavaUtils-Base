package waffleoRai_Encryption;

import java.util.Arrays;

public class AESXTS extends AES{

	/* ----- Constants ----- */
	
	/* ----- Instance Variables ----- */
	
	private byte[] key_tweak;
	
	//private boolean steal_flag;
	private byte[] tweak;
	private byte[] lastblock; //For stealing
	
	/* ----- Construction ----- */
	
	public AESXTS(byte[] main_key, byte[] tweak_key){
		super(main_key);
		key_tweak = tweak_key;
		
		genKeySchedule();
	}
	
	/* ----- Tweak ----- */
	
	public void resetTweak(byte[] t){
		AES tcrypter = new AES(key_tweak);
		tweak = tcrypter.rijndael_enc(t);
	}
	
	private void advanceTweak(){

		long lo = 0L; long hi = 0L;
		for(int i = 0; i < 8; i++){
			lo |= Byte.toUnsignedLong(tweak[i]) << (i << 3);
			hi |= Byte.toUnsignedLong(tweak[i+8]) << (i << 3);
		}
		
		boolean highest = (hi & 0x8000000000000000L) != 0;
		
		hi = (hi << 1) | (lo >>> 63);
		lo = (lo << 1);
		if(highest) lo ^= 0x87;
		
		for(int i = 0; i < 8; i++){
			tweak[i] = (byte)((lo >>> (i << 3)) & 0xFF);
			tweak[i+8] = (byte)((hi >>> (i << 3)) & 0xFF);
		}
		
	}
	
	/* ----- Encrypt ----- */
	
	protected static byte[] xorArr_Barr(byte[] x, byte[] y){
		if(x == null){
			if(y == null) return null;
			x = new byte[y.length];
		}
		if(y == null){y = new byte[x.length];}
		int len = x.length;
		if(y.length < len) len = y.length;
		
		byte[] out = new byte[len];
		for(int i = 0; i < len; i++){
			int bx = Byte.toUnsignedInt(x[i]);
			int by = Byte.toUnsignedInt(y[i]);
			int xor = bx ^ by;
			out[i] = (byte)xor;
		}
		
		return out;
	}
	
	public boolean initEncrypt(byte[] t){
		resetTweak(t);
		lastblock = null;
		//steal_flag = false;
		return true;
	}
	
	public byte[] encryptBlock(byte[] in){

		//Steal, if needed
		if(in.length < 16){
			//steal_flag = true;
			byte[] temp = new byte[16];
			int ilen = in.length;
			for(int i = 0; i < ilen; i++) temp[i] = in[i];
			for(int i = ilen; i < 16; i++) temp[i] = lastblock[i];
			in = temp;
		}
		
		//Encrypt
		byte[] temp = xorArr_Barr(in, tweak);
		temp = rijndael_enc(temp);
		temp = xorArr_Barr(temp, tweak);
		
		//State
		lastblock = Arrays.copyOf(temp, 16);
		advanceTweak();
		
		return temp;
	}
	
	public byte[] encrypt(byte[] t, byte[] in){
		initEncrypt(t);
		if(in.length < 16) throw new UnsupportedOperationException("Cannot encrypt block smaller thatn 16 bytes");
		
		int mod = in.length % 16;
		byte[] out = new byte[in.length];
		
		int fullrows = 0;
		if(mod != 0){
			fullrows = (in.length - mod - 16) >>> 4;
		}
		else fullrows = in.length >>> 4;
		
		int i = 0;
		for(int r = 0; r < fullrows; r++){
			byte[] inrow = Arrays.copyOfRange(in, i, i+16);
			byte[] outrow = encryptBlock(inrow);
			for(int j = 0; j < 16; j++) out[i+j] = outrow[j];
			i+=16;
		}
		
		if(mod != 0){
			//Last two rows
			byte[] inrow = Arrays.copyOfRange(in, i, i+16);
			byte[] outrow1 = encryptBlock(inrow);
			inrow = Arrays.copyOfRange(in, i+16, i+16+mod);
			byte[] outrow2 = encryptBlock(inrow);
			for(int j = 0; j < 16; j++) out[i+j] = outrow2[j];
			i+=16;
			
			for(int j = 0; j < mod; j++) out[i+j] = outrow1[j];
		}
		
		return out;
	}
	
	/* ----- Decrypt ----- */
	
	public boolean initDecrypt(byte[] t){
		resetTweak(t);
		lastblock = null;
		//steal_flag = false;
		return true;
	}
	
	public byte[] decryptBlock(byte[] in, boolean finalBlock){
		//This one doesn't deal with cipher text stealing.
		//Must handle beforehand...
		
		//Decrypt
		byte[] temp = xorArr_Barr(in, tweak);
		temp = rijndael_dec(temp);
		temp = xorArr_Barr(temp, tweak);
		
		//Advance state
		lastblock = Arrays.copyOf(temp, 16);
		advanceTweak();
		
		return temp;
	}
	
	public byte[] decrypt(byte[] t, byte[] in){
		initEncrypt(t);
		if(in.length < 16) throw new UnsupportedOperationException("Cannot decrypt block smaller thatn 16 bytes");
		
		int mod = in.length % 16;
		byte[] out = new byte[in.length];
		
		int fullrows = 0;
		if(mod != 0){
			fullrows = (in.length - mod - 16) >>> 4;
		}
		else fullrows = in.length >>> 4;
		
		int i = 0;
		for(int r = 0; r < fullrows; r++){
			byte[] inrow = Arrays.copyOfRange(in, i, i+16);
			byte[] outrow = decryptBlock(inrow, false);
			for(int j = 0; j < 16; j++) out[i+j] = outrow[j];
			i+=16;
		}
		
		if(mod != 0){
			//Last two rows
			byte[] inrow = Arrays.copyOfRange(in, i, i+16);
			byte[] outrow1 = decryptBlock(inrow, false);
			
			//First part of last
			inrow = new byte[16];
			for(int j = 0; j < mod; j++) inrow[j] = in[i+16+j];
			//Second part of last...
			for(int j = mod; j < 16; j++) inrow[j] = outrow1[j];
			byte[] outrow2 = decryptBlock(inrow, false);
			
			for(int j = 0; j < 16; j++) out[i+j] = outrow2[j];
			i += 16;
			for(int j = 0; j < mod; j++) out[i+j] = outrow1[j];
			
		}
		
		return out;
	}
	
}
