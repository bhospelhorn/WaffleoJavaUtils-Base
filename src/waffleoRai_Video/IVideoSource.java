package waffleoRai_Video;

import java.io.IOException;

public interface IVideoSource {

	public int getLength(); //In seconds
	public int getFrameCount();
	public int getHeight();
	public int getWidth();
	
	public double getFrameRate();
	public int millisPerFrame();
	public VideoIO.Rational getFrameRateRational();
	
	public VideoFrameStream openStream() throws IOException;
	public VideoFrameStream openStreamAt(int min, int sec, int frame) throws IOException;
	public VideoFrameStream openStreamAt(int frame) throws IOException;
	
	public int getRawDataFormat();
	public int getRawDataColorspace();
	public boolean rawOutputAnalogColor();
	
}
