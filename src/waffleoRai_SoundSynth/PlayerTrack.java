package waffleoRai_SoundSynth;

public interface PlayerTrack {

	public void onTick(long tick) throws InterruptedException;
	//public void reset();
	public void resetTo(long tick);
	public boolean trackEnd();
	public void setMute(boolean b);
	public boolean isMuted();
	
}
