package waffleoRai_Image;

import java.awt.image.BufferedImage;

public class AnimationFrame {

	private BufferedImage image;
	private int length;
	
	public AnimationFrame(BufferedImage img, int length_frames){
		image = img;
		length = length_frames;
	}
	
	public BufferedImage getImage(){return image;}
	public int getLengthInFrames(){return length;}
	
	public void setImage(BufferedImage img){image = img;}
	public void setLength(int millis){length = millis;}
	
}
