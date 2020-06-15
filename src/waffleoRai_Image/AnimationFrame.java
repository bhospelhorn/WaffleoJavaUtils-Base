package waffleoRai_Image;

import java.awt.image.BufferedImage;

public class AnimationFrame {

	private BufferedImage image;
	private int length;
	
	public AnimationFrame(BufferedImage img, int millis){
		image = img;
		length = millis;
	}
	
	public BufferedImage getImage(){return image;}
	public int getLengthInMillis(){return length;}
	
	public void setImage(BufferedImage img){image = img;}
	public void setLength(int millis){length = millis;}
	
}
