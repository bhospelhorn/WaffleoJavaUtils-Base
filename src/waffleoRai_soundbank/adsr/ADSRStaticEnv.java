package waffleoRai_soundbank.adsr;

public class ADSRStaticEnv implements EnvelopeStreamer{

	private double level;
	
	public ADSRStaticEnv(double lvl){
		level = lvl;
	}
	
	@Override
	public double getNextAmpRatio() {
		return level;
	}

	@Override
	public boolean done() {
		return false;
	}

}
