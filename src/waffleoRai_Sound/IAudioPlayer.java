package waffleoRai_Sound;

import javax.sound.sampled.LineUnavailableException;

import waffleoRai_SoundSynth.AudioSampleStream;

/*
 * UPDATES
 * 
 * 201219 | 1.0.0
 * 	Creation/Initial Doc 
 * 210131 | 1.1.0
 * 	Added master volume control method 
 * 
 */

/**
 * An interface for playing an <code>AudioSampleStream</code> directly.
 * Should handle any kind of system resource acquisition, buffering,
 * timing, and conversion between samples and bytes.
 * @author Blythe Hospelhorn
 * @version 1.1.0
 * @since January 31, 2021
 */
public interface IAudioPlayer {

	/**
	 * Get a reference to the source stream being used for this
	 * player.
	 * @return Player source stream, or <code>null</code> if none.
	 * @since 1.0.0
	 */
	public AudioSampleStream getStream();
	
	/**
	 * Get whether this player automatically closes itself and the
	 * input stream when the input stream runs out of samples.
	 * <br>If the input stream is set to loop indefinitely, the
	 * player will never close even if this value is <code>true</code>.
	 * @return <code>true</code> if player is set to close upon stream end.
	 * <code>false</code> if not (line will be fed zero samples until closed).
	 * @since 1.0.0
	 */
	public boolean closeOnStreamEnd();
	
	/**
	 * Get whether this player is currently engaged in playback.
	 * This does not necessarily indicate whether the output line is open, as the
	 * player may be paused with the line open sending zero samples. 
	 * @return <code>true</code> if player is running. <code>false</code> if not.
	 * @since 1.0.0
	 */
	public boolean isRunning();
	
	/**
	 * Get whether this player and its input stream have been closed.
	 * If so, then this player cannot be used again.
	 * @return <code>true</code> if player has been closed. <code>false</code>
	 * otherwise.
	 * @since 1.0.0
	 */
	public boolean isClosed();
	
	/**
	 * Get whether this player can be rewound, that is,
	 * whether this player can be reset to the beginning of the input
	 * stream. This may not be possible depending upon the implementation
	 * of the player and the source <code>AudioSampleStream</code>.
	 * @return <code>true</code> if player can be rewound. 
	 * <code>false</code> if not, and the player must be disposed of after stopping.
	 * @since 1.0.0
	 */
	public boolean rewindable();
	
	/**
	 * Get the current position, in samples, of the playback.
	 * This may not be exact and its interval may depend upon the timing of 
	 * the internal updater.
	 * @return Current position of playback in samples, relative to input stream
	 * start upon player opening.
	 * @since 1.0.0
	 */
	public long getPlaybackLocation();
	
	/**
	 * Get the current position, in samples, of the buffer.
	 * This may not be exact and its interval may depend upon the timing of 
	 * the internal updater.
	 * @return Current position of buffer in samples, relative to input stream
	 * start upon player opening.
	 * @since 1.0.0
	 */
	public long getBufferLocation();
	
	/**
	 * Set the master output volume of this player.
	 * @param factor Value from 0x0 - 0x7fffffff representing
	 * the proportion relative to 0x7fffffff the total volume should
	 * be set to. Negative values are taken to be 0.
	 * @since 1.1.0
	 */
	public void setMasterVolume(int factor);
	
	/**
	 * Set the master output volume of this player.
	 * @param amt Ratio relative to 1.0 to set the master volume to.
	 * Values < 0.0 are set to 0.0. 
	 * @since 1.1.0
	 */
	public void setMasterVolume(float amt);
	
	/**
	 * Gets whether this player has the ability to have its master
	 * volume adjusted.
	 * @return True if player reacts to master volume changes. False if
	 * not.
	 * @since 1.1.0
	 */
	public boolean masterVolumeEnabled();
	
	/**
	 * Set whether this player is on mute.
	 * @param b True to mute, false to unmute.
	 * @return Current mute value
	 * @since 1.1.0
	 */
	public boolean setMute(boolean b);
	
	/**
	 * Begin playback of the input audio stream. If the player/line
	 * has not been opened, then it will be opened before playback.
	 * <br>Calling this method without calling <code>open()</code> first may result
	 * in a delay in playback due to the system line not having any pre-buffered samples.
	 * @since 1.0.0
	 * @throws UnsupportedOperationException If the player or stream is closed.
	 */
	public void play() throws LineUnavailableException;
	
	/**
	 * Pause playback of the input audio stream. Depending upon the implementation,
	 * either the line will be left open with zero samples being sent, or the line
	 * may be closed and a new one generated for the next play command.
	 * <br>The input stream will remain as it is, so restarting the playback should
	 * restart sample pulling from the input stream where it was left off, assuming
	 * the stream has not since been modified.
	 * @since 1.0.0
	 * @throws UnsupportedOperationException If the player or stream is closed.
	 */
	public void pause();
	
	/**
	 * Stop playback of the stream. 
	 * <br>The stream position should be reset to zero. If the
	 * stream or player is not rewindable, the player & stream may
	 * be closed depending upon the implementation.
	 * @since 1.0.0
	 * @throws UnsupportedOperationException If the player or stream is closed.
	 */
	public void stop();
	
	/**
	 * Open the audio output line to allow for pre-buffering before playback starts.
	 * @since 1.0.0
	 * @throws UnsupportedOperationException If the player or stream is closed.
	 */
	public void open() throws LineUnavailableException;
	
	/**
	 * Close the player and dispose of any system resources it has requested.
	 * <br>The player should not be usable after this is called.
	 * @since 1.0.0
	 */
	public void close();
	
}
