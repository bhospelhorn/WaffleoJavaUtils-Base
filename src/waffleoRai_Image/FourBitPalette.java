package waffleoRai_Image;

import java.awt.image.BufferedImage;

public class FourBitPalette implements Palette{

	private Pixel[] values;
	
	public FourBitPalette()
	{
		this.values = new Pixel[16];
		for (int i = 0; i < 16; i++)
		{
			this.values[i] = new Pixel_RGBA(0, 0, 0, 255);
		}
	}
	
	public int getRGBA(int index)
	{
		if (index < 0 || index >= 16) throw new ArrayIndexOutOfBoundsException();
		return this.values[index].getRGBA();
	}
	
	public int getRed(int index)
	{
		if (index < 0 || index >= 16) throw new ArrayIndexOutOfBoundsException();
		return this.values[index].getRed();
	}
	
	public int getGreen(int index)
	{
		if (index < 0 || index >= 16) throw new ArrayIndexOutOfBoundsException();
		return this.values[index].getGreen();
	}
	
	public int getBlue(int index)
	{
		if (index < 0 || index >= 16) throw new ArrayIndexOutOfBoundsException();
		return this.values[index].getBlue();
	}
	
	public Pixel getPixel(int index)
	{
		if (index < 0 || index >= 16) throw new ArrayIndexOutOfBoundsException();
		return this.values[index];
	}
	
	public void setPixel(Pixel p, int index)
	{
		if (index < 0 || index >= 16) throw new ArrayIndexOutOfBoundsException();
		this.values[index] = p;
	}
	
	public int getBitDepth()
	{
		return 4;
	}
	
	public int getClosestValue(int RGBA)
	{
		int best = -1;
		int threshold = 0x7FFFFFFF;
		for (int i = 0; i < 16; i++)
		{
			int dist = values[i].getColorDistance(RGBA);
			if (dist < threshold)
			{
				threshold = dist;
				best = i;
			}
		}
		
		return best;
	}
	
	public void printMe()
	{
		System.out.println("Palette - FourBitPalette");
		System.out.println("-------------------------");
		System.out.println("HEX\tRED\tGREEN\tBLUE\tALPHA");
		System.out.println("---\t---\t-----\t----\t-----");
		for (int i = 0; i < values.length; i++)
		{
			System.out.print(String.format("0x%02x\t", i));
			Pixel p = values[i];
			System.out.print(String.format("0x%02x\t", p.getRed()));
			System.out.print(String.format("0x%02x\t", p.getGreen()));
			System.out.print(String.format("0x%02x\t", p.getBlue()));
			System.out.print(String.format("0x%02x", p.getAlpha()));
			System.out.println();
		}
	}
	
	public BufferedImage renderVisual(){
		
		final int SAMPLE_WIDTH = 20;
		final int IMG_DIM = SAMPLE_WIDTH << 2;
		
		BufferedImage img = new BufferedImage(IMG_DIM, IMG_DIM, BufferedImage.TYPE_INT_ARGB);
		int x = 0; int y = 0;
		
		for(int i = 0; i < 16; i++){
			//System.err.println("Writing color " + i);
			Pixel p = values[i];
			int argb = p.getARGB();
			
			for(int r = 0; r < SAMPLE_WIDTH; r++){
				for(int l = 0; l < SAMPLE_WIDTH; l++){
					img.setRGB(x+l, y+r, argb);
				}
			}
			
			x+= SAMPLE_WIDTH;
			if(x >= IMG_DIM){
				x = 0; y += SAMPLE_WIDTH;
			}
			
		}
		
		return img;
	}

}
