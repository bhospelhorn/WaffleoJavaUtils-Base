package waffleoRai_SoundSynth.general;


import waffleoRai_SoundSynth.FunctionWindow;

public class BlackmanWindow implements FunctionWindow{
	
	private static double[] coeff_tbl;
	
	private double Nm1; //N-1
	
	public BlackmanWindow(int N)
	{
		Nm1 = (double)(N-1);
		if(coeff_tbl == null) buildCoeffTable();
	}
	
	private static void buildCoeffTable(){
		coeff_tbl = new double[65536];
		for(int i = 0; i < 65536; i++){
			
			double frac = ((double)i/65535.0) * 2.0 * Math.PI;
			double val = 0.5 * Math.cos(frac);
			val += 0.08 * Math.cos(2.0 * frac);
			val = 0.42 - val;
			coeff_tbl[i] = val;
			//System.err.println("blackman[" + i + "] = " + coeff_tbl[i]);
		}
	}
	
	public double getMultiplier(double n)
	{
		//Double check = precalculated.get(n);
		//if(check != null) return check;
		
		/*double val = 0.42;
		double twopin = 2.0 * Math.PI * n;
		val -= 0.5 * Math.cos(twopin/Nm1);
		val += 0.08 * Math.cos((twopin * 2.0)/Nm1);
		return val;*/
		//precalculated.put(n, val);
		
		double frac = n/Nm1;
		frac -= (long)frac;
		int idx = Math.abs((int)Math.round(frac * 65535.0));
		return coeff_tbl[idx];
	}

}
