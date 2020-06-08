package waffleoRai_SoundSynth.general;

public class OscTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int sr = 22050;
		int del = 0;
		int or = 20;
		
		BasicLFO lfo = new SineLFO(sr);
		lfo.setDelay(del);
		lfo.setLFORate(or);
		
		for(int i = -11025; i < 11025; i++)System.err.println(i + "\t" + lfo.getNextValue());
		
	}

}
