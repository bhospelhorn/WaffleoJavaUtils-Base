package waffleoRai_Files;

import java.io.IOException;
import java.io.OutputStream;

import waffleoRai_Utils.FileBuffer;

public class FileBufferOutputStream extends OutputStream{

	private FileBuffer target = null;
	
	public FileBufferOutputStream(FileBuffer trg){
		target = trg;
		
	}

	public void write(int b) throws IOException {
		if(b == -1) return;
		target.addToFile((byte)b);
	}
	
	public void write(byte[] b) throws IOException {
		if(b == null) return;
		for(int i = 0; i < b.length; i++){
			target.addToFile(b[i]);
		}
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		if(b == null) return;
		int min = Math.max(0, off);
		int max = Math.min(off+len, b.length);
		for(int i = min; i < max; i++){
			target.addToFile(b[i]);
		}
	}
	
}
