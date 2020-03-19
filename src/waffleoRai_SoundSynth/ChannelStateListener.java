package waffleoRai_SoundSynth;

/*
 * UPDATES
 * 
 * 2020.03.19 | 1.0.0
 * 		Initial Documentation
 * 
 */

/**
 * An interface for listening to the state of and events fired by
 * a synth channel from within a player.
 * @author Blythe Hospelhorn
 * @version 1.0.0
 * @since March 19, 2020
 */
public interface ChannelStateListener {

	/**
	 * Send level for every audio channel output by linked synth channel
	 * at specific time to this listener.
	 * @param level Current audio level (PCM, wrapped in signed Java int) for
	 * every audio channel.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void sendLevel(int[] level, long time);
	
	/**
	 * Notify listener that a Note On sequence instruction has been sent to
	 * and received by a linked synth channel.
	 * @param note MIDI standard pitch value (0-127) of Note On.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onNoteOn(byte note, long time);
	
	/**
	 * Notify listener that a Note Off sequence instruction has been sent to
	 * and received by a linked synth channel.
	 * @param note MIDI standard pitch value (0-127) of Note Off.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onNoteOff(byte note, long time);
	
	/**
	 * Notify listener that a Program Change sequence instruction has been sent
	 * to and received by a linked synth channel.
	 * @param bank Index (0-65536) of bank in target patch.
	 * @param program Index (0-127) of program in target patch.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void setProgram(int bank, int program, long time);
	
	/**
	 * Notify listener that a Pitch Bend/Set Pitch Wheel sequence instruction 
	 * has been sent to and received by a linked synth channel.
	 * @param value New raw value of pitch wheel location as a signed short centered
	 * around 0 where 0x7FFF is the maximum position and 0x8000 is the minimum.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onPitchWheelSet(short value, long time);
	
	/**
	 * Notify listener that a Pitch Bend/Set Mod Wheel sequence instruction 
	 * has been sent to and received by a linked synth channel.
	 * @param value New raw value of mod wheel location as a signed short centered
	 * around 0 where 0x7FFF is the maximum position and 0x8000 is the minimum.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onModWheelSet(short value, long time);
	
	/**
	 * Notify listener that the synth channel volume has been changed
	 * (usually either by a set volume or set expression instruction).
	 * @param level New channel volume level expressed in terms of an amplitude ratio
	 * between 0.0 (full attenuation) and 1.0 (no attenuation).
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onVolumeSet(double level, long time);
	
	/**
	 * Notify listener that the pan of a linked synth channel has been changed.
	 * @param pan Pan value as an unsigned byte (MIDI standard) - 0x40 is center, 0x7F is
	 * max right, 0x00 is max left.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onPanSet(byte pan, long time);
	
	/**
	 * Notify listener that an effect (such as reverb, LFO, or LPF) has been turned
	 * on or off for linked synth channel.
	 * @param effect Int enum indicating which effect has been switched on or off. 
	 * Values are implementation dependent.
	 * @param b True if effect has been turned on. False if effect has been turned off.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onEffectSet(int effect, boolean b, long time);
	
}
