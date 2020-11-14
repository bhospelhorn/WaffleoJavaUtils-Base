package waffleoRai_Files;

import java.io.IOException;
import java.io.InputStream;

import waffleoRai_Utils.FileBuffer;

public class FileBufferInputStream extends InputStream{

	private FileBuffer src;
	
	public FileBufferInputStream(FileBuffer dat){
		src = dat;
		src.setCurrentPosition(0);
	}
	
	public int read() throws IOException {
		int i = -1;
		if(src.hasRemaining()){
			i = Byte.toUnsignedInt(src.nextByte());
		}
		return i;
	}
	
	public void close() throws IOException{
		if(src != null) src.dispose();
		super.close();
	}

}
