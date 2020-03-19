package waffleoRai_SoundSynth;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public abstract class SystemSeqPlayer implements SynthPlayer{
	
	/*--- Instance Variables ---*/
	
	private String seqname;
	
	protected Synthesizer synth;
	protected MidiChannel[] channels;
	protected PlayerTrack[] tracks;
	
	private int ch_solo_vector;
	private int ch_mute_vector;
	private long tr_solo_vector;
	private long tr_mute_vector;
	
	private double master;
	
	private int tpqn;
	private int res_scale; //If resolution is too high, need to scale down
	private int tempo; //us per beat
	private int us_per_tick;
	
	private long tick;
	
	private volatile boolean running;
	private boolean tempoflag;
	private boolean loopme; //Flag set when end loop event encountered
	
	private long looptick; //Tick to jump to upon loop
	private int loopcount; //Max loops. 0 means infinite
	private int myloops; //Number of times this player has looped
	
	private boolean pauseflag;
	private long time_coord;
	private Timer timer;
	
	protected List<PlayerListener> listeners;
	
	/*--- Common Functions ---*/
	
	protected void setupSynth() throws MidiUnavailableException
	{
		synth = MidiSystem.getSynthesizer();
		channels = synth.getChannels();
	}
	
	public AudioFormat getOutputFormat()
	{
		return new AudioFormat(getSampleRate(), getBitDepth(), getChannelCount(), true, false);
	}
	
	protected boolean playChannel(int mask)
	{
		if((mask & ch_mute_vector) != 0) return false;
		if(ch_solo_vector == 0) return true; //No solo flags at all
		
		//Otherwise, only play if this channel is soloed
		return (mask & ch_solo_vector) != 0;
	}
	
	/*--- Getters ---*/
	
	protected int getResolutionScalingFactor(){
		return res_scale;
	}
	
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
	
	public int getChannelMuteVector(){
		return ch_mute_vector;
	}
	
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
	
	public long getTrackMuteVector(){
		return tr_mute_vector;
	}
	
	public long getTrackSoloVector(){
		return tr_solo_vector;
	}
	
	public boolean anyChannelsSolo(){
		return ch_solo_vector != 0;
	}
	
	public boolean anyTracksSolo(){
		return tr_solo_vector != 0L;
	}
	
	public String getBankName(){
		return "System Default General MIDI";
	}
	
	public String getSequenceName() {
		return seqname;
	}
	
	public float getSampleRate() {
		return 0;
	}

	public int getBitDepth() {
		return 16;
	}
	
	public int getChannelCount() {
		return 2;
	}
	
	public boolean isCapturable(){
		return false;
	}
	
	/*--- Setters ---*/

	public void setMasterAttenuation(double amp_ratio){
		master = amp_ratio;
	}
	
	public void setChannelMute(int ch_idx, boolean mute){
		int mask = 1 << ch_idx;
		if(mute)ch_mute_vector |= mask;
		else ch_mute_vector &= ~mask;
		channels[ch_idx].setMute(mute);
	}
	
	public void setChannelSolo(int ch_idx, boolean solo){
		int mask = 1 << ch_idx;
		if(solo)ch_solo_vector |= mask;
		else ch_solo_vector &= ~mask;
		channels[ch_idx].setSolo(solo);
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
	
	protected void setLoopFlag(boolean b){
		loopme = b;
	}
	
	protected void setLoopTick(long tick){
		looptick = tick;
	}
	
	protected void setLoopCount(int c){
		loopcount = c;
	}
	
	protected void setTickResolution(int ticks_per_qn){
		this.tpqn = ticks_per_qn;
		res_scale = tpqn/48;
	}
	
	public void setSequenceName(String name){
		seqname = name;
	}
	
	/*--- Listeners ---*/
	
	public void addListener(PlayerListener l)
	{
		if(listeners == null) listeners = new LinkedList<PlayerListener>();
		listeners.add(l);
	}
	
	public void clearListeners()
	{
		listeners.clear();
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
	
	public void addChannelListener(int ch_idx, ChannelStateListener l){
		//TODO
	}
	
	public void removeChannelListener(int ch_idx, Object l){
		//TODO
	}
	
	public void clearChannelListeners(){
		//TODO
	}
	
	/*--- Timing ---*/
	
	public void setTempo(int us)
	{
		tempoflag = true;
		tempo = us;
		//ticks_per_tick = 1;
		us_per_tick = tempo/(tpqn/res_scale);
		//System.err.println("mintempo = " + mintempo);
		
		/*while(us_per_tick < 1000)
		{
			us_per_tick = (tempo/tpqn) * ++ticks_per_tick;
		}*/
		sendTempoToListeners(us);
	}
	
	private TimerTask generateTimerTask()
	{
		TimerTask task = new TimerTask(){

			@Override
			public void run() {
				
					boolean allend = true;

					//int d = 0;
					try{
						for(PlayerTrack t : tracks){
							t.onTick(tick);
							allend = allend && t.trackEnd();
						//System.err.println("track " + d++ + " end: " + t.trackend);
						}	
					}
					catch(InterruptedException ex)
					{
						System.err.println("Unexpected interrupt!");
						ex.printStackTrace();
						return;
					}

				
					//Handle flags
					if(allend && !loopme)
					{
						running = false;
						this.cancel();
						stop();
						return;
					}
					if(tempoflag)
					{
						//System.err.println("us per beat: " + tempo);
						//System.err.println("tpqn: " + tpqn);
						//System.err.println("us per tick: " + us_per_tick);
						//System.err.println("ticks per tick: " + ticks_per_tick);
						tempoflag = false;
						TimerTask ntask = generateTimerTask();
						//int t = us_per_tick/1000;
						int t = (int)Math.round((double)us_per_tick/1000.0);
						//timer.schedule(ntask, t, t);
						timer.scheduleAtFixedRate(ntask, t, t);
						this.cancel();
					}
					if(loopme){loop(); return;}
					sendTickToListeners();
					tick++;	
					time_coord++;
			}};
		
		return task;
	}
	
	/*--- Control ---*/
	
	private void loop()
	{
		if(loopcount < 0) {stop(); return;}
		loopme = false;
		tick = looptick;
		myloops++;
		if(loopcount > 0)
		{
			if(myloops >= loopcount)
			{
				stop();
				return;
			}
		}
		for(PlayerTrack t : tracks)t.resetTo(looptick);
	}
	
	public void start() throws MidiUnavailableException
	{
		running = true;
		synth.open();
		int time = (int)Math.round((double)us_per_tick/1000.0);
		
		TimerTask inittask = generateTimerTask();
		timer = new Timer();
		//timer.schedule(inittask, time, time);
		timer.scheduleAtFixedRate(inittask, time, time);
	}
	
	public void stop()
	{
		pauseflag = false;
		timer.cancel();
		running = false;
		synth.close();
	}
	
	public boolean isPlaying(){
		return running;
	}
	
	public void rewind() {
		pauseflag = false;
		if(running)stop();
		tick = 0;
		try {setupSynth();} 
		catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
		time_coord = 0;
	}

	public void startAsyncPlaybackToDefaultOutputDevice() throws LineUnavailableException, MidiUnavailableException {
		start();
	}

	public void startAsyncStreamTo(SourceDataLine line) {
		throw new UnsupportedOperationException("Audio capture from default MIDI device unavailable at this time.");
	}

	public void pause() {
		pauseflag = true;
		timer.cancel();
		for(MidiChannel ch : channels)ch.allNotesOff();
	}

	public void unpause() {
		pauseflag = false;
		int time = (int)Math.round((double)us_per_tick/1000.0);
		TimerTask inittask = generateTimerTask();
		timer.scheduleAtFixedRate(inittask, time, time);
	}

	public boolean isPaused() {
		return pauseflag;
	}

	public long getPlaybackPosition() {
		return time_coord;
	}
	
	public int[] nextSample() throws InterruptedException {
		throw new UnsupportedOperationException("Audio capture from default MIDI device unavailable at this time.");
	}

	public void close() {
		stop();
		timer = null;
	}
	
	/*--- Writers ---*/
	
	public void writeMixdownTo(String path, int loops) throws IOException {
		throw new UnsupportedOperationException("Audio capture from default MIDI device unavailable at this time.");
	}

	public void writeChannelTo(String pathPrefix, int loops, int ch) throws IOException {
		throw new UnsupportedOperationException("Audio capture from default MIDI device unavailable at this time.");
	}

	public void writeChannelsTo(String pathPrefix, int loops) throws IOException {
		throw new UnsupportedOperationException("Audio capture from default MIDI device unavailable at this time.");
	}
	
}
