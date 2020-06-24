package waffleoRai_Image;

import java.awt.Image;
import java.awt.image.BufferedImage;

public class SimpleAnimation implements Animation{
	
	//public static final int DEFO_MILLIS = 33;
	
	private int mode;
	private AnimationFrame[] frames;
	
	public SimpleAnimation(int frameCount){
		frames = new AnimationFrame[frameCount];
	}

	public int getNumberFrames() {
		if(frames == null) return 0;
		return frames.length;
	}

	public BufferedImage getFrameImage(int index) {
		AnimationFrame f = getFrame(index);
		if(f == null) return null;
		return f.getImage();
	}

	public void setNumberFrames(int newNFrames) {
		if(newNFrames < 1) return;
		
		AnimationFrame[] old = frames;
		frames = new AnimationFrame[newNFrames];
		
		for(int i = 0; i < old.length; i++){
			if(i < frames.length) frames[i] = old[i];
		}
	}

	public void setFrame(BufferedImage frame, int index) {
		if(index < 0 || frames == null || index >= frames.length) throw new IndexOutOfBoundsException("Index " + index + " invalid.");
		frames[index] = new AnimationFrame(frame, 1);
	}

	public Animation scale(double factor) {
		if(frames == null) return null;
		SimpleAnimation scaled = new SimpleAnimation(frames.length);
		for(int i = 0; i < frames.length; i++){
			if(frames[i] == null) continue;
			BufferedImage img = frames[i].getImage();
			int w = (int)Math.round((double)img.getWidth() * factor);
			int h = (int)Math.round((double)img.getHeight() * factor);
			Image scimg = img.getScaledInstance(w, h, Image.SCALE_DEFAULT);
			BufferedImage img2 = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
			img2.getGraphics().drawImage(scimg, 0, 0, null);
			scaled.frames[i] = new AnimationFrame(img2, frames[i].getLengthInFrames());
		}
		
		return scaled;
	}

	public AnimationFrame getFrame(int index) {
		if(index < 0 || frames == null || index >= frames.length) throw new IndexOutOfBoundsException("Index " + index + " invalid.");
		return frames[index];
	}

	public void setFrame(AnimationFrame frame, int index) {
		if(index < 0 || frames == null || index >= frames.length) throw new IndexOutOfBoundsException("Index " + index + " invalid.");
		frames[index] = frame;
	}
	
	public void setAnimationMode(int mode){
		this.mode = mode;
	}

	public int getAnimationMode(){return mode;}
	
}
