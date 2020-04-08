package waffleoRai_Sound;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DBScaling {
	
	//-- dB table lookups (to bypass log10 and pow)
		
	private static ConcurrentMap<Integer, Double> db_to_amp_g0; //0-6 dB (0.0001 res)
	private static ConcurrentMap<Integer, Double> db_to_amp_g1; //6-20 dB (0.001 res)
	private static ConcurrentMap<Integer, Double> db_to_amp_g2; //20-50 dB (0.01 res)
	private static ConcurrentMap<Integer, Double> db_to_amp_g3; //50-64 dB (0.1 res)
	private static ConcurrentMap<Integer, Double> db_to_amp_g4; //64-100 dB (1 res)
	
	
	private static double getAmp_g0(double raw){
		if (db_to_amp_g0 == null) db_to_amp_g0 = new ConcurrentHashMap<Integer, Double>();
		
		int key = (int)Math.round(raw * 10000.0);
		Double val = db_to_amp_g0.get(key);
		if(val != null) return val;
		
		val = ((double)key / 10000.0);
		val = Math.pow(10.0, val/20.0);
		db_to_amp_g0.put(key, val);
		return val;
	}
	
	private static double getAmp_g1(double raw){
		if (db_to_amp_g1 == null) db_to_amp_g1 = new ConcurrentHashMap<Integer, Double>();
		
		int key = (int)Math.round(raw * 1000.0);
		Double val = db_to_amp_g1.get(key);
		if(val != null) return val;
		
		val = ((double)key / 1000.0);
		val = Math.pow(10.0, val/20.0);
		db_to_amp_g1.put(key, val);
		return val;
	}
	
	private static double getAmp_g2(double raw){
		if (db_to_amp_g2 == null) db_to_amp_g2 = new ConcurrentHashMap<Integer, Double>();
		
		int key = (int)Math.round(raw * 100.0);
		Double val = db_to_amp_g2.get(key);
		if(val != null) return val;
		
		val = ((double)key / 100.0);
		val = Math.pow(10.0, val/20.0);
		db_to_amp_g2.put(key, val);
		return val;
	}
	
	private static double getAmp_g3(double raw){
		if (db_to_amp_g3 == null) db_to_amp_g3 = new ConcurrentHashMap<Integer, Double>();
		
		int key = (int)Math.round(raw * 10.0);
		Double val = db_to_amp_g3.get(key);
		if(val != null) return val;
		
		val = ((double)key / 10.0);
		val = Math.pow(10.0, val/20.0);
		db_to_amp_g3.put(key, val);
		return val;
	}
	
	private static double getAmp_g4(double raw){
		if (db_to_amp_g4 == null) db_to_amp_g4 = new ConcurrentHashMap<Integer, Double>();
		
		int key = (int)Math.round(raw);
		Double val = db_to_amp_g4.get(key);
		if(val != null) return val;
		
		val = Math.pow(10.0, key/20.0);
		db_to_amp_g4.put(key, val);
		return val;
	}
	
	public static double quick_dB_2_ampratio(double db){
		if(db <= -100) return 0.0;
		
		if(db <= -64.0 || db >= 64.0)return getAmp_g4(db);
		else if(db <= -50.0 || db >= 50.0)return getAmp_g3(db);
		else if(db <= -20.0 || db >= 20.0)return getAmp_g2(db);
		else if(db <= -6.0 || db >= 6.0)return getAmp_g1(db);
		else return getAmp_g0(db);

	}

}
