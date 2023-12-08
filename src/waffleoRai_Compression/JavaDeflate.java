package waffleoRai_Compression;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import waffleoRai_Compression.definitions.AbstractCompDef;
import waffleoRai_Files.FileBufferInputStream;
import waffleoRai_Files.FileBufferOutputStream;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBufferStreamer;
import waffleoRai_Utils.FileInputStreamer;
import waffleoRai_Utils.StreamWrapper;

public class JavaDeflate {
	
	public static FileBuffer inflate(StreamWrapper input){
		if(input == null) return null;
		if(input instanceof FileInputStreamer){
			return inflate(((FileInputStreamer) input).getStream());
		}
		else if(input instanceof FileBufferStreamer){
			return inflate(((FileBufferStreamer) input).getData());
		}
		
		return null;
	}

	public static FileBuffer inflate(InputStream input){
		if(input == null) return null;
		try{
			//Read first byte
			int decsize = input.read();
			if((decsize & 0x80) != 0){
				decsize &= ~0x80;
				long decsizel = Integer.toUnsignedLong(decsize);
				for(int i = 1; i < 8; i++){
					decsizel <<= 8;
					decsizel |= input.read();
				}
				if(decsizel > 0x7fffffff) return null;
				decsize = (int)decsizel;
			}
			else{
				for(int i = 1; i < 4; i++){
					decsize <<= 8;
					decsize |= input.read();
				}
			}
			
			FileBuffer dec = new FileBuffer(decsize, true);
			
			InflaterInputStream iis = new InflaterInputStream(input);
			for(int i = 0; i < decsize; i++){
				dec.addToFile((byte)iis.read());
			}
			iis.close();
			return dec;
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		return null;
	}
	
	public static FileBuffer inflate(FileBuffer input){
		if(input == null) return null;
		input.setCurrentPosition(0L);
		int decsize = input.nextInt();
		if((decsize & 0x80000000) != 0){
			input.setCurrentPosition(0L);
			long decsizel = input.nextLong() & ~(1L<<63);
			if(decsizel > 0x7fffffff) return null;
			decsize = (int)decsizel;
		}
		
		FileBuffer dec = new FileBuffer(decsize, true);
		try{
			InflaterInputStream iis = new InflaterInputStream(new FileBufferInputStream(input));
			input.setCurrentPosition(4L);
			for(int i = 0; i < decsize; i++){
				dec.addToFile((byte)iis.read());
			}
			iis.close();
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		return dec;
	}
	
	public static boolean inflateTo(StreamWrapper input, String outpath){
		if(input == null) return false;
		if(input instanceof FileInputStreamer){
			return inflateTo(((FileInputStreamer) input).getStream(), outpath);
		}
		else if(input instanceof FileBufferStreamer){
			return inflateTo(((FileBufferStreamer) input).getData(), outpath);
		}
		
		return false;
	}
	
	public static boolean inflateTo(InputStream input, String outpath){
		if(input == null) return false;
		try{
			long decsize = input.read();
			if((decsize & 0x80) != 0){
				decsize &= ~0x80;
				for(int i = 1; i < 8; i++){
					decsize <<= 8;
					decsize |= input.read();
				}
			}
			else{
				for(int i = 1; i < 4; i++){
					decsize <<= 8;
					decsize |= input.read();
				}
			}
			
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outpath));
			
			InflaterInputStream iis = new InflaterInputStream(input);
			for(long i = 0; i < decsize; i++){
				bos.write(iis.read());
			}
			iis.close();
			bos.close();
			
			return true;
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		return false;
	}
	
	public static boolean inflateTo(FileBuffer input, String outpath){
		if(input == null) return false;
		input.setCurrentPosition(0L);
		long decsize = input.nextInt();
		if((decsize & 0x80000000) != 0){
			input.setCurrentPosition(0L);
			decsize = input.nextLong() & ~(1L<<63);
		}
		
		try{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outpath));
			
			InflaterInputStream iis = new InflaterInputStream(new FileBufferInputStream(input));
			input.setCurrentPosition(4L);
			for(int i = 0; i < decsize; i++){
				bos.write(iis.read());
			}
			iis.close();
			bos.close();
			
			return true;
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		return false;
	}
	
	public static FileBuffer deflate(FileBuffer input){
		if(input == null) return null;
		int size = (int)input.getFileSize();
		FileBuffer comp = new FileBuffer(size + 4, true);
		comp.addToFile(size);
		
		try{
			DeflaterOutputStream dos = new DeflaterOutputStream(new FileBufferOutputStream(comp));
			dos.write(input.getBytes(), 0, (int)input.getFileSize());
			dos.close();
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		
		return comp;
	}
	
	/*----- Compression Definition -----*/
	
	public static final int TYPE_ID = 0xdef1a2e0;
	public static final String DEFO_NAME_ENG = "Java Deflate";
	
	private static JavaDeflateDef stat_def;
	
	public static class JavaDeflateDef extends AbstractCompDef{
		
		protected JavaDeflateDef() {
			super(DEFO_NAME_ENG);
			super.extensions.add("jdfl");
		}
		
		public int getDefinitionID() {return TYPE_ID;}

		public boolean decompressToDiskBuffer(StreamWrapper input, String bufferPath, int options) {
			return inflateTo(input, bufferPath);
		}

		public boolean decompressToDiskBuffer(InputStream input, String bufferPath, int options) {
			return inflateTo(input, bufferPath);
		}

		public boolean decompressToDiskBuffer(BufferReference input, String bufferPath, int options) {
			return inflateTo(input.getBuffer(), bufferPath);
		}

		public FileBuffer decompressToMemory(StreamWrapper input, int allocAmount, int options) {
			return inflate(input);
		}

		public FileBuffer decompressToMemory(InputStream input, int allocAmount, int options) {
			return inflate(input);
		}

		public FileBuffer decompressToMemory(BufferReference input, int allocAmount, int options) {
			return inflate(input.getBuffer());
		}

	}
	
	public static JavaDeflateDef getDefinition(){
		if(stat_def == null) stat_def = new JavaDeflateDef();
		return stat_def;
	}
	
}
