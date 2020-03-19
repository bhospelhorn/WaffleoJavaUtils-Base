package waffleoRai_soundbank.adsr;

public class Attack {
	
	public static final double DEFO_POWER = 2.0;
	public static final double MIN_DB = 100.0;
	
	private int iTime;
	private ADSRMode eMode;
	
	private int tblSR;
	private int[] iTable;

	protected Attack()
	{
		iTime = 0;
		eMode = ADSRMode.LINEAR_ENVELOPE;
	}
	
	public Attack(int millis, ADSRMode mode)
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
	
	public static Attack getDefault()
	{
		return new Attack(0, ADSRMode.LINEAR_ENVELOPE);
	}
	
	public int[] getLevelTable(int sampleRate)
	{
		if(iTable != null && tblSR == sampleRate) return iTable;
		tblSR = sampleRate;
		
		//Determine #samples
		double seconds = (double)iTime/1000.0;
		int samples = (int)Math.round(seconds * (double)sampleRate);
		iTable = new int[samples];
		
		if(eMode == ADSRMode.EXPONENTIAL_ENVELOPE)
		{
			//lv = ((p^(t/maxtime))/p) * maxlv
			double p = DEFO_POWER;
			for(int i = 0; i < samples; i++)
			{
				double ratio = (double)i / (double)samples;
				double pow = Math.pow(p, ratio);
				double val = pow /p;
				val *= (double)0x7FFFFFFF;
				iTable[i] = (int)Math.round(val);
			}
		}
		else if(eMode == ADSRMode.LINEAR_ENVELOPE)
		{
			// l = (maxlv/atime) * time
			double ratio = (double)0x7FFFFFFF / (double)samples;
			for(int i = 0; i < samples; i++)
			{
				iTable[i] = (int)(Math.round(ratio * (double)i));
			}
		}
		else if(eMode == ADSRMode.LINEAR_DB)
		{
			//db = ((MINDB)/maxtime)*time - MINDB
			//l = maxlv * (10 ^ (db/20))
			double slope = MIN_DB/(double)samples;
			for(int i = 0; i < samples; i++)
			{
				double db = (slope * (double)i) - MIN_DB;
				double val = Math.pow(10.0, db/20.0);
				val *= (double)0x7FFFFFFF;
				iTable[i] = (int)(Math.round(val));
			}
		}
		else if(eMode == ADSRMode.PSX_PSEUDOEXPONENTIAL)
		{
			//Normally, Above 0x6000, then its slope increases (4 fold)
			//For now, just give linear dB
			//If want properly PSX scaling, override Attack
			double slope = MIN_DB/(double)samples;
			for(int i = 0; i < samples; i++)
			{
				double db = (slope * (double)i) - MIN_DB;
				double val = Math.pow(10.0, db/10.0);
				val *= (double)0x7FFFFFFF;
				iTable[i] = (int)(Math.round(val));
			}
		}
		
		return iTable;
	}
	
	public EnvelopeStreamer openStream(int sampleRate)
	{
		int samps = (int)Math.round(((double)sampleRate * (double)iTime) / 1000.0);
		if(eMode == ADSRMode.LINEAR_ENVELOPE)
		{
			return new ADSRLinearAmpRamper(true, 0.0, 1.0, samps);
		}
		else if(eMode == ADSRMode.LINEAR_DB)
		{
			return new ADSRLinearDBRamper(true, 0.0, 1.0, samps);
		}
		
		return null;
	}
	
}
