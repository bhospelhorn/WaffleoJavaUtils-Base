package waffleoRai_Containers;

public class CDUtils {
	
	public static int fromBCD(int bcd_val){
		int out = 0;
		int shamt = 0;
		int factor = 1;
		for(int i = 0; i < 8; i++){
			out += ((bcd_val >>> shamt) & 0xf) * factor;
			shamt += 4;
			factor *= 10;
		}
		return out;
	}
	
	public static int toBCD(int val){
		int shamt = 0;
		int out = 0;
		
		for(int i = 0; i < 8; i++){
			int r = val % 10;
			out |= (r << shamt);
			shamt += 4;
			val /= 10;
		}
		
		return out;
	}
	
	public static int[] relSector2Time(int sector){
		//Standard image starts at rel sec 0 and 00:02:00
		sector += 150;
		int[] time = new int[3];
		time[2] = sector % 75;
		sector /= 75;
		time[1] = sector % 60;
		sector /= 60;
		time[0] = sector;
		
		return time;
	}
	
	public static int time2RelSector(int[] time){
		if(time == null) return -1;
		int sector = time[0] * 60 * 75;
		sector += (time[1]) * 75;
		sector += time[2];
		
		sector -= 150;
		return sector;
	}

}
