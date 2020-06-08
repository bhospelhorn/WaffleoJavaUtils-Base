package waffleoRai_Image;

import java.awt.image.BufferedImage;

public interface Palette {

	public int getRGBA(int index);
	public int getRed(int index);
	public int getGreen(int index);
	public int getBlue(int index);
	
	public Pixel getPixel(int index);
	public void setPixel(Pixel p, int index);
	public int getBitDepth();
	
	public int getClosestValue(int RGBA);
	
	public void printMe();
	
	public BufferedImage renderVisual();
	
	public static Palette get4BitGreyscalePalette(){
		
		Palette p = new FourBitPalette();
		for(int i = 0; i < 16; i++){
			int val8 = i << 4;
			p.setPixel(new Pixel_RGBA(val8, val8, val8, 255), i);
		}
		
		return p;
	}
	
	public static Palette get8BitGreyscalePalette(){
		Palette p = new EightBitPalette();
		for(int i = 0; i < 256; i++){
			p.setPixel(new Pixel_RGBA(i, i, i, 255), i);
		}
		
		return p;
	}
	
}
