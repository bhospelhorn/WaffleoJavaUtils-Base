package waffleoRai_SoundSynth;

/*
 * UPDATES
 * 
 * 2020.03.19 | 1.0.0
 * 		Initial Documentation
 * 
 */

/**
 * An interface for an object that listens to state changes and events fired
 * by the activity of a SynthPlayer.
 * @author Blythe Hospelhorn
 * @version 1.0.0
 * @since March 19, 2020
 *
 */
public interface PlayerListener{
	
	/**
	 * Method to call when a player changes its tempo.
	 * @param usPerBeat New tempo in microseconds per beat (MIDI standard).
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onTempoChange(int usPerBeat, long time);
	
	/**
	 * Method to call on sequence tick.
	 * @param tick Current tick value.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void onTick(long tick, long time);
	
	/**
	 * Send audio level for every channel at specific time to this listener.
	 * @param level Current audio level (PCM, wrapped in signed Java int) for
	 * every audio channel.
	 * @param time Time that event occurred, usually in ticks or audio samples since
	 * playback start. This is used for synchronizing to playback of pre-buffered players.
	 * @since 1.0.0
	 */
	public void sendLevel(int[] level, long time);

}
