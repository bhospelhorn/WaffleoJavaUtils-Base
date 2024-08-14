package waffleoRai_SeqSound;

public class MIDIMetaCommands {
	
	public static final int SEQNO = 0x00;
	public static final int TEXT = 0x01;
	public static final int COPYRIGHT = 0x02;
	public static final int NAME = 0x03;
	public static final int INSTRUMENT_NAME = 0x04;
	public static final int LYRIC = 0x05;
	public static final int MARKER = 0x06;
	public static final int CUE = 0x07;
	
	public static final int CH_PREFIX = 0x20;
	public static final int END = 0x2f;
	public static final int TEMPO = 0x51;
	public static final int SMPTE_OFF = 0x54;
	public static final int TIMESIG = 0x58;
	public static final int KEYSIG = 0x59;
	public static final int CUSTOM = 0x7f;
	
	public static final int LEN_SEQNO = 2;
	public static final int LEN_CH_PREFIX = 1;
	public static final int LEN_END = 0;
	public static final int LEN_TEMPO = 3;
	public static final int LEN_SMPTE_OFF = 5;
	public static final int LEN_TIMESIG = 4;
	public static final int LEN_KEYSIG = 2;

}
