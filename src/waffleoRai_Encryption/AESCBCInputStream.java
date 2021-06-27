package waffleoRai_Encryption;

import java.io.IOException;
import java.io.InputStream;

public class AESCBCInputStream extends InputStream{

	private AES aes;
	private byte[] buffer;
	private int buff_pos;
	
	private InputStream src;
	private boolean eof;
	
	public AESCBCInputStream(InputStream source, byte[] aes_key, byte[] aes_iv) throws IOException{
		aes = new AES(aes_key);
		concore(source, aes_iv);
	}
	
	public AESCBCInputStream(InputStream source, int[] aes_key, byte[] aes_iv) throws IOException{
		aes = new AES(aes_key);
		concore(source, aes_iv);
	}
	
	private void concore(InputStream source, byte[] aes_iv) throws IOException{
		buffer = new byte[16];
		src = source;
		
		aes.setCBC();
		aes.initDecrypt(aes_iv);
		
		eof = false;
		fillBuffer();
	}
	
	private void fillBuffer() throws IOException{
		for(int i = 0; i < 16; i++){
			int r = src.read();
			if(r >= 0) buffer[i] = (byte)r;
			else{
				buffer[i] = 0;
				eof = true;
			}
		}
		buffer = aes.decryptBlock(buffer, false);
		buff_pos = 0;
	}
	
	public int read() throws IOException {
		//System.err.println("AESCBCInputStream.read() || Called");
		if(buff_pos >= 16){
			if(eof) return -1;
			fillBuffer();
		}
		return Byte.toUnsignedInt(buffer[buff_pos++]);
	}
	
	public void close() throws IOException{
		super.close();
		aes.decryptBlock(new byte[16], true);
		src.close();
	}

}
