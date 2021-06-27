package waffleoRai_Encryption;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;

import waffleoRai_Utils.FileBuffer;

public class EncryptStuff {

	public static void main(String[] args) {
		
		//Key gen
		Random rand = new Random();
		System.out.println("Ur random number: " + String.format("%016x", rand.nextLong()));
		
		String dir = "C:\\Users\\Blythe\\Documents\\Game Stuff\\logos\\ntd_placeholders";
		String[] files = {"agb_small.png", "ctr_small.png", "ctr_small_locked.png",
				"dol_small.png", "hac_small.png", "hac_small_locked.png", "ntr_small.png",
				"nus_small.png", "psx_small.png", "rvl_small.png", "rvl_small_locked.png",
				"wup_small.png", "wup_small_locked.png"};
		byte[] iv = {(byte)'N', (byte)'T', (byte)'D', (byte)'e',
					 (byte)'x', (byte)'p', (byte)'l', (byte)'o',
					 (byte)'r', (byte)'e', (byte)'r', (byte)' ',
					 (byte)'2', (byte)'0', (byte)'2', (byte)'0',};
		int[] key = {0x97, 0xbc, 0x9f, 0xa5,
					 0x56, 0xd7, 0xce, 0x76,
					 0x07, 0x18, 0x5e, 0xab,
					 0x69, 0x1e, 0x3f, 0xa4};
		
		try{
			
			for(String filename : files){
				System.out.println("Now doing " + filename);
				String inpath = dir + "\\" + filename;
				String outpath = inpath + ".aes";
				
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outpath));
				
				AES aes = new AES(key);
				aes.initEncrypt(iv);
				FileBuffer data = FileBuffer.createBuffer(inpath);
				long remaining = data.getFileSize();
				long pos = 0;
				while(remaining > 16){
					bos.write(aes.encryptBlock(data.getBytes(pos, pos+16), false));
					pos += 16; remaining -=16;
				}
				byte[] buffer = new byte[16];
				int i = 0;
				while(remaining > 0){
					buffer[i] = data.getByte(pos++);
					remaining--;
				}
				bos.write(aes.encryptBlock(buffer, true));
				bos.close();
				
				
				//Try to read it back to make sure
				String checkpath = inpath + "check.png";
				AESCBCInputStream checkstr = new AESCBCInputStream(
						new BufferedInputStream(new FileInputStream(outpath)), key, iv);
				bos = new BufferedOutputStream(new FileOutputStream(checkpath));
				
				int b = checkstr.read();
				while(b >= 0){
					bos.write(b);
					b = checkstr.read();
				}
				
				bos.close();
				checkstr.close();
			}
			
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(1);
		}
		
	}

}
