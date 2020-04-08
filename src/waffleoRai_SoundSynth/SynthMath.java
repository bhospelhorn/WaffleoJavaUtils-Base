package waffleoRai_SoundSynth;

public class SynthMath {
	
	private static double[] sin_table;
	//private static double[] factor_table;
	
	public static double sinc(double in)
	{
		if(in == 0.0) return 1.0;
		double pix = in * Math.PI;
		
		return Math.sin(pix)/pix;
	}
	
	private static void buildSinTable(){
		sin_table = new double[0x8000];
		
		for(int i = 0; i < 0x8000; i++){
			double frac = (double)i / (double)0x7FFF;
			double val = frac * 2.0 * Math.PI;
			sin_table[i] = Math.sin(val);
		}
	}
	
	public static double quicksinc(double in){
		if(in == 0.0) return 1.0;
		if(sin_table == null) buildSinTable();
		
		in = Math.abs(in);
		long whole = (long)in;
		double frac = in - whole;
		int idx = (int)Math.round(frac * (double)0x4000);
		if(whole % 2 != 0) idx += 0x3FFF;
		double sin = sin_table[idx];
		
		return sin/(Math.PI * in);
	}
	
	public static double cents2FreqRatio(int cents)
	{
		double exp = ((double)cents * -1.0) / 1200.0;
		return Math.pow(2.0, exp);
	}


}
