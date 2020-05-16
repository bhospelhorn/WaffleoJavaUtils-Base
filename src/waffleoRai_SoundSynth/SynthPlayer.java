package waffleoRai_SoundSynth;

import java.io.IOException;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/*
 * UPDATES
 * 
 * 2020.03.19 | 1.0.0
 * 		Initial Documentation
 * 
 * 2020.05.10 | 1.1.0
 * 		Added removeListener(Object o) and dispose()
 * 
 */

/**
 * An interface for players that generate sound from synthesizers,
 * soundbanks, and/or sequence data.
 * @author Blythe Hospelhorn
 * @version 1.1.0
 * @since May 10, 2020
 */
public interface SynthPlayer extends AudioSampleStream{
	
	/**
	 * Get a descriptor for the loaded sequence, if there is one.
	 * This can be anything like the file name, an internal name, or just an index.
	 * @return String representing a name for the loaded sequence, or null if none.
	 * @since 1.0.0
	 */
	public String getSequenceName();
	
	/**
	 * Get a descriptor for the loaded soundbank, if there is one.
	 * This can be anything like the file name, an internal name, or just an index.
	 * @return String representing a name for the loaded soundbank, or null if none.
	 * @since 1.0.0
	 */
	public String getBankName();
	
	/**
	 * Get a String describing the player or type of data the player
	 * handles.
	 * @return A String describing the player, audio, sequence, or bank data type(s).
	 * @since 1.0.0
	 */
	public String getTypeInfoString();
	
	/**
	 * Get an AudioFormat object (Java Sampled Sound API) describing
	 * the audio output of the player.
	 * @return An AudioFormat describing the audio output (sample rate, bit depth etc.)
	 * @since 1.0.0
	 */
	public AudioFormat getOutputFormat();
	
	/**
	 * Get whether audio output can be captured from this player.
	 * @return True if audio output can be captured. In this case,
	 * it can be used for GUI level monitoring or to write the
	 * output to disk in a sound file.
	 * <br>False if the output cannot be captured. This can be the 
	 * case when the System synthesizer is used.
	 * @since 1.0.0
	 */
	public boolean isCapturable();
	
	/**
	 * Get the currently set Master attenuation (as an amplitude ratio)
	 * for this player.
	 * @return A value between 0.0 and 1.0 inclusive representing the
	 * value all samples are multiplied by before being output. This
	 * value can be easily converted to dB attenuation.
	 * @since 1.0.0
	 */
	public double getMasterAttenuation();
	
	/**
	 * Set the Master attenuation (represented by an amplitude ratio)
	 * for this player. This is the value all samples will be multiplied by
	 * before output.
	 * @param amp_ratio Value between 0.0 to 1.0 inclusive representing the amplitude
	 * ratio that should be applied to all samples output by this player.
	 * @since 1.0.0
	 */
	public void setMasterAttenuation(double amp_ratio);
	
	/**
	 * Get the number of synthesis channels (eg. MIDI channels) used
	 * by this player.
	 * <br>Note that this is different from getChannelCount(), which instead
	 * retrieves the number of <i>audio</i> channels.
	 * @return Number of synthesis channels used by player.
	 * @since 1.0.0
	 */
	public int getSynthChannelCount();
	
	/**
	 * Get whether a specific synth channel is muted (ie. audio samples are not
	 * being forwarded to the master mixer).
	 * @param idx Index of synth channel. Cannot be less than 0 or
	 * greater than the maximum number of synth channels.
	 * @return True if the channel is currently muted. False if not.
	 * @since 1.0.0
	 */
	public boolean channelMuted(int idx);
	
	/**
	 * Get whether a specific synth channel is soloed
	 * (ie. only it and other soloed channels are forwarding audio
	 * samples to master mixer).
	 * @param idx Index of synth channel. Cannot be less than 0 or
	 * greater than the maximum number of synth channels.
	 * @return True if the channel is currently soloed. False if not.
	 * @since 1.0.0
	 */
	public boolean channelSoloed(int idx);
	
	/**
	 * Set mute for specific synth channel.
	 * @param ch_idx Index of synth channel. Cannot be less than 0 or
	 * greater than the maximum number of synth channels.
	 * @param mute True to mute channel, False to unmute.
	 * @since 1.0.0
	 */
	public void setChannelMute(int ch_idx, boolean mute);
	
	/**
	 * Set solo for specific synth channel.
	 * @param ch_idx Index of synth channel. Cannot be less than 0 or
	 * @param solo True to set solo. False to unset solo.
	 * @since 1.0.0
	 */
	public void setChannelSolo(int ch_idx, boolean solo);
	
	/**
	 * Get the number of sequence data tracks used by this player.
	 * <br>Note that although in many sequence types tracks and (synth) channels
	 * are one-to-one, in this framework a track refers to a structure/object
	 * holding sequence data and a channel refers to an object responsible for
	 * accepting sequence commands and converting them to audio.
	 * @return The number of tracks used by this player.
	 * @since 1.0.0
	 */
	public int getTrackCount();
	
	/**
	 * Get whether a specific track is muted (ie. not sending sequencing
	 * signals to synthesizer)
	 * @param idx Index of track. Cannot be less than 0 or
	 * greater than the maximum number of tracks.
	 * @return True if track is muted. False if track is not muted.
	 * @since 1.0.0
	 */
	public boolean trackMuted(int idx);
	
	/**
	 * Get whether a specific track is soloed (ie. only it and
	 * other soloed tracks are sending sequencing signals to synth)
	 * @param idx Index of track. Cannot be less than 0 or
	 * greater than the maximum number of tracks.
	 * @return True if track is soloed. False if track is not soloed.
	 * @since 1.0.0
	 */
	public boolean trackSoloed(int idx);
	
	/**
	 * Set mute for a track.
	 * @param tr_idx Index of track. Cannot be less than 0 or
	 * greater than the maximum number of tracks.
	 * @param mute True to set mute. False to unset mute.
	 * @since 1.0.0
	 */
	public void setTrackMute(int tr_idx, boolean mute);
	
	/**
	 * Set solo for a track.
	 * @param tr_idx Index of track. Cannot be less than 0 or
	 * greater than the maximum number of tracks.
	 * @param solo True to set solo. False to unset solo.
	 * @since 1.0.0
	 */
	public void setTrackSolo(int tr_idx, boolean solo);
	
	/**
	 * Add a player state listener to this player. Note that
	 * it is up to the implementation of the player to determine
	 * whether or not signals get sent to listeners.
	 * @param l Listener to register with player.
	 * @since 1.0.0
	 */
	public void addListener(PlayerListener l);
	
	/**
	 * Remove a listener from this player. Listener will only be removed
	 * if REFERENCE of provided object matches a listener in the listener
	 * list.
	 * @param o Listener to remove
	 * @since 1.1.0
	 */
	public void removeListener(Object o);
	
	/**
	 * Clear all player listeners from this player.
	 * @since 1.0.0
	 */
	public void clearListeners();
	
	/**
	 * Add a synth channel state listener to a synth channel 
	 * in this player. 
	 * Note that it is up to the implementation of the 
	 * player and channels to determine whether or not signals 
	 * get sent to listeners.
	 * @param ch_idx Index of synth channel to link listener to.
	 * @param l Listener to add.
	 * @since 1.0.0
	 */
	public void addChannelListener(int ch_idx, ChannelStateListener l);
	
	/**
	 * Remove a channel state listener from a synth channel in
	 * this player.
	 * @param ch_idx Index of synth channel to remove listener from.
	 * @param l Listener to remove.
	 * @since 1.0.0
	 */
	public void removeChannelListener(int ch_idx, Object l);
	
	/**
	 * Clear all synth channel state listeners from this player.
	 * @since 1.0.0
	 */
	public void clearChannelListeners();
	
	/**
	 * Reset this player to its initial state. This stops any playback.
	 * @since 1.0.0
	 */
	public void rewind();
	
	/**
	 * Induce this player to open an audio line to the system default playback
	 * device and start sending samples as fast as it synthesizes them.
	 * <br>Implementations are encouraged to buffer asynchronously to playback, but
	 * in cases where for example the system synthesizer is used, synthesis may end
	 * up being more or less synchronous to playback (possibly resulting in lag).
	 * @throws LineUnavailableException If line to system audio device could not be opened.
	 * @throws MidiUnavailableException In the event that the Java synthesizer API is utilized,
	 * this exception may be thrown if a synthesizer cannot be initialized.
	 * @since 1.0.0
	 */
	public void startAsyncPlaybackToDefaultOutputDevice() throws LineUnavailableException, MidiUnavailableException;
	
	/**
	 * Signal player to start sending samples to the provided data line (beginning
	 * from the player's state when it is called) as fast as it synthesizes them.
	 * <br><b>!WARNING!</b> This method may throw an exception if the player is not capturable.
	 * @param line Line to send audio data to.
	 * @since 1.0.0
	 */
	public void startAsyncStreamTo(SourceDataLine line);
	
	/**
	 * Stop synthesis and close open data lines that are accepting output audio.
	 * @since 1.0.0
	 */
	public void stop();
	
	/**
	 * Pause playback. This method does not close any data lines, it simply stops
	 * the synthesizer from synthesizing and outputting new samples.
	 * The player state is preserved until unpaused unless stop() or rewind() is called.
	 * @since 1.0.0
	 */
	public void pause();
	
	/**
	 * Resume paused playback. If the player isn't in a paused state, this method 
	 * does nothing.
	 * @since 1.0.0
	 */
	public void unpause();
	
	/**
	 * Get whether this player is in a paused state.
	 * @return True if player is paused, false otherwise.
	 * @since 1.0.0
	 */
	public boolean isPaused();
	
	/**
	 * Get the current playback (not player buffer) position. Value is usually
	 * in terms of audio samples since playback start or ticks.
	 * @return Playback position.
	 * @since 1.0.0
	 */
	public long getPlaybackPosition();
	
	/**
	 * Write finite output of player directly to a WAV file without
	 * realtime playback.
	 * @param path Path of output file.
	 * @param loops Number of loops of loaded sequence to write. If sequence
	 * or player does not loop, then this parameter is ignored.
	 * @throws IOException If output file cannot be opened or written to.
	 * @since 1.0.0
	 */
	public void writeMixdownTo(String path, int loops) throws IOException;
	
	/**
	 * Write finite output of a single player synth channel directly to
	 * a WAV file without realtime playback.
	 * @param pathPrefix Prefix of output path to write WAV file to. Channel number
	 * and file extension are added to the end.
	 * @param loops Number of loops of loaded sequence to write. If sequence
	 * or player does not loop, then this parameter is ignored.
	 * @param ch Index of synth channel to capture.
	 * @throws IOException If output file cannot be opened or written to.
	 * @since 1.0.0
	 */
	public void writeChannelTo(String pathPrefix, int loops, int ch) throws IOException;
	
	/**
	 * Write finite output of each player synth channel to a separate WAV file
	 * directly without realtime playback.
	 * @param pathPrefix Prefix of output path to write WAV file to. Channel number
	 * and file extension are added to the end.
	 * @param loops Number of loops of loaded sequence to write. If sequence
	 * or player does not loop, then this parameter is ignored.
	 * @throws IOException If output file(s) cannot be opened or written to.
	 * @since 1.0.0
	 */
	public void writeChannelsTo(String pathPrefix, int loops) throws IOException;

	/**
	 * Clear all resources unique to this player instance to free up memory
	 * and remove references.
	 * Whether or not the player is usable again after this method is called
	 * depends upon the implementation. It's best to assume that it is not.
	 * @since 1.1.0
	 */
	public void dispose();
	
}
