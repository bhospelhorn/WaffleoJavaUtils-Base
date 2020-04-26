package waffleoRai_Sound;

import java.io.IOException;

public interface PCMSound extends RandomAccessSound{
	
	public void normalizeAmplitude();
	public void setLoopStart(int pos);
	public void setLoopEnd(int pos);
	public void setLoopPoints(int stpos, int edpos);
	public void clearLoop();
	
	public void writeWAV(String path) throws IOException;
	public void writeTxt(String path) throws IOException;

}
