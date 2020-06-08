package waffleoRai_SoundSynth;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import waffleoRai_SoundSynth.soundformats.WAVWriter;

/*
 * UPDATES
 * 
 * 2020.03.19 | 1.0.0
 * 		Initial Documentation
 * 
 * 2020.04.06 | 1.1.0
 * 		Added done()
 * 2020.04.21 | 1.2.0
 * 		Implemented writeMixdownTo()
 * 2020.04.22 | 1.2.1
 * 		Oopsy doopsy, worker wasn't closing audio line before terminating :P
 * 2020.05.10 | 1.3.0
 * 		Added removeListener(Object o) and dispose()
 */

/**
 * A partial, generally applicable implementation of SynthPlayer that when 
 * connected to a SourceDataLine, uses a background thread to synthesize 
 * samples as fast as it can and forward them to the line (as far as the line can buffer).
 * <br>Samples can also be nabbed directly using nextSample(), but as nextSample() does not
 * advance the sequence clock, nothing will be synthesized without implementing a means of
 * inducing the tracks to send sequence commands.
 * This would only be an issue for code trying to induce synthesis from without; the
 * SequencePlayer's worker thread handles command issuing and timing.
 * <br>This player class notifies PlayerListeners of level, tick, and tempo changes.
 * (ChannelStateListener events are mostly dependent upon the implementation of PlayerTrack
 * used by the player). However, because sequence commands and audio data generation
 * can occur well before they are played back, noting the time coordinate of the event
 * relative to the current playback time coordinate is essential.
 * @author Blythe Hospelhorn
 * @version 1.3.0
 * @since May 10, 2020
 */
public abstract class SequencePlayer implements SynthPlayer{
	
	/*--- Instance Variables ---*/
	
	private SourceDataLine playback_line;
	private PlayerWorker worker;
	private volatile boolean exporting = false;
	
	protected SynthChannel[] channels;
	protected PlayerTrack[] tracks;
	
	private int ch_solo_vector;
	private int ch_mute_vector;
	private long tr_solo_vector;
	private long tr_mute_vector;
	
	private double ch_atten = 1.0; //Factor by which to attenuate individual channels
	private double master = 0.5; //Master volume
		
	private int tpqn; //Ticks per quarter note
	private int tempo; //us per qn
	private double tickrate; //Hz (ticks/s)
	private boolean tempo_flag; //Set when tempo has been changed
	
	private long tick;
	
	private boolean loopme; //Flag set when end loop event encountered
	private long looptick;
	private int loopcount; //Max loops. 0 means infinite
	private int myloops; //Loops done
	
	private long time_coord; //True sample #
	private int ctr_sampling;
	private int ctr_tick;

	protected List<PlayerListener> listeners;
	protected Map<Integer, List<ChannelStateListener>> ch_listeners;
	
	/*--- Common Functions ---*/
	
	/**
	 * Allocate array space for synth channels. This does not instantiate
	 * any synth channels.
	 * @param chcount Number of channels to allocate array space for.
	 * @since 1.0.0
	 */
	protected void allocateChannels(int chcount)
	{
		channels = new SynthChannel[chcount];
		
		//int ccount = getChannelCount();
		//lastMasterLevel = new int[ccount];
		//lastChLevels = new int[chcount][ccount];
	}
	
	/**
	 * Clamp a sample value between minimum and maximum values reflecting the
	 * output audio's bit depth.
	 * @param in Input sample
	 * @return Clamped sample
	 * @since 1.0.0
	 */
	protected abstract int saturate(int in); 
	
	/**
	 * Get the next sample from the master mixer and write it to the data line.
	 * This is left abstract in the SequencePlayer parent class because the data line
	 * takes samples as byte streams, not as samples.
	 * Therefore, the breaking down of these samples into bytes needs to match whatever
	 * AudioFormat was obtained in getOutputFormat().
	 * @param target DataLine to write sample data to.
	 * @throws InterruptedException If getting a sample from this player involves blocking
	 * and the block is interrupted unexpectedly.
	 * @since 1.0.0
	 */
	protected abstract void putNextSample(SourceDataLine target) throws InterruptedException;
	
	public AudioFormat getOutputFormat()
	{
		return new AudioFormat(getSampleRate(), getBitDepth(), getChannelCount(), true, false);
	}
	
	/**
	 * Whether or not a channel should be playing based upon its mute/solo
	 * status.
	 * @param mask Bitmask to apply to mute and solo vectors to check status.
	 * Mask is passed instead of channel index for a bit of speed.
	 * @return True if channel sample should be included in master mix given
	 * current state. False if not.
	 * @since 1.0.0
	 */
	protected boolean playChannel(int mask)
	{
		if((mask & ch_mute_vector) != 0) return false;
		if(ch_solo_vector == 0) return true; //No solo flags at all
		
		//Otherwise, only play if this channel is soloed
		return (mask & ch_solo_vector) != 0;
	}
	
	/*--- Getters ---*/
	
	public double getMasterAttenuation(){
		return this.master;
	}
	
	public int getSynthChannelCount(){
		if(channels == null) return 0;
		return channels.length;
	}
	
	public int getTrackCount(){
		if(tracks == null) return 0;
		return tracks.length;
	}
	
	public boolean channelMuted(int idx){
		if(idx < 0) return false;
		int mask = 1 << idx;
		return (ch_mute_vector & mask) != 0;
	}
	
	public boolean channelSoloed(int idx){
		if(idx < 0) return false;
		int mask = 1 << idx;
		return (ch_solo_vector & mask) != 0;
	}
	
	/**
	 * Get (a copy of) the Java int representing the channel mute vector
	 * as a series of bits.
	 * To check whether a channel is muted, see if the bit shifted (ch_idx) left
	 * from 0x1 is set.
	 * @return Player's current channel mute vector.
	 * @since 1.0.0
	 */
	public int getChannelMuteVector(){
		return ch_mute_vector;
	}
	
	/**
	 * Get (a copy of) the Java int representing the channel solo vector
	 * as a series of bits.
	 * To check whether a channel is soloed, see if the bit shifted (ch_idx) left
	 * from 0x1 is set.
	 * @return Player's current channel solo vector.
	 * @since 1.0.0
	 */
	public int getChannelSoloVector(){
		return ch_solo_vector;
	}
	
	public boolean trackMuted(int idx){
		if(idx < 0) return false;
		long mask = 1 << idx;
		return (tr_mute_vector & mask) != 0;
	}
	
	public boolean trackSoloed(int idx){
		if(idx < 0) return false;
		long mask = 1 << idx;
		return (tr_solo_vector & mask) != 0;
	}
	
	/**
	 * Get (a copy of) the Java long representing the track mute vector
	 * as a series of bits.
	 * To check whether a track is muted, see if the bit shifted (tr_idx) left
	 * from 0x1 is set.
	 * @return Player's current track mute vector.
	 * @since 1.0.0
	 */
	public long getTrackMuteVector(){
		return tr_mute_vector;
	}
	
	/**
	 * Get (a copy of) the Java long representing the track solo vector
	 * as a series of bits.
	 * To check whether a track is soloed, see if the bit shifted (tr_idx) left
	 * from 0x1 is set.
	 * @return Player's current track solo vector.
	 * @since 1.0.0
	 */
	public long getTrackSoloVector(){
		return tr_solo_vector;
	}
	
	/**
	 * Check whether any channels in this player are currently soloed.
	 * @return True if any channels soloed, false if not.
	 * @since 1.0.0
	 */
	public boolean anyChannelsSolo(){
		return ch_solo_vector != 0;
	}
	
	/**
	 * Check whether any tracks in this player are currently soloed.
	 * @return True if any tracks soloed, false if not.
	 * @since 1.0.0
	 */
	public boolean anyTracksSolo(){
		return tr_solo_vector != 0L;
	}
	
	public boolean isCapturable(){
		return true;
	}
	
	public boolean done(){
		return !(isRunning());
	}
	/*--- Setters ---*/
	
	public void setMasterAttenuation(double amp_ratio){
		master = amp_ratio;
	}
	
	public void setChannelMute(int ch_idx, boolean mute){
		int mask = 1 << ch_idx;
		if(mute)ch_mute_vector |= mask;
		else ch_mute_vector &= ~mask;
	}
	
	public void setChannelSolo(int ch_idx, boolean solo){
		int mask = 1 << ch_idx;
		if(solo)ch_solo_vector |= mask;
		else ch_solo_vector &= ~mask;
	}
	
	public void setTrackMute(int tr_idx, boolean mute){
		long mask = 1 << tr_idx;
		if(mute){tr_mute_vector |= mask; tracks[tr_idx].setMute(true);}
		else {
			tr_mute_vector &= ~mask; 
			if(anyTracksSolo()) tracks[tr_idx].setMute((mask & tr_solo_vector) == 0);
			else tracks[tr_idx].setMute(false);
		}
	}
	
	public void setTrackSolo(int tr_idx, boolean solo){
		long mask = 1 << tr_idx;
		if(solo){
			tr_solo_vector |= mask;
			tracks[tr_idx].setMute((mask & tr_mute_vector) != 0);
		}
		else{
			tr_solo_vector &= ~mask;
			if(anyTracksSolo()) tracks[tr_idx].setMute(true);
			else tracks[tr_idx].setMute((mask & tr_mute_vector) != 0);
		}
	}
	
	/**
	 * Set or unset flag that tells the player whether or not
	 * to sequence loop on the next tick.
	 * @param b True to set (loop), false to unset (don't loop).
	 * @since 1.0.0
	 */
	protected void setLoopFlag(boolean b){
		loopme = b;
	}
	
	/**
	 * Set the sequence loop point in ticks.
	 * @param tick Target loop point
	 * @since 1.0.0
	 */
	protected void setLoopTick(long tick){
		looptick = tick;
	}
	
	/**
	 * Set the maximum number of times sequence should loop.
	 * -1 is interpreted as a one-shot, and 0 is interpreted as infinite.
	 * @param c Number of loops
	 * @since 1.0.0
	 */
	protected void setLoopCount(int c){
		loopcount = c;
	}
	
	/**
	 * Set the sequence resolution in ticks per quarter note.
	 * @param ticks_per_qn Sequence resolution (TPQN)
	 * @since 1.0.0
	 */
	protected void setTickResolution(int ticks_per_qn){
		this.tpqn = ticks_per_qn;
		updateTickRate();
	}
	
	/**
	 * Increment the number of times the sequence has looped.
	 * This is for direct use of subclasses in case they want to bypass
	 * the loop flag.
	 * @since 1.2.0
	 */
	protected void incrementLoopNumber(){
		myloops++;
	}
	
	/*--- Listeners ---*/
	
	public void addListener(PlayerListener l)
	{
		if(listeners == null) listeners = new LinkedList<PlayerListener>();
		listeners.add(l);
	}
	
	public void removeListener(Object o){
		listeners.remove(o);
	}
	
	public void clearListeners()
	{
		listeners.clear();
	}
	
	private void sendLevelToListeners(int[] samps)
	{
		if(listeners == null) return;
		for(PlayerListener l : listeners) l.sendLevel(samps, time_coord);
	}
	
	private void sendTempoToListeners(int us)
	{
		if(listeners == null) return;
		for(PlayerListener l : listeners) l.onTempoChange(us, time_coord);
	}
	
	private void sendTickToListeners()
	{
		if(listeners == null) return;
		for(PlayerListener l : listeners) l.onTick(tick, time_coord);
	}
	
	/**
	 * Forward the specified audio level of a synth channel to all
	 * listeners registered for that channel.
	 * @param ch Channel index.
	 * @param samps Sample levels to forward.
	 * @since 1.0.0
	 */
	protected void sendChannelLevelToListeners(int ch, int[] samps){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch);
		if(ch_listeners == null) return;
		for(ChannelStateListener l : list) l.sendLevel(samps, time_coord);
	}
	
	/**
	 * Forward a program change event from a synth channel to all
	 * listeners registered for that channel.
	 * @param ch Channel index.
	 * @param bank Bank index from program change
	 * @param program Program index from program change
	 * @since 1.0.0
	 */
	protected void sendProgramChangeToListeners(int ch, int bank, int program){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch);
		if(ch_listeners == null) return;
		for(ChannelStateListener l : list) l.setProgram(bank, program, time_coord);
	}
	
	/**
	 * Forward a note on event from a synth channel to all 
	 * listeners registered to that channel.
	 * @param ch Channel index.
	 * @param key MIDI pitch value of note
	 * @since 1.0.0
	 */
	protected void sendNoteOnToListeners(int ch, byte key){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch);
		if(ch_listeners == null) return;
		for(ChannelStateListener l : list) l.onNoteOn(key, time_coord);
	}
	
	/**
	 * Forward a note off event from a synth channel to all 
	 * listeners registered to that channel.
	 * @param ch Channel index.
	 * @param key MIDI pitch value of note
	 * @since 1.0.0
	 */
	protected void sendNoteOffToListeners(int ch, byte key){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch);
		if(ch_listeners == null) return;
		for(ChannelStateListener l : list) l.onNoteOff(key, time_coord);
	}
	
	/**
	 * Forward a pitch wheel change event from a synth channel to all
	 * listeners registered to that channel.
	 * @param ch Channel index.
	 * @param val New value of pitch wheel (scaled to signed 16-bit centered around 0)
	 * @since 1.0.0
	 */
	protected void sendPitchWheelToListeners(int ch, short val){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch);
		if(ch_listeners == null) return;
		for(ChannelStateListener l : list) l.onPitchWheelSet(val, time_coord);
	}
	
	/**
	 * Forward a mod wheel change event from a synth channel to all
	 * listeners registered to that channel.
	 * @param ch Channel index.
	 * @param val New value of mod wheel (scaled to signed 16-bit centered around 0)
	 * @since 1.0.0
	 */
	protected void sendModWheelToListeners(int ch, short val){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch);
		if(ch_listeners == null) return;
		for(ChannelStateListener l : list) l.onModWheelSet(val, time_coord);
	}
	
	/**
	 * Forward a pan change event from a synth channel to all
	 * listeners registered to that channel.
	 * @param ch Channel index
	 * @param val 8-bit value (centered around 0x40) representing new pan value.
	 * @since 1.0.0
	 */
	protected void sendPanToListeners(int ch, byte val){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch);
		if(ch_listeners == null) return;
		for(ChannelStateListener l : list) l.onPanSet(val, time_coord);
	}
	
	/**
	 * Forward a volume change event from a synth channel to all
	 * listeners registered to that channel.
	 * @param ch Channel index
	 * @param val New channel master volume represented as an amplitude ratio
	 * from 0.0 to 1.0.
	 * @since 1.0.0
	 */
	protected void sendVolumeToListeners(int ch, double val){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch);
		if(ch_listeners == null) return;
		for(ChannelStateListener l : list) l.onVolumeSet(val, time_coord);
	}
	
	/**
	 * Notify listeners that the sequence playback has ended
	 * (end of sequence was reached) and playback has auto-terminated.
	 * @since 1.2.1
	 */
	protected void notifyListenersOfSeqEnd(){
		if(listeners == null) return;
		for(PlayerListener l : listeners) l.onSequenceEnd();
	}
	
	public void addChannelListener(int ch_idx, ChannelStateListener l){
		if(ch_listeners == null) ch_listeners = new HashMap<Integer, List<ChannelStateListener>>();
		List<ChannelStateListener> list = ch_listeners.get(ch_idx);
		if(list == null)
		{
			list = new LinkedList<ChannelStateListener>();
			ch_listeners.put(ch_idx, list);
		}
		list.add(l);
	}
	
	public void removeChannelListener(int ch_idx, Object l){
		if(ch_listeners == null) return;
		List<ChannelStateListener> list = ch_listeners.get(ch_idx);
		if(list == null) return;
		list.remove(l);
	}
	
	public void clearChannelListeners(){
		if(ch_listeners == null) return;
		ch_listeners.clear();
	}

	/*--- Control ---*/
	
	private void updateTickRate()
	{
		tickrate = (double)1000000/((double)tempo/(double)tpqn);
		//System.err.println("Audio sample rate: " + getSampleRate() + " hz");
		//System.err.println("Ticks per quarter note: " + tpqn);
		//System.err.println("Microseconds per quarter note: " + tempo + " us");
		//System.err.println("Tick rate: " + tickrate + " hz");
		//System.err.println("Tempo: " + (tickrate * (60.0/(double)tpqn)) + " bpm");
	}
	
	/**
	 * Set the sequence tempo of the player.
	 * @param microsPerQN New tempo in microseconds per quarter note (MIDI standard).
	 * @since 1.0.0
	 */
	public void setTempo(int microsPerQN)
	{
		tempo_flag = true;
		tempo = microsPerQN;
		updateTickRate();
		sendTempoToListeners(microsPerQN);
	}
	
	/**
	 * Force this player to sequence loop. This method
	 * is automatically called when the loopflag is set
	 * while the player is running.
	 * @since 1.0.0
	 */
	public void loopMe()
	{
		loopme = false;
		tick = looptick;
		myloops++;
		for(PlayerTrack t : tracks){
			if(t!= null) t.resetTo(looptick, true);
		}
	}
	
	public void rewind()
	{
		time_coord = 0;
		tick = 0;
		myloops = 0;
		ctr_sampling = 0;
		ctr_tick = 0;
		for(int i = 0; i < tracks.length; i++){if(tracks[i] != null) tracks[i].resetTo(0, false);}
		//for(PlayerTrack t : tracks)t.resetTo(0);
	}
	
	public int[] nextSample() throws InterruptedException
	{
		//System.err.println("get sample cycle - start");
		
		//Mixdown sample
		int ccount = getChannelCount();
		double[] sum = new double[ccount];
		
		int chmask = 1;
		for(int i = 0; i < channels.length; i++)
		{
			//System.err.println("get sample cycle - channel " + i + " start");
			//Determine if channel muted
			SynthChannel ch = channels[i];
			int[] chsamps = ch.nextSample();
			//System.err.println("get sample cycle - channel " + i + " sample got");
			if(!playChannel(chmask))
			{
				//Don't play this channel, (but save 0 samples)
				//lastChLevels[i] = new int[ccount];
			}
			else
			{
				//SynthChannel ch = channels[i];
				//int[] chsamps = ch.nextSample();
				//lastChLevels[i] = chsamps;
				for(int j = 0; j < ccount; j++) {
					sum[j] += chsamps[j] * ch_atten;
					//if(i == 0 && chsamps[j] != 0) System.err.println(chsamps[j]);
				}
			}	
			sendChannelLevelToListeners(i, chsamps);
			chmask = chmask << 1;
			//System.err.println("get sample cycle - channel " + i + " end");
		}
		
		//System.err.println("get sample cycle - master adjust");
		int[] out = new int[ccount];
		for(int j = 0; j < ccount; j++) out[j] = saturate((int)Math.round(sum[j] * master));
		//lastMasterLevel = out;
		
		//System.err.println("get sample cycle - send to listeners");
		sendLevelToListeners(out);
		time_coord++;
		//if(out[0] != 0) System.err.println("Output: " + out[0] + " | " + out[1]);
		
		//System.err.println("get sample cycle - end");
		return out;
	}
	
	public void close(){
		dispose();
	}
	
	public void dispose(){
		//Stop playback
		//Clear all channels and tracks
		stop();
		
		//If worker is not null, wait for it to stop.
		if(worker != null){
			while(worker.isRunning()){
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					System.err.println("SequencePlayer.dispose || Unexpected interrupt occurred while waiting for worker to terminate!");
					e.printStackTrace();
					return;
				}
			}
		}
		
		worker = null;
		playback_line = null;
		if(listeners != null) listeners.clear();
		if(ch_listeners != null){
			for(List<ChannelStateListener> list : ch_listeners.values()){
				if(list != null) list.clear();
			}
			ch_listeners.clear();	
		}
		
		if(channels != null){
			for(int i = 0; i < channels.length; i++){
				if(channels[i] != null) channels[i].clear();
				channels[i] = null;
			}
			channels = null;
		}
		
		if(tracks != null){
			for(int i = 0; i < tracks.length; i++){
				if(tracks[i] != null) tracks[i].clearPlaybackResources();
				tracks[i] = null;
			}
			tracks = null;
		}
	}
	
	/*--- Realtime Playback ---*/
	
	/**
	 * The background worker for a SequencePlayer.
	 * Handles timing of sequence commands and audio sample
	 * retrieval. Forwards audio samples to output line for
	 * as much as the line's buffer can hold.
	 * @author Blythe Hospelhorn
	 * @version 1.0.0
	 * @since SequencePlayer 1.0.0
	 *
	 */
	private class PlayerWorker implements Runnable
	{
		private Boolean locker;
		
		private volatile boolean pause_flag;
		private volatile boolean tflag;
		
		private volatile boolean running;
		
		/**
		 * Construct a new PlayerWorker object. Because this
		 * is a Runnable, constructing it alone does not run it.
		 */
		public PlayerWorker()
		{
			locker = new Boolean(false);
		}
		
		@Override
		public void run() 
		{
			//System.err.println("Player worker starting!");
			running = true;
			try{playback_line.open();}
			catch(LineUnavailableException ex)
			{
				ex.printStackTrace();
				end();
				notifyListenersOfSeqEnd();
				//System.err.println("Player worker returning (cp 1)");
				return;
			}
			
			playback_line.start();
			
			double hzratio = (double)getSampleRate()/tickrate;
			int nexttick = 0;
			//System.err.println("sample rate = " + getSampleRate());
			//System.err.println("tickrate = " + tickrate);
			//System.err.println("hzratio = " + hzratio);
			//System.err.println("nexttick = " + nexttick);
			
			boolean seqend = false;
			while(!seqend && !tflag)
			{
				//System.err.println("player cycle - start");
				if(pause_flag)
				{
					try {synchronized(locker){locker.wait();}} 
					catch (InterruptedException e){continue;}
				}
				
				//System.err.println("player cycle - loopcheck");
				if(loopcount > 0)
				{
					if(myloops >= loopcount) break;
				}
				try{
					//System.err.println("player cycle - pull sample");
					putNextSample(playback_line);
					//System.err.println("sample");
					//System.err.println("nexttick = " + nexttick);
					//System.err.println("player cycle - tick check");
					if(ctr_sampling++ >= nexttick)
					{
						//Do operations for this tick
						//System.err.println("tick");
						for(int i = 0; i < tracks.length; i++){if(tracks[i] != null) tracks[i].onTick(tick);}
						//for(PlayerTrack t : tracks) t.onTick(tick);
						sendTickToListeners();
						tick++;
						
						//Check flags set by MIDI operations...
						//(Track end, tempo change, loop end)
						for(int i = 0; i < tracks.length; i++){if(tracks[i] != null) {seqend = (seqend && tracks[i].trackEnd());};}
						//for(PlayerTrack t : tracks){seqend = (seqend && t.trackEnd());};
						if(seqend) break;
						if(loopme)loopMe();
						if(tempo_flag)
						{
							hzratio = (double)getSampleRate()/tickrate;
							ctr_sampling = 0;
							ctr_tick = 0;
							tempo_flag = false;
							nexttick = 0;
							//System.err.println("sample rate = " + getSampleRate());
							//System.err.println("tickrate = " + tickrate);
							//System.err.println("hzratio = " + hzratio);
						}
						else
						{
							//Find sampling coordinate of next tick
							double nextraw = hzratio * (++ctr_tick);
							nexttick = (int)Math.floor(nextraw);
							if(nextraw == nexttick)
							{
								//Reset to 0
								ctr_sampling = 0;
								ctr_tick = 0;
							}
							//System.err.println("sample rate = " + getSampleRate());
							//System.err.println("tickrate = " + tickrate);
							//System.err.println("hzratio = " + hzratio);
							//System.err.println("nexttick = " + nexttick);
						}
					}
				}
				catch(InterruptedException ex)
				{
					System.err.println("Unexpected interrupt occurred! Stopping playback!");
					ex.printStackTrace();
					end();
					notifyListenersOfSeqEnd();
					//System.err.println("Player worker returning (cp 2)");
					return;
				}
				//System.err.println("player cycle - end");
			}
			
			//System.err.println("Player worker-- terminate command received!");
			for(SynthChannel ch : channels) ch.allNotesOff();
			boolean sremain = true;
			while(sremain)
			{
				try{
					//Copy a handful of samples
					for(int i = 0; i < 128; i++) putNextSample(playback_line);
				}
				catch(InterruptedException ex)
				{
					System.err.println("Unexpected interrupt occurred! Stopping playback!");
					ex.printStackTrace();
					end(); 
					notifyListenersOfSeqEnd();
					//System.err.println("Player worker returning (cp 3)");
					return;
				}
				
				//Check to see if channels are clear
				sremain = false;
				for(SynthChannel ch : channels)
				{
					if(ch.countActiveVoices() > 0) {sremain = true; break;}
				}
			}
			
			end();
			if(seqend)notifyListenersOfSeqEnd();
			//System.err.println("Player worker returning (cp 4)");
		}
		
		private void end()
		{
			playback_line.stop();
			playback_line.close();
			running = false;
		}
		
		/**
		 * Set the termination flag on this worker to tell it
		 * to exit run() on its next cycle. This should effectively
		 * kill its wrapping thread.
		 */
		public synchronized void stop()
		{
			tflag = true;
		}
		
		/**
		 * Pause synthesis and audio output. This does not terminate the
		 * worker, it only forces it to block until the pause flag is unset.
		 */
		public synchronized void pause()
		{
			pause_flag = true;
		}
		
		/**
		 * Unset the pause flag and allow the worker to continue synthesis
		 * and output.
		 */
		public synchronized void unpause()
		{
			pause_flag = false;
			synchronized(locker){locker.notifyAll();}
		}
		
		/**
		 * Check whether this worker is running. The running flag is set
		 * when the run() method is first called, and unset when end() is called (ie.
		 * run() is returning)
		 * @return True if this worker is active, false if not.
		 */
		public boolean isRunning()
		{
			return running;
		}
		
		/**
		 * Check whether this worker is paused or has a set pause flag.
		 * @return True if paused, false if not.
		 */
		public boolean isPaused()
		{
			return pause_flag;
		}
		
	}
	
	public void startAsyncPlaybackToDefaultOutputDevice() throws LineUnavailableException
	{
		AudioFormat fmt = getOutputFormat();
		SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
		startAsyncStreamTo(line);
	}
	
	public void startAsyncStreamTo(SourceDataLine line)
	{
		rewind();
		playback_line = line;
		
		worker = new PlayerWorker();
		Thread t = new Thread(worker);
		Random r = new Random();
		t.setName("SeqpPlayer_WorkerThread_0x" + Integer.toHexString(r.nextInt()));
		t.start();
	}
	
	public void stop()
	{
		if(worker != null && worker.isRunning()) worker.stop();
	}
	
	public void pause()
	{
		if(worker != null && worker.isRunning()) worker.pause();
	}
	
	public void unpause()
	{
		if(worker != null && worker.isRunning()) worker.unpause();
	}
	
	public boolean isPaused(){
		if(worker == null) return false;
		return worker.isPaused();
	}
	
	/**
	 * Directly retrieve the SourceDataLine that is currently being used for
	 * audio playback, if there is one.
	 * @return The line in current use for playback.
	 */
	public SourceDataLine getCurrentPlaybackLine()
	{
		return playback_line;
	}
	
	public long getPlaybackPosition()
	{
		if(playback_line == null) return 0;
		return playback_line.getLongFramePosition();
	}
	
	/**
	 * Check whether this player is currently synthesizing and playing back.
	 * @return True if player is running, false if not.
	 * @since 1.0.0
	 */
	public boolean isRunning(){
		if(exporting) return true;
		if(worker == null) return false;
		return (worker.isRunning());
	}
	
	/*--- Write ---*/
	
	public void writeMixdownTo(String path, int loops) throws IOException
	{
		//Uses current state of player (master vol and muted channels)
		
		//Check to make sure it is not running.
		if(isRunning()) throw new IllegalStateException("Player cannot export while playing!");
		
		//Mark as active so another thread won't try to run it
		synchronized(this){exporting = true;}
		rewind();
		
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		WAVWriter writer = new WAVWriter(this, bos);
		
		double hzratio = (double)getSampleRate()/tickrate;
		int nexttick = 0;
		
		boolean seqend = false;
		while(!seqend && (myloops < loops))
		{	
			if(loopcount > 0){
				if(myloops >= loopcount) break;
			}
			try{
				//putNextSample(playback_line);
				writer.write(1);

				if(ctr_sampling++ >= nexttick)
				{
					//Do operations for this tick
					for(int i = 0; i < tracks.length; i++){if(tracks[i] != null) tracks[i].onTick(tick);}
					tick++;
					
					//Check flags set by MIDI operations...
					//(Track end, tempo change, loop end)
					for(int i = 0; i < tracks.length; i++){if(tracks[i] != null) {seqend = (seqend && tracks[i].trackEnd());};}
					//for(PlayerTrack t : tracks){seqend = (seqend && t.trackEnd());};
					if(seqend) break;
					if(loopme)loopMe();
					if(tempo_flag)
					{
						hzratio = (double)getSampleRate()/tickrate;
						ctr_sampling = 0;
						ctr_tick = 0;
						tempo_flag = false;
					}
					else
					{
						//Find sampling coordinate of next tick
						double nextraw = hzratio * (++ctr_tick);
						nexttick = (int)Math.floor(nextraw);
						if(nextraw == nexttick)
						{
							//Reset to 0
							ctr_sampling = 0;
							ctr_tick = 0;
						}
					}
				}
			}
			catch(InterruptedException ex)
			{
				System.err.println("Unexpected interrupt occurred! Stopping synthesis!");
				ex.printStackTrace();
				bos.close();
				synchronized(this){exporting = false;}
				return;
			}
		}
		
		for(SynthChannel ch : channels) ch.allNotesOff();
		boolean sremain = true;
		while(sremain)
		{
			try{
				//for (int i = 0; i < 16; i++) putNextSample(playback_line);
				writer.write(1);
			}
			catch(InterruptedException ex)
			{
				System.err.println("Unexpected interrupt occurred! Stopping playback!");
				ex.printStackTrace();
				bos.close();
				synchronized(this){exporting = false;}
				return;
			}
			
			sremain = false;
			for(SynthChannel ch : channels){
				if(ch.countActiveVoices() > 0) {sremain = true; break;}
			}
		}
		
		writer.complete();
		bos.close();
		synchronized(this){exporting = false;}
	}
	
	public void writeChannelTo(String pathPrefix, int loops, int ch) throws IOException
	{
		//TODO
	}
	
	public void writeChannelsTo(String pathPrefix, int loops) throws IOException
	{
		//TODO
	}
	
}
