package waffleoRai_Sound;

import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import waffleoRai_Compression.ArrayWindow;
import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_Utils.MathUtils;

/*
 * UPDATES
 * 
 * 	2020/12/19 | 1.0.0
 * 		Creation/Initial doc
 * 
 * 	2020/12/20 | 1.1.0
 * 		Added ability to manually release samples to the line (for A/V syncing)
 * 
 * 	2021/01/21 | 1.2.0
 * 		Trying to add volume/mute control. Default system line doesn't have volume
 * 		control though, so I gotta figure that out
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
	
	//protected volatile double master_vol = 1.0;
	
	protected volatile boolean closeMe;
	protected volatile boolean closedFlag;
	protected boolean pauseFlag;
	
	//protected long loc_pb;
	protected long loc_buf;
	
	protected FloatControl vol_ctrl;
	protected BooleanControl mute_ctrl;
	protected SourceDataLine line;
	protected boolean mute;
	protected float vol;
	
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
		
		//Use the currently set millis per cycle as a minimum.
		int gcf = MathUtils.gcf((int)source.getSampleRate(), millis_per_cycle);
		//int cycles = 1;
		if(gcf <= 1){
			//Just make the cycle one second.
			millis_per_cycle = 1000;
			frames_per_cycle = new int[]{(int)source.getSampleRate()};
		}
		else{
			//Div by gcf is smallest unit.
			int millis = 1000/gcf;
			int fpc = (int)source.getSampleRate()/gcf;
			if(millis < millis_per_cycle){
				//Increase it until it is above.
				int factor = millis_per_cycle/millis;
				if(millis_per_cycle%millis != 0) factor++;
				millis_per_cycle = millis*factor;
				fpc *= factor;
			}
			else millis_per_cycle = millis;
			frames_per_cycle = new int[]{fpc};
		}
		
		/*System.err.println("Millis per cycle: " + millis_per_cycle);
		System.err.print("Frames per cycle: ");
		for(int i = 0; i < cycles; i++){System.err.print(frames_per_cycle[i] + " ");}*/
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
		
		
		try{
			vol_ctrl = (FloatControl)line.getControl(FloatControl.Type.VOLUME);
			vol_ctrl.setValue(vol);
		}
		catch(Exception x){
			x.printStackTrace();
		}
		
		try{
			mute_ctrl = (BooleanControl)line.getControl(BooleanControl.Type.MUTE);
			mute_ctrl.setValue(mute);
		}
		catch(Exception x){
			x.printStackTrace();
		}
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
				if(closedFlag) return;
				loc_buf++;
				if(closeOnEnd && source.done()){
					closeWhenFinished();
					return;
				}
			
				int[] samps = source.nextSample();
				if(samps == null){
					System.err.println("JavaSoundPlayer.onBufferCycle || ERROR: Returned sample is null!");
					return;
				}
				/*if(master_vol < 1.0){
					for(int i = 0; i < samps.length; i++){
						samps[i] = (int)Math.round((double)samps[i] * master_vol);
					}	
				}*/
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
	
	public synchronized void startTimer(){
		if(timer != null)timer.cancel();
		
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask(){

			public void run() {
				onBufferCycle();
			}
			
		}, 0, millis_per_cycle);
	}
	
	/*--- Control ---*/
	
	public boolean setMute(boolean b){
		mute = b;
		if(mute_ctrl == null) return false;
		mute_ctrl.setValue(mute);
		return mute_ctrl.getValue();
	}
	
	public void setMasterVolume(int factor){
		if(factor < 0) factor = 0;
		//master_vol = (double)factor/(double)0x7fffffff;
		setMasterVolume((float)factor/(float)0x7fffffff);
	}
	
	public void setMasterVolume(float amt){
		if (amt < 0) amt = 0;
		vol = amt;
		if(vol_ctrl != null) vol_ctrl.setValue(vol);
	}
	
	public boolean masterVolumeEnabled(){
		return vol_ctrl != null;
	}
	
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
		
		//System.err.println("Released " + ct);
		return ct;
	}
	
	public synchronized void play() throws LineUnavailableException{
		if(closedFlag || isRunning()) return;
		if(!pauseFlag) open();
		startTimer();
		line.start();
		pauseFlag = false;
	}
	
	public synchronized void unpause(){
		if(closedFlag || isRunning()) return;
		if(!pauseFlag) return;
		startTimer();
		line.start();
		pauseFlag = false;
	}
	
	public synchronized void pause(){
		if(closedFlag || !isRunning()) return;
		line.stop();
		timer.cancel();
		pauseFlag = true;
		
		timer = null;
	}
	
	public synchronized void stop(){
		close();
	}
	
	public synchronized void open()throws LineUnavailableException{
		if(closedFlag || isRunning()) return;
		if(line != null) return;
		
		generateOutputLine();
		line.open();
		
		for(int i = 0; i < prebuff_cycles; i++) onBufferCycle();
		
	}
	
	public synchronized void close(){
		if(closedFlag) return;
		closedFlag = true;
		if(line != null) line.stop();
		
		if(timer != null) timer.cancel();
		timer = null;
		
		source.close();
		if(line != null) line.close();
		line = null;
		vol_ctrl = null;
		mute_ctrl = null;
	}
	
}
