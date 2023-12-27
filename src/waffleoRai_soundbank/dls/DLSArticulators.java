package waffleoRai_soundbank.dls;

public class DLSArticulators {

	//Level 1
	public static final int CONN_SRC_NONE = 0;
	public static final int CONN_SRC_LFO = 1;
	public static final int CONN_SRC_KEYONVELOCITY = 2;
	public static final int CONN_SRC_KEYNUMBER = 3;
	public static final int CONN_SRC_EG1 = 4;
	public static final int CONN_SRC_EG2 = 5;
	public static final int CONN_SRC_PITCHWHEEL = 6;
	
	public static final int CONN_SRC_CC1 = 0x81; //Modulation
	public static final int CONN_SRC_CC7 = 0x87; //Channel volume
	public static final int CONN_SRC_CC10 = 0x8a; //Pan
	public static final int CONN_SRC_CC11 = 0x8b; //Expression
	
	public static final int CONN_SRC_RPN0 = 0x100; //Pitch bend range
	public static final int CONN_SRC_RPN1 = 0x101; //Fine tune
	public static final int CONN_SRC_RPN2 = 0x102; //Coarse tune
	
	public static final int CONN_DST_NONE = 0;
	public static final int CONN_DST_GAIN = 1;
	public static final int CONN_DST_RESERVED = 2;
	public static final int CONN_DST_PITCH = 3;
	public static final int CONN_DST_PAN = 4;
	
	public static final int CONN_DST_LFO_FREQUENCY = 0x104;
	public static final int CONN_DST_LFO_STARTDELAY = 0x105;
	
	public static final int CONN_DST_EG1_ATTACKTIME = 0x206;
	public static final int CONN_DST_EG1_DECAYTIME = 0x207;
	public static final int CONN_DST_EG1_RESERVED = 0x208;
	public static final int CONN_DST_EG1_RELEASETIME = 0x209;
	public static final int CONN_DST_EG1_SUSTAINLEVEL = 0x20a;
	public static final int CONN_DST_EG2_ATTACKTIME = 0x30a;
	public static final int CONN_DST_EG2_DECAYTIME = 0x30b;
	public static final int CONN_DST_EG2_RESERVED = 0x30c;
	public static final int CONN_DST_EG2_RELEASETIME = 0x30d;
	public static final int CONN_DST_EG2_SUSTAINLEVEL = 0x30e;
	
	public static final int CONN_TRN_NONE = 0;
	public static final int CONN_TRN_CONCAVE = 1;
	
	//Level 2
	public static final int CONN_SRC_POLYPRESSURE = 7;
	public static final int CONN_SRC_CHANNELPRESSURE = 8;
	public static final int CONN_SRC_VIBRATO = 9;
	
	public static final int CONN_SRC_CC91 = 0xdb; //Chorus send
	public static final int CONN_SRC_CC93 = 0xdd; //Reverb send
	
	public static final int CONN_DST_KEYNUMBER = 5;
	
	public static final int CONN_DST_LEFT = 0x10;
	public static final int CONN_DST_RIGHT = 0x11;
	public static final int CONN_DST_CENTER = 0x12;
	public static final int CONN_DST_LFE_CHANNEL = 0x13;
	public static final int CONN_DST_LEFTREAR = 0x14;
	public static final int CONN_DST_RIGHTREAR = 0x15;
	public static final int CONN_DST_CHORUS = 0x80;
	public static final int CONN_DST_REVERB = 0x81;
	
	public static final int CONN_DST_VIB_FREQUENCY = 0x114;
	public static final int CONN_DST_VIB_STARTDELAY = 0x115;
	
	public static final int CONN_DST_EG1_DELAYTIME = 0x20b;
	public static final int CONN_DST_EG1_HOLDTIME = 0x20c;
	public static final int CONN_DST_EG1_SHUTDOWNTIME = 0x20d;
	public static final int CONN_DST_EG2_DELAYTIME = 0x30f;
	public static final int CONN_DST_EG2_HOLDTIME = 0x310;
	
	public static final int CONN_DST_FILTER_CUTOFF = 0x500;
	public static final int CONN_DST_FILTER_Q = 0x501;
	
	public static final int CONN_TRN_CONVEX = 2;
	public static final int CONN_TRN_SWITCH = 3;
	
}
