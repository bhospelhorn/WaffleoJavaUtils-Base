package waffleoRai_soundbank.adsr;

public class Sustain {
	
	public static final double MIN_DB = 100.0;
	public static final double DEFO_POWER = 2.0;
	
	//private int iMax;
	
	private int iLevel;
	
	private int iTime;
	private boolean bDirection;
	private ADSRMode eMode;
	
	private int tblSR;
	private int[] iTable;
	
	public Sustain(int level)
	{
		iLevel = level;
		bDirection = false;
		iTime = 0;
		eMode = ADSRMode.STATIC;
		//iMax = maxLevel;
	}
	
	public Sustain(int level, boolean up, int millis, ADSRMode mode)
	{
		bDirection = up;
		iLevel = level;
		iTime = millis;
		eMode = mode;
		//iMax = maxLevel;
	}
	
	public int getTime()
	{
		return iTime;
	}
	
	public ADSRMode getMode()
	{
		return eMode;
	}
	
	public int getLevel32()
	{
		return iLevel;
	}
	
	public int getLevel16()
	{
		double prop = (double)iLevel/(double)0x7FFFFFFF;
		return (int)Math.round(prop * (double)0x7FFF);
	}
	
	public double getLevelDbl(){
		return (double)iLevel/ (double)0x7FFFFFFF;
	}
	
	public boolean rampUp()
	{
		return bDirection;
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

	public void setLevel(int level)
	{
		iLevel = level;
	}
	
	public void setDirection(boolean up)
	{
		bDirection = up;
	}
	
	public static Sustain getDefault()
	{
		return new Sustain(Integer.MAX_VALUE);
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
			//UP:   lv = ((p^(t          /maxtime))/p) * maxlv
			//DOWN: lv = ((p^((maxtime-t)/maxtime))/p) * maxlv
			double p = DEFO_POWER;
			double dsamps = (double)samples;
			double maxl = (double)0x7FFFFFFF;
			for(int i = 0; i < samples; i++)
			{
				double val = (double)i;
				if(!bDirection) val = dsamps - val;
				val = val/dsamps;
				val = Math.pow(p, val);
				val = val/p;
				val = val * maxl;
				iTable[i] = (int)Math.round(val);
			}
		}
		else if(eMode == ADSRMode.LINEAR_ENVELOPE)
		{
			//UP:   l = (maxlv/maxtime) * time
			//DOWN: l = (-maxlv/maxtime) * time + maxlv
			double dsamps = (double)samples;
			double maxl = (double)0x7FFFFFFF;
			double slope = maxl/dsamps;
			if(!bDirection)slope *= -1.0;
			for(int i = 0; i < samples; i++)
			{
				double val = slope * (double)i;
				if(!bDirection) val += maxl;
				iTable[i] = (int)Math.round(val);
			}
		}
		else if(eMode == ADSRMode.LINEAR_DB)
		{
			//UP:   db = ((MINDB)/maxtime)*time - MINDB
			//DOWN: db = (-(MINDB)/maxtime)*time
			
			//l = maxlv * (10 ^ (db/20))
			double dsamps = (double)samples;
			double maxl = (double)0x7FFFFFFF;
			double slope = MIN_DB/dsamps;
			if(bDirection) slope *= -1.0;
			for(int i = 0; i < samples; i++)
			{
				double val = slope * (double)i; //dB
				if(bDirection) val -= MIN_DB;
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
	
	public EnvelopeStreamer openStream(int sampleRate)
	{
		if(eMode == ADSRMode.STATIC){
			return new ADSRStaticEnv(getLevelDbl());
		}
		else if(eMode == ADSRMode.LINEAR_ENVELOPE){
			int samps = (int)Math.round(((double)sampleRate * (double)iTime) / 1000.0);
			if(bDirection)return new ADSRLinearAmpRamper(true, getLevelDbl(), 1.0, samps);
			else return new ADSRLinearAmpRamper(false, getLevelDbl(), 0.0, samps);
		}
		else if(eMode == ADSRMode.LINEAR_DB){
			int samps = (int)Math.round(((double)sampleRate * (double)iTime) / 1000.0);
			if(bDirection) return new ADSRLinearDBRamper(true, getLevelDbl(), 1.0, samps);
			else return new ADSRLinearDBFullRampdown(samps);
		}
		return null;
	}
	
}
