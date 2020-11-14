package waffleoRai_GUITools;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class RasterImageDrawer extends AnimatedImagePaneDrawer{

	private int x_pos;
	private int y_pos;
	
	private BufferedImage img;
	
	public RasterImageDrawer(BufferedImage image){
		img = image;
	}
	
	public int getX() {return x_pos;}
	public int getY() {return y_pos;}

	public int getWidth() {
		if(img == null) return 0;
		return img.getWidth();
	}

	public int getHeight() {
		if(img == null) return 0;
		return img.getHeight();
	}

	public void drawMe(Graphics g, int x, int y) {
		if(img == null) return;
		g.drawImage(img, x + x_pos, y + y_pos, null);
	}

	public void setPosition(int x, int y) {
		x_pos = x; y_pos = y;
	}

	public AnimatedImagePaneDrawer getCopy() {
		RasterImageDrawer copy = new RasterImageDrawer(img);
		copy.x_pos = this.x_pos;
		copy.y_pos = this.y_pos;
		copy.setStartTime(this.getStartTime());
		copy.setLength(this.getLengthInMillis());
		
		return copy;
	}

}
