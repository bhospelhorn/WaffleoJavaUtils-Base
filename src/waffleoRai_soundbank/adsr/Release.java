package waffleoRai_soundbank.adsr;

public class Release {
	
	public static final double MIN_DB = 100.0;
	public static final double DEFO_POWER = 2.0;
	
	private int iTime;
	private ADSRMode eMode;
	
	private int tblSR;
	private int[] iTable;
	
	protected Release()
	{
		iTime = 0;
		eMode = ADSRMode.LINEAR_DB;
	}
	
	public Release(int millis, ADSRMode mode)
	{
		iTime = millis;
		eMode = mode;
	}
	
	public int getTime()
	{
		return iTime;
	}
	
	public ADSRMode getMode()
	{
		return eMode;
	}
	
	public void setTime(int millis)
	{
		iTime = millis;
		iTable = null;
	}
	
	public void setMode(ADSRMode mode)
	{
		eMode = mode;
		iTable = null;
	}
	
	public static Release getDefault()
	{
		return new Release(0, ADSRMode.LINEAR_DB);
	}
	
	public int[] getLevelTable(int sampleRate)
	{
		//To min level. (just stop decay when hit sustain level)
		if(iTable != null && tblSR == sampleRate) return iTable;
		tblSR = sampleRate;
		
		//Determine #samples
		double seconds = (double)iTime/1000.0;
		int samples = (int)Math.round(seconds * (double)sampleRate);
		iTable = new int[samples];
		
		if(eMode == ADSRMode.EXPONENTIAL_ENVELOPE)
		{
			//lv = ((p^((maxtime-t)/maxtime))/p) * maxlv
			double p = DEFO_POWER;
			double dsamps = (double)samples;
			double maxl = (double)0x7FFFFFFF;
			for(int i = 0; i < samples; i++)
			{
				double val = dsamps - (double)i;
				val = val/dsamps;
				val = Math.pow(p, val);
				val = val/p;
				val = val * maxl;
				iTable[i] = (int)Math.round(val);
			}
		}
		else if(eMode == ADSRMode.LINEAR_ENVELOPE)
		{
			//l = (-maxlv/maxtime)*time + maxlv
			double dsamps = (double)samples;
			double maxl = (double)0x7FFFFFFF;
			double slope = (-1.0) * (maxl/dsamps);
			for(int i = 0; i < samples; i++)
			{
				double val = slope * (double)i;
				val += maxl;
				iTable[i] = (int)Math.round(val);
			}
		}
		else if(eMode == ADSRMode.LINEAR_DB)
		{
			//db = (-(MINDB)/maxtime)*time
			//l = maxlv * (10 ^ (db/20))
			double dsamps = (double)samples;
			double maxl = (double)0x7FFFFFFF;
			double slope = (-1.0) * (MIN_DB/dsamps);
			for(int i = 0; i < samples; i++)
			{
				double val = slope * (double)i; //dB
				val = val/20.0;
				val = Math.pow(10.0, val);
				val *= maxl;
				iTable[i] = (int)Math.round(val);
			}
		}
		else if(eMode == ADSRMode.PSX_EXPONENTIAL_DECAY)
		{
			//I'll do later...
		}
		
		return iTable;
	}
	
	public EnvelopeStreamer openStream(int sampleRate, double suslev)
	{
		int samps = (int)Math.round(((double)sampleRate * (double)iTime) / 1000.0);
		if(eMode == ADSRMode.LINEAR_ENVELOPE)
		{
			return new ADSRLinearAmpRamper(false, suslev, 0.0, samps);
		}
		else if(eMode == ADSRMode.LINEAR_DB)
		{
			//return new ADSRLinearDBRamper(false, suslev, 0.0, samps);
			return new ADSRLinearDBFullRampdown(suslev, 0.0, samps);
		}
		return null;
	}
	
	public EnvelopeStreamer openStream(int sampleRate)
	{
		int samps = (int)Math.round(((double)sampleRate * (double)iTime) / 1000.0);
		if(eMode == ADSRMode.LINEAR_ENVELOPE)
		{
			return new ADSRLinearAmpRamper(false, 1.0, 0.0, samps);
		}
		else if(eMode == ADSRMode.LINEAR_DB)
		{
			//return new ADSRLinearDBRamper(false, 1.0, 0.0, samps);
			return new ADSRLinearDBFullRampdown(samps);
		}
		return null;
	}

}
