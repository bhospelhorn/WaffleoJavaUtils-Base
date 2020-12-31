package waffleoRai_Video;

import javax.swing.JPanel;

import waffleoRai_GUITools.DisposableJPanel;
import waffleoRai_Sound.JavaSoundPlayer;
import waffleoRai_Sound.JavaSoundPlayer_16LE;
import waffleoRai_Sound.JavaSoundPlayer_24LE;
import waffleoRai_Sound.Sound;
import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_Utils.Arunnable;
import waffleoRai_Utils.MathUtils;
import waffleoRai_Utils.VoidCallbackMethod;

import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.BevelBorder;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.JLabel;

public class AVPlayerPanel extends DisposableJPanel{
	
	/*----- Constants -----*/

	private static final long serialVersionUID = -3768816625850941306L;
	
	private static final int DIM_ICON = 15;
	
	/*----- Static Variables -----*/
	
	/*----- Instance Variables -----*/
	
	private BufferedVideoPanel pnlVideo;
	private JavaSoundPlayer audioPlayer;
	
	private JButton btnPlayPause;
	private JButton btnStop;
	private JButton btnRewind;
	private JSlider slider;
	private JLabel lblTime;

	private Icon ico_play;
	private Icon ico_pause;
	private Icon ico_stop;
	private Icon ico_rewind;
	
	private IVideoSource v_src;
	private Sound a_src;
	
	private boolean click_sensitive;
	private boolean showPause;
	private boolean fr_snap;
	
	private volatile boolean playing;
	private volatile boolean lock_slider;
	private volatile boolean pauseFlag;
	//private boolean endFlag;
	
	private int[] a_cycle; //Audio samples to release per video frame cycle.
	private int ac;
	private volatile long ex_samps; //Expected audio sample at the beginning of v cycle (used for A/V sync)
	private int s_leeway; //Max difference in sample position between audio and v expected before sync kicks in
	private int wait_millis;
	
	private volatile boolean audio_hold;
	private AVSyncWorker sync_worker;
	
	private Timer slide_timer;
	private int vframes; //Total video frames
	private int v_cycle; //Current vcycle
	private int now_hr;
	private int now_min;
	private int now_sec;
	
	/*----- Initialization -----*/
	
	public AVPlayerPanel(IVideoSource video, Sound audio, boolean clickControl, boolean hoverPause, boolean snap_framerate){
		v_src = video;
		a_src = audio;
		click_sensitive = clickControl;
		showPause = hoverPause;
		fr_snap = snap_framerate;
		
		initGUI();
		
		calculateAudioCycle();
		if(v_src != null) vframes = v_src.getFrameCount();
	}
	
	private void initGUI(){
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JPanel pnlVWrap = new JPanel();
		pnlVWrap.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0};
		gridBagLayout.rowHeights = new int[]{0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		pnlVWrap.setLayout(gridBagLayout);
		GridBagConstraints gbc_pnlVideo = new GridBagConstraints();
		gbc_pnlVideo.insets = new Insets(0, 0, 5, 0);
		gbc_pnlVideo.fill = GridBagConstraints.BOTH;
		gbc_pnlVideo.gridx = 0;
		gbc_pnlVideo.gridy = 0;
		add(pnlVWrap, gbc_pnlVideo);
		
		pnlVideo = new BufferedVideoPanel(v_src, click_sensitive, showPause, fr_snap);
		gbc_pnlVideo = new GridBagConstraints();
		gbc_pnlVideo.insets = new Insets(0, 0, 0, 0);
		gbc_pnlVideo.fill = GridBagConstraints.BOTH;
		gbc_pnlVideo.gridx = 0;
		gbc_pnlVideo.gridy = 0;
		pnlVWrap.add(pnlVideo, gbc_pnlVideo);
		pnlVideo.setAsyncBuffering(true);
		pnlVideo.setCycleCallback(new VoidCallbackMethod(){
			public void doMethod() {
				frameCycleCallback();
			}
		});
		pnlVideo.setEndCallback(new VoidCallbackMethod(){
			public void doMethod() {
				videoEndCallback();
			}
		});
		pnlVideo.setPanelClickCallbacks(new VoidCallbackMethod(){
			public void doMethod() {
				onPlayButton();
			}
		}, new VoidCallbackMethod(){
			public void doMethod() {
				onPauseButton();
			}
		});

		
		JPanel pnlSlider = new JPanel();
		GridBagConstraints gbc_pnlSlider = new GridBagConstraints();
		gbc_pnlSlider.insets = new Insets(0, 0, 5, 0);
		gbc_pnlSlider.fill = GridBagConstraints.BOTH;
		gbc_pnlSlider.gridx = 0;
		gbc_pnlSlider.gridy = 1;
		add(pnlSlider, gbc_pnlSlider);
		GridBagLayout gbl_pnlSlider = new GridBagLayout();
		gbl_pnlSlider.columnWidths = new int[]{0, 0, 0};
		gbl_pnlSlider.rowHeights = new int[]{0, 0};
		gbl_pnlSlider.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		gbl_pnlSlider.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		pnlSlider.setLayout(gbl_pnlSlider);
		
		slider = new JSlider();
		slider.setValue(0);
		GridBagConstraints gbc_slider = new GridBagConstraints();
		gbc_slider.insets = new Insets(0, 0, 0, 5);
		gbc_slider.fill = GridBagConstraints.BOTH;
		gbc_slider.gridx = 0;
		gbc_slider.gridy = 0;
		pnlSlider.add(slider, gbc_slider);
		slider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				//System.err.println("AVPlayerPanel.slider.ChangeListener.stateChanged || Source: " + e.getSource());
				if(lock_slider) return; //Easiest workaround to ignoring event triggers by the player itself.
				if(slide_timer != null && slide_timer.isRunning()) return;
				int val = slider.getValue();
				//if(val == 0) return; //Final check
				onManualSliderMove(val);
			}
		});
		
		lblTime = new JLabel("00:00:00");
		GridBagConstraints gbc_lblTime = new GridBagConstraints();
		gbc_lblTime.insets = new Insets(0, 0, 0, 5);
		gbc_lblTime.gridx = 1;
		gbc_lblTime.gridy = 0;
		pnlSlider.add(lblTime, gbc_lblTime);
		
		JPanel pnlControls = new JPanel();
		GridBagConstraints gbc_pnlControls = new GridBagConstraints();
		gbc_pnlControls.fill = GridBagConstraints.BOTH;
		gbc_pnlControls.gridx = 0;
		gbc_pnlControls.gridy = 2;
		add(pnlControls, gbc_pnlControls);
		GridBagLayout gbl_pnlControls = new GridBagLayout();
		gbl_pnlControls.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_pnlControls.rowHeights = new int[]{0, 0};
		gbl_pnlControls.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_pnlControls.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		pnlControls.setLayout(gbl_pnlControls);
		
		btnPlayPause = new JButton("Play");
		GridBagConstraints gbc_btnPlayPause = new GridBagConstraints();
		gbc_btnPlayPause.insets = new Insets(5, 5, 5, 2);
		gbc_btnPlayPause.gridx = 0;
		gbc_btnPlayPause.gridy = 0;
		pnlControls.add(btnPlayPause, gbc_btnPlayPause);
		BufferedImage ico = BufferedVideoPanel.getPlayIcon();
		if(ico != null){
			ico_play = new ImageIcon(ico.getScaledInstance(DIM_ICON, DIM_ICON, Image.SCALE_DEFAULT));
		}
		ico = BufferedVideoPanel.getPauseIcon();
		if(ico != null){
			ico_pause = new ImageIcon(ico.getScaledInstance(DIM_ICON, DIM_ICON, Image.SCALE_DEFAULT));
		}
		setButtonPlay();
		btnPlayPause.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(playing) onPauseButton();
				else onPlayButton();
			}
		});
		
		btnStop = new JButton("Stop");
		GridBagConstraints gbc_btnStop = new GridBagConstraints();
		gbc_btnStop.insets = new Insets(5, 2, 5, 2);
		gbc_btnStop.gridx = 1;
		gbc_btnStop.gridy = 0;
		pnlControls.add(btnStop, gbc_btnStop);
		ico = BufferedVideoPanel.getStopIcon();
		if(ico != null){
			ico_stop = new ImageIcon(ico.getScaledInstance(DIM_ICON, DIM_ICON, Image.SCALE_DEFAULT));
			btnStop.setIcon(ico_stop);
			btnStop.setText("");
		}
		btnStop.repaint();
		btnStop.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				onStopButton();
			}
		});
		
		
		btnRewind = new JButton("Rewind");
		GridBagConstraints gbc_btnRewind = new GridBagConstraints();
		gbc_btnRewind.insets = new Insets(5, 2, 5, 2);
		gbc_btnRewind.gridx = 2;
		gbc_btnRewind.gridy = 0;
		pnlControls.add(btnRewind, gbc_btnRewind);
		ico = BufferedVideoPanel.getRewindIcon();
		if(ico != null){
			ico_rewind = new ImageIcon(ico.getScaledInstance(DIM_ICON, DIM_ICON, Image.SCALE_DEFAULT));
			btnRewind.setIcon(ico_rewind);
			btnRewind.setText("");
		}
		btnRewind.repaint();
		btnRewind.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				onRewindButton();
			}
		});
		
	}
	
	private void calculateAudioCycle(){
		if(a_src == null){
			a_cycle = null;
			return;
		}
		
		int v_cycle = pnlVideo.millisPerCycle();
		int sr = a_src.getSampleRate();
		ac = 0;
		
		//Get a unit of audio... (integer # of ms)
		int gcf = MathUtils.gcf(sr, 1000);
		int a_unit = 1000;
		int a_unit_samps = sr;
		if(gcf >= 2) {
			a_unit = 1000/gcf;
			a_unit_samps = sr/gcf;
		}
		
		//Now compare audio units to video units
		int lcm = MathUtils.lcm(a_unit, v_cycle); //Millis it will take for both to be integer.
		int vc_count = lcm/v_cycle; //Video cycles for this to occur.
		int ac_count = lcm/a_unit;
		int as_count = ac_count * a_unit_samps;
		
		//Break back up.
		int bsc = as_count/vc_count; //Total samples/ video cycles.
		int bsm = as_count%vc_count; //Remainder
		
		if(bsm == 0) a_cycle = new int[]{bsc};
		else{
			a_cycle = new int[vc_count];
			Arrays.fill(a_cycle, bsc);
			for(int i = 0; i < bsm; i++){
				a_cycle[a_cycle.length-1-i]++;
			}
		}
		
		//Calculate the leeway in samples. ms defaults to 500 = (1/2 second)
		s_leeway = sr/2;
		wait_millis = (int)Math.round((((double)s_leeway * 1000.0)/(double)sr) * 0.75); //Should be around 75
	}
	
	/*----- Getters -----*/
	
	public boolean isPlaying(){
		return playing;
	}
	
	/*----- Setters -----*/
	
	/*----- GUI -----*/
	
	private void setButtonPlay(){
		if(ico_play != null){
			btnPlayPause.setIcon(ico_play);
			btnPlayPause.setText("");
		}
		else btnPlayPause.setText("Play");
		btnPlayPause.repaint();
	}
	
	private void setButtonPause(){
		if(ico_pause != null){
			btnPlayPause.setIcon(ico_pause);
			btnPlayPause.setText("");
		}
		else btnPlayPause.setText("Pause");
		btnPlayPause.repaint();
	}
	
	private void onProgressUpdate(){
		int fpos = pnlVideo.getTimeFrames();
		
		//Instead, let's calculate time from frames...
		
		/*now_sec++;
		if(now_sec >= 60){
			now_min++;
			now_sec = 0;
		}
		if(now_min >= 60){
			now_hr++;
			now_min = 0;
		}*/
		updateTime(fpos);
		
		double ratio = (double)fpos/(double)vframes;
		ratio *= 100.0;
		int spos = (int)Math.round(ratio);
		//System.err.println("lock_slider = " + lock_slider);
		slider.getModel().setValue(spos); 
		
		slider.repaint();
		
		StringBuilder sb = new StringBuilder(20);
		if(now_hr > 0) sb.append(String.format("%02d", now_hr) + ":");
		sb.append(String.format("%02d", now_min) + ":");
		sb.append(String.format("%02d", now_sec));
		
		lblTime.setText(sb.toString());
		lblTime.repaint();
	}
	
	public void setControlsEnabled(boolean b){
		btnPlayPause.setEnabled(b);
		btnStop.setEnabled(b);
		btnRewind.setEnabled(b);
		
		slider.setEnabled(b);
	}
	
	public void setWait(){
		setControlsEnabled(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	public void unsetWait(){
		setCursor(null);
		setControlsEnabled(true);
	}
	
	protected void startSliderTimer(){
		lock_slider = true;
		if(slide_timer != null && slide_timer.isRunning()) slide_timer.stop();
		slide_timer = new Timer(1000, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				onProgressUpdate();
			}
			
		});
		slide_timer.start();
	}
	
	protected void stopSliderTimer(){
		if(slide_timer != null && slide_timer.isRunning()) slide_timer.stop();
		slide_timer = null;
	}
	
	protected void startSyncWorker(){
		if(sync_worker != null) stopSyncWorker();
		sync_worker = new AVSyncWorker();
		
		Thread t = new Thread(sync_worker);
		t.setName(sync_worker.getName());
		t.setDaemon(true);
		t.start();
	}
	
	protected void stopSyncWorker(){
		if(sync_worker == null) return;
		sync_worker.requestTermination();
		sync_worker = null;
	}
	
	protected void pauseSyncWorker(){
		if(sync_worker == null) return;
		sync_worker.requestPause();
	}
	
	protected void unpauseSyncWorker(){
		if(sync_worker == null) return;
		sync_worker.requestResume();
	}
	
	/*----- A/V Sync -----*/
	
	public class AVSyncWorker extends Arunnable{

		public AVSyncWorker(){
			super.delay = 250;
			super.sleeps = true;
			super.sleeptime = 1000;
			
			Random r = new Random();
			super.setName("AVSyncWorker_" + Long.toHexString(r.nextLong()));
		}
		
		public int checkSync(){
			
			long apos = audioPlayer.getPlaybackLocation();
			long diff = ex_samps - apos;
			
			if(Math.abs(diff) >= s_leeway){
				System.err.println("AVSyncWorker.checkSync || DEBUG-- Desync detected!");	
				if(diff < 0) return -1;
				else return 1;
			}
			return 0;
		}
		
		public void doSomething() {
			//Check AV sync
			if(audioPlayer == null) return;

			int sync = checkSync();
			while(sync != 0){
				//Need to sync.
				if(sync < 0){
					//Video is behind.
					//Stop audio and wait for 3/4 leeway time.
					audio_hold = true;
					audioPlayer.pause();
					try {Thread.sleep(wait_millis);} 
					catch (InterruptedException e) {e.printStackTrace();}
					audioPlayer.unpause();
					audio_hold = false;
				}
				else{
					//Audio is behind.
					//Stop video and wait for 3/4 leeway time.
					pnlVideo.quickPause();
					try {Thread.sleep(wait_millis);} 
					catch (InterruptedException e) {e.printStackTrace();}
					pnlVideo.quickUnpause();
				}
				sync = checkSync();
			}
			
			
		}
		
	}
	
	/*----- Players -----*/
	
	private void updateTime(int frame){

		double fps = v_src.getFrameRate();
		double f = (double)frame;
		
		int time = (int)Math.round(f/fps);
		now_sec = time%60;
		time /= 60;
		now_min = time%60;
		time /= 60;
		now_hr = time;
		
	}
	
	private void genAudioPlayer(long sampSkip){
		if(a_src == null){
			audioPlayer = null;
			return;
		}
		
		AudioSampleStream astr = a_src.createSampleStream(false);
		
		switch(a_src.getBitDepth()){
		case SIXTEEN_BIT_SIGNED:
			audioPlayer = new JavaSoundPlayer_16LE(astr);
			break;
		case TWENTYFOUR_BIT_SIGNED:
			audioPlayer = new JavaSoundPlayer_24LE(astr);
			break;
		default:
			//None!
			audioPlayer = null;
			return;
		}
		
		//Fast forward stream to start point.
		/*int v_cycle = pnlVideo.millisPerCycle();
		int m = 0; int c = 0;
		try{
		while(m < milli_start){
			if(c > a_cycle.length) c = 0;
			for(int j = 0; j < a_cycle[c]; j++) astr.nextSample();
			m += v_cycle;
		}}
		catch(InterruptedException x){
			x.printStackTrace();
		}*/
		
		try{
			for(int s = 0; s < sampSkip; s++){
				astr.nextSample();
			}
		}
		catch(InterruptedException x){
				x.printStackTrace();
		}
		
		//audioPlayer.setSyncedMode(true);
		audioPlayer.setPrebuffCycles(5);
	}
	
	/*----- Actions -----*/
	
	private void frameCycleCallback(){
		//For now, make synchronous......
		//if(endFlag) return;
		v_cycle++;
		if(audioPlayer == null || a_cycle == null) return;
		
		//Start audio player if it's not running...
		if(!audioPlayer.isRunning() && !audio_hold) {
			try{audioPlayer.play();}
			catch(LineUnavailableException x){x.printStackTrace();}
		}
		
		if(ac >= a_cycle.length) ac = 0;
		ex_samps += a_cycle[ac++];
		//System.err.println("Release " + a_cycle[ac]);
		//audioPlayer.releaseToLine(a_cycle[ac++]);
	}
	
	private void videoEndCallback(){
		stop();
	}
	
	private void onPlayButton(){play();}
	private void onPauseButton(){pause();}
	private void onStopButton(){stop();}
	
	private void onRewindButton(){
		stop();
		openBuffer();
	}
	
	private void onManualSliderMove(int value){
		System.err.println("AVPlayerPanel.onManualSliderMove || Manual slider move detected!");
		double ratio = (double)value/100.0;
		int f = (int)Math.round(ratio * (double)vframes);
		seek(f);
	}
	
	/*----- Control -----*/
	
	public void openBuffer(){
		setWait();
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				
				//Video
				if(v_src != null){
					pnlVideo.startBufferWorker();
				}
				
				//Audio
				if(a_src != null){
					if(audioPlayer == null) genAudioPlayer(ex_samps);
					try{audioPlayer.open();}
					catch(LineUnavailableException x){x.printStackTrace();
					 return null;}
					
					audioPlayer.startTimer(); 
				}
				
				return null;
			}
			
			public void done(){
				unsetWait();
			}
			
		};
		task.execute();
	}
	
	public void play(){
		if(isPlaying()) return;
		
		//Do these in worker threads!!
		setWait();
		playing = true;
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				
				//Audio
				if(a_src != null){
					if(audioPlayer == null){
						genAudioPlayer(ex_samps);
					}
					/*try{audioPlayer.play();}
					catch(LineUnavailableException x){x.printStackTrace();
					 return null;}*/
				}
				
				//Video
				if(v_src != null){
					pnlVideo.play();
				}
				
				startSliderTimer();
				
				if(!pauseFlag) startSyncWorker();
				else unpauseSyncWorker();
				pauseFlag = false;
				return null;
			}
			
			public void done(){
				setButtonPause();
				unsetWait();
				slider.setEnabled(false);
			}
			
		};
		task.execute();
		
	}
	
	public void pause(){
		if(!isPlaying()) return;

		setWait();
		playing = false;
		pauseFlag = true;
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				
				stopSliderTimer();
				//stopSyncWorker();
				pauseSyncWorker();
			
				//Video
				if(v_src != null){
					pnlVideo.pause();
				}
				
				if(audioPlayer != null){
					audioPlayer.pause();
				}

				return null;
			}
			
			public void done(){
				lock_slider = false;
				setButtonPlay();
				unsetWait();
			}
			
		};
		task.execute();

	}
	
	public void stop(){
		setWait();
		playing = false;
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				stopSliderTimer();
				stopSyncWorker();
				
				//Audio
				if(audioPlayer != null){
					audioPlayer.stop();
					audioPlayer.close();
					audioPlayer = null;
				}
				
				//Video
				if(v_src != null){
					pnlVideo.stop();
				}
				
				//Common
				v_cycle = 0;
				now_hr = 0;
				now_min = 0;
				now_sec = 0;
				ac = 0;
				ex_samps = 0;
				
				lock_slider = true;
				onProgressUpdate();
				
				return null;
			}
			
			public void done(){
				lock_slider = false;
				setButtonPlay();
				unsetWait();
			}
			
		};
		task.execute();
	}
	
	public boolean seek(int frame){
		//Snaps to frame cycle start (so can more easily sync audio)
		
		setWait();
		playing = false;
		
		SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

			protected Void doInBackground() throws Exception {
				stopSliderTimer();
				stopSyncWorker();
				
				//Audio
				if(audioPlayer != null){
					audioPlayer.stop();
					audioPlayer.close();
					audioPlayer = null;
				}
				
				//Video
				if(v_src != null){
					pnlVideo.stop();
				}
				
				//Now, update time
				ac = 0;
				int vcm = pnlVideo.millisPerCycle();
				int vcf = pnlVideo.framesPerCycle();
				v_cycle = frame/vcf;
				int rawtime = vcm * v_cycle;
				rawtime /= 1000; //Seconds
				now_hr = rawtime/3600;
				rawtime -= (now_hr * 3600);
				now_min = rawtime/60;
				rawtime -= (now_min * 60);
				now_sec = rawtime;
				
				//Expected audio samples
				ex_samps = 0;
				ac = 0;
				for(int v = 0; v < v_cycle; v++){
					if(ac >= a_cycle.length) ac = 0;
					ex_samps += a_cycle[ac++];
				}
				
				//Reopen streams
				if(v_src != null){
					int f = v_cycle * vcf;
					pnlVideo.seek(f);
					pnlVideo.startBufferWorker();
				}
				if(a_src != null){
					genAudioPlayer(ex_samps);
					try{audioPlayer.open();}
					catch(LineUnavailableException x){x.printStackTrace();
					 return null;}
					
					audioPlayer.startTimer(); 
				}
				
				//Repaint slider/timer
				onProgressUpdate();
				
				return null;
			}
			
			public void done(){
				setButtonPlay();
				unsetWait();
			}
			
		};
		task.execute();
		
		return false;
	}
	
	public void dispose(){
		stop();
		pnlVideo.dispose();
		
		ico_play = null;
		ico_pause = null;
		ico_stop = null;
		ico_rewind = null;
	}
	
}
