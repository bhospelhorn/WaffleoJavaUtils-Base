package waffleoRai_SoundSynth;

public class PanUtils {
	
	public static double[] getLRAmpRatios_Mono2Stereo(byte pan)
	{
		double[] ratios = new double[2];
		if(pan == 0x40)
		{
			ratios[0] = 1.0;
			ratios[1] = 1.0;
		}
		else if(pan < 0x40)
		{
			//Left
			if(pan == 0)
			{
				//Right channel would be asymptotic. Set to 0.0
				ratios[0] = 2.0;
				ratios[1] = 0.0;
			}
			else
			{
				double amt = (double)(0x40-pan)/(double)0x40;	
				ratios[0] = calcAmpRatio_SameChannel_Mono2Stereo(amt);
				ratios[1] = calcAmpRatio_OppChannel_Mono2Stereo(amt);
			}
		}
		else if(pan > 0x40)
		{
			//Right
			if(pan == 0x7F)
			{
				//Left channel would be asymptotic. Set to 0.0
				ratios[0] = 0.0;
				ratios[1] = 2.0;
			}
			else
			{
				double amt = (double)(0x7F-pan)/(double)0x40;	
				ratios[1] = calcAmpRatio_SameChannel_Mono2Stereo(amt);
				ratios[0] = calcAmpRatio_OppChannel_Mono2Stereo(amt);
			}
		}
		
		return ratios;
	}
	
	private static double calcAmpRatio_SameChannel_Mono2Stereo(double amt)
	{
		//y = x + 1.0
		return amt + 1.0;
	}
	
	private static double calcAmpRatio_OppChannel_Mono2Stereo(double amt)
	{
		//y = -1/(x-1)
		return -1.0 / (amt - 1.0);
	}

	public static double[][] getLRAmpRatios_Stereo2Stereo(byte pan)
	{
		//out[0][0] L->L
		//out[0][1] L->R
		//out[1][0] R->L
		//out[1][1] R->R
		double[][] out = new double[2][2];
		
		//(eg. if pan is L, then C is L and T is R)
		//Cis channel
		/* C->C || Always 1.0
		 * C->T || Always 0.0
		 * */

		//Trans channel
		/* T->C || y = x
		 * T->T || y = 1-x
		 */
		
		if(pan == 0x40)
		{
			//Center
			out[0][0] = 1.0; out[0][1] = 0.0;
			out[1][0] = 0.0; out[1][1] = 1.0;
		}
		else if(pan < 0x40)
		{
			//Left
			out[0][0] = 1.0; out[0][1] = 0.0;
			if(pan == 0)
			{
				//Send all left
				out[1][0] = 1.0; out[1][1] = 0.0;
			}
			else
			{
				double amt = (double)(0x40-pan)/(double)0x40;	
				out[1][0] = amt; 
				out[1][1] = 1.0 - amt;
			}	
		}
		else if(pan > 0x40)
		{
			//Right
			out[1][0] = 0.0; out[1][1] = 1.0;
			if(pan == 0x7F)
			{
				//Send all right
				out[0][0] = 0.0; out[1][1] = 1.0;
			}
			else
			{
				double amt = (double)(0x7F-pan)/(double)0x40;	
				out[0][0] = 1.0 - amt; 
				out[0][1] = amt;
			}	
		}
		
		
		return out;
	}
	
}
