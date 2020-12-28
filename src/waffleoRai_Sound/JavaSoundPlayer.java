package waffleoRai_Sound;

import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import waffleoRai_Compression.ArrayWindow;
import waffleoRai_SoundSynth.AudioSampleStream;

/*
 * UPDATES
 * 
 * 	2020/12/19 | 1.0.0
 * 		Creation/Initial doc
 * 
 * 	2020/12/20 | 1.1.0
 * 		Added ability to manually release samples to the line (for A/V syncing)
 * 
 */

/**
 * Basic Java sound playback ABC for <code>AudioSampleStream</code>
 * to system playback. Uses Java sound API, but may be less efficient
 * than using a native library.
 * @author Blythe Hospelhorn
 * @version 1.1.0
 * @since December 20, 2020
 */
public abstract class JavaSoundPlayer implements IAudioPlayer{

	/*--- Constants ---*/
	
	public static final int DEFO_MILLIS_BUFF = 100; //Buffers every 1/10 sec by default.
	public static final int DEFO_CYCLES_PREBUFF = 5; //Prebuff cycles to run on open()
	
	/*--- Instance Variables ---*/
	
	protected AudioSampleStream source;
	protected boolean closeOnEnd;
	protected boolean syncMode; //For manual sample release to line.
	
	protected volatile boolean closeMe;
	protected boolean closedFlag;
	protected boolean pauseFlag;
	
	//protected long loc_pb;
	protected long loc_buf;
	
	protected SourceDataLine line;
	
	//Standard mode
	protected Timer timer;
	protected int[] frames_per_cycle; //How many frames to pull each buffer cycle
	protected int buff_cycle; //Index of buffer cycle
	protected int millis_per_cycle = DEFO_MILLIS_BUFF;
	protected int prebuff_cycles = DEFO_CYCLES_PREBUFF;
	
	//Manual mode
	protected ArrayWindow byteBuffer; //Buffer thread instead synths and serializes to this.
	
	//protected int millis_per_poscheck = 10;
	//protected int samples_per_poscheck;
	
	/*--- Initialization ---*/
	
	protected JavaSoundPlayer(AudioSampleStream input){
		source = input;
		calculateCycleFrames();
	}
	
	protected void calculateCycleFrames(){
		//Uses sample rate and millis/cycle
		
		//How many times per second is buff called?
		//(If uneven how many seconds needed for even div?)
		int fullmillis = 500;
		while(fullmillis % millis_per_cycle != 0){
			fullmillis += 500;
			if(fullmillis > 10000) break;
		}
		int cycles = fullmillis/millis_per_cycle;
		frames_per_cycle = new int[cycles];
		
		//Now, check the sample rate...
		int sr = (int)source.getSampleRate();
		int total = (int)Math.round((double)sr * (double)(fullmillis/1000));
		int div = total/cycles;
		int mod = total%cycles;
		
		for(int i = 0; i < cycles; i++) frames_per_cycle[i] = div;
		frames_per_cycle[cycles-1] += mod;
	}
	
	/*--- Getters ---*/
	
	public AudioSampleStream getStream(){return source;}
	public boolean closeOnStreamEnd(){return closeOnEnd;}
	public boolean isClosed(){return closedFlag;}
	public boolean rewindable(){return false;}
	
	public long getPlaybackLocation(){
		if(line == null) return -1;
		return line.getLongFramePosition();
	}
	
	public long getBufferLocation(){return loc_buf;}
	
	public boolean isRunning(){
		return (timer != null && line != null && line.isRunning());
	}
	
	/*--- Setters ---*/
	
	public void setSyncedMode(boolean b){
		if(isRunning()) return;
		syncMode = b;
		if(syncMode){
			//Allocate byte buffer (5 seconds worth)
			int sz = (int)source.getSampleRate() * 5;
			sz *= source.getChannelCount();
			sz *= bytesPerSample();
			byteBuffer = new ArrayWindow(sz);
		}
		else{
			if(byteBuffer != null) {
				//byteBuffer.clear();
				byteBuffer = null;
			}
		}
	}
	
	public void setPrebuffCycles(int i){prebuff_cycles = i;}
	
	/*--- Playback ---*/
	
	protected abstract int bytesPerSample();
	protected abstract byte[] frame2Bytes(int[] samples);
	protected abstract AudioFormat getOutputFormat();
	
	protected void generateOutputLine() throws LineUnavailableException{
		AudioFormat fmt = getOutputFormat();
		line = AudioSystem.getSourceDataLine(fmt);
	}
	
	protected void onBufferCycle(){
		if(closedFlag) return;
		if(closeMe){
			//Exhaust buffer, then quit.
			if(line.getLongFramePosition() >= loc_buf){
				close();
				return;
			}
		}
		if(buff_cycle >= frames_per_cycle.length) buff_cycle = 0;
		int frames = frames_per_cycle[buff_cycle++];
		if(syncMode){
			//Check to see if enough room in buffer.
			int needed = frames * source.getChannelCount() * bytesPerSample();
			if(byteBuffer.emptySpace() < needed){
				buff_cycle--;
				if(buff_cycle < 0) buff_cycle = frames_per_cycle.length - 1;
				return;
			}
		}
		
		try{
			for(int f = 0; f < frames; f++){
				loc_buf++;
				if(closeOnEnd && source.done()){
					closeWhenFinished();
					return;
				}
			
				int[] samps = source.nextSample();
				byte[] dat = frame2Bytes(samps);
				if(!syncMode) line.write(dat, 0, dat.length);
				else byteBuffer.put(dat);
			}
		}
		catch(Exception x){
			x.printStackTrace();
			return;
		}
	}
	
	protected void closeWhenFinished(){
		synchronized(this){closeMe = true;}
	}
	
	public void startTimer(){
		if(timer != null)timer.cancel();
		
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask(){

			public void run() {
				onBufferCycle();
			}
			
		}, 0, millis_per_cycle);
	}
	
	/*--- Control ---*/
	
	public int releaseToLine(int frames){
		if(!syncMode) return 0;
		
		int fsize = source.getChannelCount() * bytesPerSample();
		int ct = 0;
		for(int f = 0; f < frames; f++){
			if(byteBuffer.getSize() < fsize) return ct;
			byte[] frame = new byte[fsize];
			for(int i = 0; i < fsize; i++){
				frame[i] = byteBuffer.pop();
			}
			line.write(frame, 0, fsize);
			ct++;
		}
		
		return ct;
	}
	
	public void play() throws LineUnavailableException{
		if(closedFlag || isRunning()) return;
		if(!pauseFlag) open();
		startTimer();
		line.start();
		pauseFlag = false;
	}
	
	public void pause(){
		if(closedFlag || !isRunning()) return;
		line.stop();
		timer.cancel();
		pauseFlag = true;
		
		timer = null;
	}
	
	public void stop(){
		close();
	}
	
	public void open()throws LineUnavailableException{
		if(closedFlag || isRunning()) return;
		if(line != null) return;
		
		generateOutputLine();
		line.open();
		
		for(int i = 0; i < prebuff_cycles; i++) onBufferCycle();
		
	}
	
	public void close(){
		if(closedFlag) return;
		closedFlag = true;
		if(line != null) line.stop();
		
		if(timer != null) timer.cancel();
		timer = null;
		
		source.close();
		if(line != null) line.close();
		line = null;
	}
	
}
