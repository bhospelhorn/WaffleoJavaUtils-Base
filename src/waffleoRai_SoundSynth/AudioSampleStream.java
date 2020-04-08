package waffleoRai_SoundSynth;

/*
 * UPDATES
 * 
 * 2020.03.19 | 1.0.0
 * 		Initial Documentation
 * 
 * 2020.04.06 | 1.1.0
 * 		Added done() so automatic stopping for players is possible
 */

/**
 * A wrapper for streaming audio data that passes on PCM samples as
 * full samples stored in signed int values instead of as data stream bytes.
 * <br>This is set up to make it easier to apply filters downstream.
 * @author Blythe Hospelhorn
 * @version 1.1.0
 * @since April 6, 2020
 */
public interface AudioSampleStream {

	/**
	 * Get the output sample rate (in Hz) of this audio sample stream. 
	 * This is the rate at which samples are expected to be played back
	 * in a file or in real time.
	 * @return Float representing the sample rate. If this value is 0 or negative, 
	 * there is probably something wrong with the stream.
	 * @since 1.0.0
	 */
	public float getSampleRate();
	
	/**
	 * Get the number of bits required to represent a single audio sample
	 * from this stream.
	 * <br>All samples are returned as Java int values, so all bits above
	 * what is used by the stream should be ignored.
	 * <br>Higher (unused) bits SHOULD still match sign of sample so that
	 * filters and players can do many arithmetic operations without needing
	 * to check bit depth!
	 * @return Number of bits needed to represent PCM sample. Cannot exceed 32.
	 * @since 1.0.0
	 */
	public int getBitDepth();
	
	/**
	 * Get the number of audio channels this stream outputs. For example, 1 indicates
	 * a monophonic stream, 2 is stereo. Some players may limit the number of channels
	 * to 2, but for the most part, the framework is set up to theoretically have
	 * as many as desired.
	 * @return Number of channels output by stream (this should match length of array
	 * returned by nextSample()). If this value is 0, this stream should have no output.
	 * @since 1.0.0
	 */
	public int getChannelCount();
	
	/**
	 * Get the next audio sample from each channel in the stream.
	 * Samples are encoding PCM and formatted as signed Java ints, 
	 * though useful bitspace is specified by getBitDepth().
	 * @return Array of PCM samples (one for each channel) stored in signed ints.
	 * @throws InterruptedException If this method institutes blocking to wait for
	 * a background thread to obtain the next sample and an unexpected interrupt
	 * occurs during the block that isn't otherwise handled by the method.
	 * @since 1.0.0
	 */
	public int[] nextSample() throws InterruptedException;
	
	/**
	 * Close this stream, releasing any resources and shutting down any
	 * background threads it is using.
	 * <br>Although not every implementation of AudioSampleStream may utilize
	 * this method, it should be assumed that the stream has been rendered
	 * unusable after this method is called.
	 * @since 1.0.0
	 */
	public void close();
	
	/**
	 * Check whether this sample stream has any samples remaining to
	 * send to output. 
	 * @return True if this sample doesn't loop and all of its
	 * samples have been exhausted. False if playable samples remain.
	 */
	public boolean done();
}
