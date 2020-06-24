package waffleoRai_Image;

import java.awt.image.BufferedImage;

public interface Animation {
	
	public static final int ANIM_MODE_NORMAL = 0;
	public static final int ANIM_MODE_PINGPONG = 1;

	public int getNumberFrames();
	public BufferedImage getFrameImage(int index);
	public void setNumberFrames(int newNFrames);
	public void setFrame(BufferedImage frame, int index);
	public Animation scale(double factor);
	
	public AnimationFrame getFrame(int index);
	public void setFrame(AnimationFrame frame, int index);
	public void setAnimationMode(int mode);
	public int getAnimationMode();
	
}
