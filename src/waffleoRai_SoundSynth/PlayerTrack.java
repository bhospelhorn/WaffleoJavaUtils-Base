package waffleoRai_SoundSynth;

/*
 * UPDATES
 * 
 * 2020.03.20 | 1.0.0
 * 		Initial Documentation
 * 2020.05.10 | 1.1.0
 * 		Added clearPlaybackResources()
 */

/**
 * Interface for a SynthPlayer track. Player-specific
 * implementation should be responsible for interpreting and
 * handling sequencer commands.
 * @author Blythe Hospelhorn
 * @version 1.1.0
 * @since May 10, 2020
 */
public interface PlayerTrack {

	/**
	 * Execute all sequence commands correlating to the provided
	 * tick value (for sequences with non-random access, the tick
	 * value can be ignored).
	 * @param tick Current value of the tick from the player.
	 * @throws InterruptedException If command execution involves any thread blocking
	 * to wait for a buffer, and an unexpected interrupt occurs while blocked.
	 * @since 1.0.0
	 */
	public void onTick(long tick) throws InterruptedException;
	
	/**
	 * Reset the track to the state it was in at the given tick.
	 * <br><b>IMPORTANT</b> - Some implementations that don't use
	 * time coordinates or don't allow for random access to sequence events
	 * may only be capable of resetting to zero or a loop point!
	 * @param tick Tick to reset track to.
	 * @param loop Is this a loop reset?
	 * @since 1.0.0
	 */
	public void resetTo(long tick, boolean loop);
	
	/**
	 * Check whether the track has ended (eg. a track end
	 * command has been executed). 
	 * @return True if track end has been reached. False otherwise.
	 * @since 1.0.0
	 */
	public boolean trackEnd();
	
	/**
	 * Set track mute. If the track is muted, it may continue to advance
	 * its relative time position, but shouldn't send any voice-on commands
	 * to any channels.
	 * @param b True to mute track, false to unmute track.
	 * @since 1.0.0
	 */
	public void setMute(boolean b);
	
	/**
	 * Check whether the track is muted. If the track is muted, it may continue to advance
	 * its relative time position, but shouldn't send any voice-on commands
	 * to any channels.
	 * @return True if track is muted, false if track is not muted.
	 * @since 1.0.0
	 */
	public boolean isMuted();
	
	/**
	 * Dispose of any playback specific resources contained within this
	 * track when no longer needed to free up memory and clear references.
	 * @since 1.1.0
	 */
	public void clearPlaybackResources();
	
}
