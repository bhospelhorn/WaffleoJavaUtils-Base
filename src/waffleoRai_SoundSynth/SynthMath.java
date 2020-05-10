package waffleoRai_SoundSynth;

public class SynthMath {
	
	private static int SINTABLE_RES = 0x100000;
	private static double SINTABLE_RES_D = (double)SINTABLE_RES;
	
	private static double[] sin_table;
	//private static double[] factor_table;
	
	public static double sinc(double in)
	{
		if(in == 0.0) return 1.0;
		double pix = in * Math.PI;
		
		return Math.sin(pix)/pix;
	}
	
	//Indexes up to 1/4 the period (so up to pi/2)
	private static void buildSinTable(){

		sin_table = new double[SINTABLE_RES+1];
		for(int i = 0; i < SINTABLE_RES+1; i++){
			double frac = (double)i/SINTABLE_RES_D;
			frac /= 4.0;
			double val = frac * 2.0 * Math.PI;
			sin_table[i] = Math.sin(val);
		}
		
	}
	
	/**
	 * Get the sine of the input from a precalculated table.
	 * <br><b>IMPORTANT:</b> Input must be in sine cycles, NOT radians!
	 * The purpose of this is for simple indexing and to reduce calculation
	 * time!
	 * @param in Input value in sine cycles ie. 1.0 is the end of one period, or
	 * 2*PI in a plain sine function.
	 * @return The estimated sine value at that cycle point. In other words,
	 * sin(2*PI*in).
	 */
	public static double quicksin(double in){
		if(sin_table == null) buildSinTable();

		//Get just the fraction
		double frac = in - (long)in;
		//Flip if negative
		if (in < 0) frac += 1.0;
		//Derive index
		boolean n = false;
		if(frac > 0.5){
			n = true;
			frac -= 0.5;
		}
		if(frac > 0.25) frac = 0.5 - frac;
		
		int idx = (int)(Math.round((frac*4.0) * SINTABLE_RES_D));
		
		double sin = sin_table[idx];
		if(n) sin *= -1.0;
		return sin;
		
		/*long whole = (long)in;
		double frac = in - whole;
		//int idx = (int)Math.round(frac * (double)0x4000);
		//if(whole % 2 != 0) idx += 0x3FFF;
		int idx = (int)Math.round(frac * (double)0x7FFF);
		double sin = sin_table[idx];
		
		return sin;*/
	}
	
	/**
	 * Get the cosine of the input from a precalculated table.
	 * <br><b>IMPORTANT:</b> Input must be in sine cycles, NOT radians!
	 * The purpose of this is for simple indexing and to reduce calculation
	 * time!
	 * @param in Input value in sine cycles ie. 1.0 is the end of one period, or
	 * 2*PI in a plain sine function.
	 * @return The estimated sine value at that cycle point. In other words,
	 * sin(2*PI*in).
	 */
	public static double quickcos(double in){
		return quicksin(in+0.25);
	}
	
	public static double quicksinc(double in){
		if(in == 0.0) return 1.0;
		if(sin_table == null) buildSinTable();
		
		in = Math.abs(in);
		if(in <= 0.000000001) return 1.0;
		
		//long whole = (long)in;
		//double frac = in - whole;
		/*int idx = (int)Math.round(frac * (double)0x4000);
		if(whole % 2 != 0) idx += 0x3FFF;
		double sin = sin_table[idx];
	
		return sin/(Math.PI * in);*/
		
		double val = in/2.0;
		double sin = quicksin(val);
		
		return sin/(Math.PI * in);
	}
	
	public static double cents2FreqRatio(int cents)
	{
		double exp = ((double)cents * -1.0) / 1200.0;
		return Math.pow(2.0, exp);
	}

	public static double quickTriangle(double in){

		double frac = (long)in - in;
		if(frac <= 0.25){
			return frac * 4.0;
		}
		else if(frac <= 0.75){
			return (frac * -4.0) + 2.0;
		}
		else{
			return (frac * 4.0) - 4.0;
		}
		
	}
	
	public static double quickSaw(double in){
		double frac = (long)in - in;
		if(frac <= 0.5){
			return frac * 2.0;
		}
		else{
			return (frac * 2.0) - 2.0;
		}
	}
	
	public static double quickSquare(double in){
		double frac = (long)in - in;
		if(frac <= 0.5) return 1.0;
		else return -1.0;
	}

	public static void main(String[] args){
		//TEST
		//-2 cycles to 2 cycles (-4PI to 4PI)
		//Every 0.01
		
		/*System.out.println("x(Cycles)\tx(Radians)\tMath.sin\tquicksin");
		for(double v = -2.0; v <= 2.0; v+=0.001){
			double rads = v*(2.0*Math.PI);
			double sin = Math.sin(rads);
			double qsin = quicksin(v);
			System.out.println(v + "\t" + rads + "\t" + sin + "\t" + qsin);
		}*/
		
		/*System.out.println("x(Cycles)\tsinc\tquicksinc");
		for(double v = -5.0; v <= 5.0; v+=0.01){
			double sinc = sinc(v);
			double qsinc = quicksinc(v);
			System.out.println(v + "\t" + sinc + "\t" + qsinc);
		}*/
		
		System.out.println("x(Cycles)\tx(Radians)\tMath.cos\tquickcos");
		for(double v = -2.0; v <= 2.0; v+=0.001){
			double rads = v*(2.0*Math.PI);
			double sin = Math.cos(rads);
			double qsin = quickcos(v);
			System.out.println(v + "\t" + rads + "\t" + sin + "\t" + qsin);
		}
		
	}
	
}
