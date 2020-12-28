package waffleoRai_Video;

import java.awt.image.BufferedImage;

public interface VideoFrameStream {

	public double getFrameRate();
	public int getFrameWidth();
	public int getFrameHeight();
	
	public int getFrameCount();	
	public BufferedImage getNextFrame();
	public boolean done();
	
	public boolean rewindEnabled();
	public boolean rewind();
	
	public void close();
	
}
