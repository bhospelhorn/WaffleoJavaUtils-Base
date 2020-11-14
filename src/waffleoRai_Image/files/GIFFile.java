package waffleoRai_Image.files;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;

import waffleoRai_Files.FileBufferInputStream;
import waffleoRai_Files.FileClass;
import waffleoRai_Files.tree.FileNode;
import waffleoRai_GUITools.AnimatedImagePaneDrawer;
import waffleoRai_Image.RasterImageDef;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;

//https://stackoverflow.com/questions/8933893/convert-each-animated-gif-frame-to-a-separate-bufferedimage

public class GIFFile {

	/*----- Instance Variables -----*/
	
	private List<GIFFrame> frames;
	
	private int width;
	private int height;
	
	private int anim_length;
	
	/*----- Inner Classes -----*/
	
	public static class GIFFrame extends AnimatedImagePaneDrawer{
		
		//private int delay;
		private BufferedImage image;
		private String dispose_str;
		
		private int x_pos;
		private int y_pos;
		
		public int getDelay(){return super.getLengthInMillis();}
		public BufferedImage getImage(){return image;}
		public String getDisposeString(){return dispose_str;}
		
		public void setDelay(int millis){
			super.setLength(millis);
		}
		
		public int getX() {return x_pos;}
		public int getY() {return y_pos;}

		public int getWidth() {
			if(image == null) return 0;
			return image.getWidth();
		}

		public int getHeight() {
			if(image == null) return 0;
			return image.getHeight();
		}
		
		public void drawMe(Graphics g, int x, int y) {
			if(image == null) return;
			g.drawImage(image, x + x_pos, y + y_pos, null);
		}
		
		public void setPosition(int x, int y) {
			x_pos = x; y_pos = y;
		}
		
		public AnimatedImagePaneDrawer getCopy() {
			GIFFrame copy = new GIFFrame();
			copy.image = this.image;
			copy.dispose_str = this.dispose_str;
			copy.x_pos = this.x_pos;
			copy.y_pos = this.y_pos;
			copy.setStartTime(this.getStartTime());
			copy.setLength(this.getLengthInMillis());
			
			return copy;
		}
		
	}
	
	/*----- Construction/Parsing -----*/
	
	private GIFFile(){
		this(16);
	}
	
	private GIFFile(int alloc){
		frames = new ArrayList<GIFFrame>(alloc);
		width = -1;
		height = -1;
		anim_length = -1;
	}
	
	public static GIFFile readGIF(FileBuffer data) throws UnsupportedFileTypeException, IOException{
		InputStream is = new FileBufferInputStream(data);
		return readGIF(is);
	}
	
	public static GIFFile readGIF(InputStream data) throws UnsupportedFileTypeException, IOException{
		//Load image reader
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
		if(readers == null || !readers.hasNext()) throw new UnsupportedFileTypeException("Java GIF reader not found!");
		ImageReader reader = readers.next();
		
		GIFFile gif = new GIFFile();
		
	    BufferedImage img = null;
	    int fidx = 0;
	    int time = 0;
	    while(true){
	    	try{img = reader.read(fidx++);}
	    	catch(IndexOutOfBoundsException x){break;} //No way to check img count beforehand	
	    	
	    	//Verbatim from stackoverflow
	    	IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(fidx).getAsTree("javax_imageio_gif_image_1.0");
	        IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
	        int delay = Integer.valueOf(gce.getAttribute("delayTime"));
	        String disposal = gce.getAttribute("disposalMethod");
	    	
	        //Frame
	        GIFFrame frame = new GIFFrame();
	        frame.image = img;
	        frame.setDelay(delay*10);
	        frame.dispose_str = disposal;
	        frame.setStartTime(time);
	        time += frame.getDelay();
	        
	        gif.frames.add(frame);
	    }
		
	    //There's also global position and size for the whole file
	    //But I'm skipping those cuz eh
	    
		reader.dispose();
		return gif;
	}
	
	/*----- Getters -----*/
	
	private void determineSize(){
		if(frames.isEmpty()){
			width = -1; height = -1;
			return;
		}
		
		for(GIFFrame f : frames){
			if(f.image.getWidth() > width) width = f.image.getWidth();
			if(f.image.getHeight() > height) height = f.image.getHeight();
		}
	}
	
	public int getFrameCount(){
		return frames.size();
	}
	
	public int getWidth(){
		if(width == -1) determineSize();
		return width;
	}
	
	public int getHeight(){
		if(height == -1) determineSize();
		return height;
	}
	
	public int getAnimationLength(){
		if(anim_length >= 0) return anim_length;
		
		//Calculate
		int len = 0;
		for(GIFFrame f : frames){
			len += f.getDelay();
		}
		
		return len;
	}
	
	public GIFFrame getFrame(int idx){
		if(frames.isEmpty()) return null;
		if(idx < 0) return null;
		if(idx >= frames.size()) return null;
		return frames.get(idx);
	}
	
	public List<GIFFrame> getFrames(){
		List<GIFFrame> list = new LinkedList<GIFFrame>();
		list.addAll(frames);
		return list;
	}
	
	/*----- Setters -----*/
	
	/*----- GUITools Interface -----*/
	
	/*----- Definition -----*/
	
	public static final int DEF_ID = 0x47494666;
	public static final String DEFO_ENG_STR = "Graphics Interchange Format (GIF) Image";
	
	private static GIFDefinition stat_def;
	
	public static class GIFDefinition extends RasterImageDef{

		private String desc = DEFO_ENG_STR;
		
		public Collection<String> getExtensions() {
			List<String> list = new ArrayList<String>(1);
			list.add("gif");
			return list;
		}

		public String getDescription() {return desc;}
		public FileClass getFileClass() {return FileClass.IMG_ANIM_2D;} 
		//Could be IMG as well, but nowadays GIFs are mostly used for short animations

		public int getTypeID() {return DEF_ID;}
		public void setDescriptionString(String s) {desc = s;}
		public String getDefaultExtension() {return "gif";}

		public BufferedImage renderImage(FileNode src) {
			try{
				FileBuffer dat = src.loadDecompressedData();
				GIFFile gif = GIFFile.readGIF(dat);
				if(gif.frames.isEmpty()) return null;
				
				//Just return the first frame
				return gif.frames.get(0).image;
			}
			catch(Exception x){
				x.printStackTrace();
				return null;
			}
		}
		
	}
	
	public static GIFDefinition getDefinition(){
		if(stat_def == null) stat_def = new GIFDefinition();
		return stat_def;
	}
	
}
