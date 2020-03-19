package waffleoRai_soundbank.adsr;

public interface EnvelopeStreamer {

	//public int getNextLevel();
	public double getNextAmpRatio();
	public boolean done();
	
}
