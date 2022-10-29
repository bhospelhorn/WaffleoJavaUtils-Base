package waffleoRai_SeqSound;

//http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html
//https://anotherproducer.com/online-tools-for-musicians/midi-cc-list/

public class MIDIControllers {
	public static final int BANK_SELECT = 0x00;
	public static final int MOD_WHEEL = 0x01;
	public static final int BREATH_CTRL = 0x02;
	public static final int FOOT_PEDAL = 0x04;
	public static final int PORTAMENTO_TIME = 0x05;
	public static final int DATA_ENTRY = 0x06;
	public static final int VOLUME = 0x07;
	public static final int BALANCE = 0x08;
	public static final int PAN = 0x0a;
	public static final int EXPRESSION = 0x0b;
	public static final int EFFECTS_1 = 0x0c;
	public static final int EFFECTS_2 = 0x0d;
	public static final int GENERAL_PURPOSE_1 = 0x10;
	public static final int GENERAL_PURPOSE_2 = 0x11;
	public static final int GENERAL_PURPOSE_3 = 0x12;
	public static final int GENERAL_PURPOSE_4 = 0x13;
	
	public static final int BANK_SELECT_LSB = 0x20;
	public static final int MOD_WHEEL_LSB = 0x21;
	public static final int BREATH_CTRL_LSB = 0x22;
	public static final int FOOT_PEDAL_LSB = 0x24;
	public static final int PORTAMENTO_TIME_LSB = 0x25;
	public static final int DATA_ENTRY_LSB = 0x26;
	public static final int VOLUME_LSB = 0x27;
	public static final int BALANCE_LSB = 0x28;
	public static final int PAN_LSB = 0x2a;
	public static final int EXPRESSION_LSB = 0x2b;
	public static final int EFFECTS_1_LSB = 0x2c;
	public static final int EFFECTS_2_LSB = 0x2d;
	public static final int GENERAL_PURPOSE_1_LSB = 0x30;
	public static final int GENERAL_PURPOSE_2_LSB = 0x31;
	public static final int GENERAL_PURPOSE_3_LSB = 0x32;
	public static final int GENERAL_PURPOSE_4_LSB = 0x33;
	
	public static final int DAMPER_PEDAL_ON = 0x40;
	public static final int PORTAMENTO_ON = 0x41;
	public static final int SUSTENUDO_PEDAL_ON = 0x42;
	public static final int SOFT_PEDAL_ON = 0x43;
	public static final int LEGATO_ON = 0x44;
	public static final int HOLD_ON = 0x45;
	
	public static final int SOUND_CTRLR_01 = 0x46;
	public static final int SOUND_CTRLR_02 = 0x47;
	public static final int SOUND_CTRLR_03 = 0x48;
	public static final int SOUND_CTRLR_04 = 0x49;
	public static final int SOUND_CTRLR_05 = 0x4a;
	public static final int SOUND_CTRLR_06 = 0x4b;
	public static final int SOUND_CTRLR_07 = 0x4c;
	public static final int SOUND_CTRLR_08 = 0x4d;
	public static final int SOUND_CTRLR_09 = 0x4e;
	public static final int SOUND_CTRLR_10 = 0x4f;
	
	public static final int SOUND_VARIATION = 0x46;
	public static final int RESONANCE = 0x47;
	public static final int RELEASE_TIME = 0x48;
	public static final int ATTACK_TIME = 0x49;
	public static final int LPF_CUTOFF_FREQ = 0x4a;
	
	public static final int GENERAL_PURPOSE_SWITCH_A = 0x50;
	public static final int GENERAL_PURPOSE_SWITCH_B = 0x51;
	public static final int GENERAL_PURPOSE_SWITCH_C = 0x52;
	public static final int GENERAL_PURPOSE_SWITCH_D = 0x53;
	
	public static final int DECAY_TIME = 0x50;
	public static final int HPF_CUTOFF_FREQ = 0x51;
	
	public static final int PORTAMENTO_CONTROL = 0x54;
	public static final int VELOCITY_PREFIX = 0x58;
	
	public static final int EFFECT_DEPTH_1 = 0x5b;
	public static final int EFFECT_DEPTH_2 = 0x5c;
	public static final int EFFECT_DEPTH_3 = 0x5d;
	public static final int EFFECT_DEPTH_4 = 0x5e;
	public static final int EFFECT_DEPTH_5 = 0x5f;
	
	public static final int REVERB_SEND = 0x5b;
	public static final int TREMOLO_SEND = 0x5c;
	public static final int CHORUS_SEND = 0x5d;
	public static final int DETUNE_SEND = 0x5e;
	public static final int PHASER_SEND = 0x5f;
	
	public static final int DATA_INCR = 0x60;
	public static final int DATA_DECR = 0x61;
	public static final int NRPN_LSB = 0x62;
	public static final int NRPN_MSB = 0x63;
	public static final int RPN_LSB = 0x64;
	public static final int RPN_MSB = 0x65;
	
	public static final int SET_MONOPHONIC = 0x7e;
	public static final int SET_POLYPHONIC = 0x7f;
}
