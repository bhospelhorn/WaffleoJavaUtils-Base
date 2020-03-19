package waffleoRai_SoundSynth.general;


import waffleoRai_SoundSynth.FunctionWindow;

public class BlackmanWindow implements FunctionWindow{
	
	private double Nm1; //N-1
	//private Map<Double, Double> precalculated;
	
	public BlackmanWindow(int N)
	{
		Nm1 = (double)(N-1);
		//precalculated = new TreeMap<Double, Double>();
	}
	
	public double getMultiplier(double n)
	{
		//Double check = precalculated.get(n);
		//if(check != null) return check;
		
		double val = 0.42;
		double twopin = 2.0 * Math.PI * n;
		val -= 0.5 * Math.cos(twopin/Nm1);
		val += 0.08 * Math.cos((twopin * 2.0)/Nm1);
		//precalculated.put(n, val);
		return val;
	}
	
	public void flushSavedValues()
	{
		//precalculated.clear();
	}

}
