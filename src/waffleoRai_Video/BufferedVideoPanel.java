package waffleoRai_Video;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

import waffleoRai_Utils.Arunnable;
import waffleoRai_Utils.MathUtils;
import waffleoRai_Utils.VoidCallbackMethod;

public class BufferedVideoPanel extends JPanel{

	/*----- Constants -----*/
	
	private static final long serialVersionUID = 8692675955369697411L;
	
	public static final int MIN_WIDTH = 120;
	public static final int MIN_HEIGHT = 100;
	
	/*----- Static Variables -----*/
	
	private static boolean img_load_attempt;
	private static BufferedImage img_scr_play;
	private static BufferedImage img_scr_pause;
	
	private static BufferedImage img_play;
	private static BufferedImage img_pause;
	private static BufferedImage img_stop;
	private static BufferedImage img_rewind;
	
	/*----- Instance Variables -----*/
	
	//private int width;
	//private int height;
	
	private Timer timer;
	private long time;
	private volatile int frame_idx;
	
	private int[] cycle;
	//private int cycle_ctr;
	
	private boolean click_sensitive;
	private boolean showPause;
	private boolean fr_snap;
	private volatile boolean hoverFlag;
	
	private IVideoSource src;
	private VideoFrameStream str; //open stream
	
	private BufferedImage current_frame;
	
	private volatile boolean endFlag; //Set by async buffer if it encounters end.
	private boolean useAsyncBuffer;
	private int buffer_size; //In frames. Buffer check every 1/2 buff size
	private ConcurrentLinkedQueue<BufferedImage> buffer;
	private BufferRunner worker;
	
	private VoidCallbackMethod cycle_callback;
	private VoidCallbackMethod end_callback;
	
	/*----- Initialization -----*/
	
	public BufferedVideoPanel(IVideoSource source, boolean clickControl, boolean hoverPause, boolean snap_framerate){
		if(!img_load_attempt) {
			try{loadImages();}
			catch(IOException e){e.printStackTrace();}
		}
		
		src = source;
		click_sensitive = clickControl;
		showPause = hoverPause;
		fr_snap = snap_framerate;
		
		//If click sensitive, add a listener
		if(click_sensitive){
			this.addMouseListener(new MouseAdapter(){

				public void mousePressed(MouseEvent e) {
					onScreenClick();
				}
				
				public void mouseEntered(MouseEvent e){
					if(showPause){hoverFlag = true; repaint();}
				}
				
				public void mouseExited(MouseEvent e){
					if(showPause){hoverFlag = false; repaint();}
				}

			});	
		}
		
		this.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		this.setBackground(Color.black);
		
		buffer = new ConcurrentLinkedQueue<BufferedImage>();
		if(src != null){
			this.setPreferredSize(new Dimension(src.getWidth(), src.getHeight()));
			buffer_size = (1000/src.millisPerFrame()) + 1;
			
			if(snap_framerate && MathUtils.isInteger(src.getFrameRate())) calculateCycle();
			else{
				cycle = new int[]{src.millisPerFrame()};
			}
		}
		else{
			buffer_size = 30;
			cycle = new int[]{1000};
		}
	}
	
	private static void loadImages() throws IOException{
		img_load_attempt = true;
		
		img_scr_play = ImageIO.read(BufferedVideoPanel.class.getResource("/waffleoRai_Video/res/vidpnl_screen_play.png"));
		img_scr_pause = ImageIO.read(BufferedVideoPanel.class.getResource("/waffleoRai_Video/res/vidpnl_screen_pause.png"));
		
		img_play = ImageIO.read(BufferedVideoPanel.class.getResource("/waffleoRai_Video/res/vidpnl_play.png"));
		img_pause = ImageIO.read(BufferedVideoPanel.class.getResource("/waffleoRai_Video/res/vidpnl_pause.png"));
		img_stop = ImageIO.read(BufferedVideoPanel.class.getResource("/waffleoRai_Video/res/vidpnl_stop.png"));
		img_rewind = ImageIO.read(BufferedVideoPanel.class.getResource("/waffleoRai_Video/res/vidpnl_back.png"));
	}
	
	private void calculateCycle(){
		int fr = (int)src.getFrameRate();
		int base_millis = src.millisPerFrame();
		
		int m_prod = base_millis * fr; //Actual millis it takes to do fr frames at the base frame len
		//If not 1000, need to adjust...
		
		if(m_prod > 1000){
			int diff = m_prod - 1000;
			//diff frames per second need to be one milli shorter
			//Also while diff > fps, subtract one milli from all frames until not.
			while(diff >= fr){
				base_millis--;
				diff -= fr;
			}
			
			int gcf = MathUtils.gcf(fr, diff);
			//gcf is the number of cycles per second.
			int cylen = fr/gcf;
			cycle = new int[cylen];
			for(int i = 0; i < cylen; i++) cycle[i] = base_millis;
			
			//Now, decrement diff/gcf per cycle
			int dec = diff/gcf;
			for(int i = 0; i < dec; i++) cycle[i]--;
		}
		else if(m_prod < 1000){
			//Same as above, but with increment
			int diff = 1000 -  m_prod;
			while(diff >= fr){
				base_millis++;
				diff -= fr;
			}
			
			int gcf = MathUtils.gcf(fr, diff);
			int cylen = fr/gcf;
			cycle = new int[cylen];
			for(int i = 0; i < cylen; i++) cycle[i] = base_millis;
			
			int inc = diff/gcf;
			for(int i = 0; i < inc; i++) cycle[i]++;
		}
		else{
			//Equal
			cycle = new int[]{base_millis};
		}
		
	}
	
	/*----- Getters -----*/
	
	public boolean isPlaying(){
		if(timer == null) return false;
		return timer.isRunning();
	}

	public long getTimeMillis(){return time;}
	public int getTimeFrames(){return this.frame_idx;}
	public IVideoSource getSource(){return src;}
	public BufferedImage getCurrentFrame(){return current_frame;}
	public boolean asyncBuffering(){return this.useAsyncBuffer;}
	
	public int millisPerCycle(){
		int ct = 0;
		for(int i = 0; i < cycle.length; i++) ct += cycle[i];
		return ct;
	}
	
	public int framesPerCycle(){
		if(cycle == null) return 1;
		return cycle.length;
	}
	
	public static BufferedImage getPlayIcon(){return img_play;}
	public static BufferedImage getPauseIcon(){return img_pause;}
	public static BufferedImage getStopIcon(){return img_stop;}
	public static BufferedImage getRewindIcon(){return img_rewind;}
	
	/*----- Setters -----*/
	
	public void setAsyncBuffering(boolean b){
		if(isPlaying()) throw new UnsupportedOperationException();
		useAsyncBuffer = b;
		if(!useAsyncBuffer){
			//Make sure there's no buffer worker!
			stopBufferWorker();
		}
		else{
			//Generate a buffer worker
			worker = new BufferRunner(500);
		}
	}
	
	public void setCycleCallback(VoidCallbackMethod method){
		cycle_callback = method;
	}
	
	public void setEndCallback(VoidCallbackMethod method){
		end_callback = method;
	}
	
	public void setVideo(IVideoSource vid){
		stop();
		
		if(src != null){
			this.setPreferredSize(new Dimension(src.getWidth(), src.getHeight()));
			buffer_size = (1000/src.millisPerFrame()) + 1;
			
			if(fr_snap && MathUtils.isInteger(src.getFrameRate())) calculateCycle();
			else{
				cycle = new int[]{src.millisPerFrame()};
			}
		}
		else{
			buffer_size = 30;
			cycle = new int[]{1000};
		}
	}
	
	/*----- Buffer -----*/
	
	private class BufferRunner extends Arunnable{

		public BufferRunner(int interval){
			super.setName("BufferedVideoPanel_BufferDaemon");
			super.sleeps = true;
			super.sleeptime = interval;
		}
		
		public void doSomething() {
			fillBuffer();
		}
		
	}
	
	public boolean fillBuffer(){
		if(str == null) return false;
		if(endFlag) return true;
		
		while(buffer.size() < buffer_size){
			if(str.done()) {endFlag = true; return true;}
			buffer.add(str.getNextFrame());
		}
		return true;
	}
	
	public boolean bufferRunning(){
		if(worker == null) return false;
		return worker.anyThreadsAlive();
	}
	
	public void startBufferWorker(){
		if(!useAsyncBuffer) return;
		if(bufferRunning()) return;
		if(src == null) return;
		
		if(worker == null) worker = new BufferRunner(500);
		Random r = new Random();
		Thread t = new Thread(worker);
		t.setName(worker.getName() + "_" + Long.toHexString(r.nextLong()));
		t.setDaemon(true);
		t.start();
	}
	
	public void stopBufferWorker(){
		if(worker != null && worker.anyThreadsAlive()) worker.requestTermination();
		worker = null;
	}
	
	/*----- GUI -----*/
	
	private void drawCenterButton(BufferedImage img, Graphics g, int fw, int fh){
		
		int cx = fw/2;
		int cy = fh/2;
		
		int iw = img.getWidth();
		int ih = img.getHeight();
		
		int x = cx - (iw/2);
		int y = cy - (ih/2);
		
		g.drawImage(img, x, y, null);
	}
	
	public void paintComponent(Graphics g){

		//Clear frame?
		Rectangle bounds = g.getClipBounds();
		int w = bounds.width;
		int h = bounds.height;
		
		g.clearRect(0, 0, w, h);
		
		//Draw image, if applicable
		if(current_frame != null){
			//Paint frame (may also need to rescale)
			if(current_frame.getWidth() != w || current_frame.getWidth() != h){
				g.drawImage(current_frame.getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT), 0, 0, null);
			}
			else g.drawImage(current_frame, 0, 0, null);
			
			if(!isPlaying() || hoverFlag){
				//Draw semi-transparent black rectangle over to dim it.
				Color c = g.getColor();
				g.setColor(new Color(0, 0, 0, 128));
				g.fillRect(0, 0, w, h);
				g.setColor(c);
			}
		}
		
		//Paint play/pause button if appropriate
		if(!isPlaying()){
			drawCenterButton(img_scr_play, g, w, h);
		}
		else if (hoverFlag) drawCenterButton(img_scr_pause, g, w, h);
		
	}
	
	protected void onScreenClick(){
		//Pause or play
		if(isPlaying()) pause();
		else play();
	}
	
	/*----- Control -----*/
	
	private void onTickCycle(){
		if(str == null) return;
		if(cycle_callback != null) cycle_callback.doMethod();
		
		for(int i = 0; i < cycle.length-1; i++){
			if(!onFrameTick()) return;
			try{Thread.sleep(cycle[i]);}
			catch(InterruptedException x){
				x.printStackTrace();
				//But continue...
			}
			time += cycle[i];
		}
		
		if(!onFrameTick()) return;
		time += cycle[cycle.length-1];
	}
	
	private boolean onFrameTick(){
		//if(str == null) return;
		frame_idx++;
		//time += src.millisPerFrame();
		if(buffer.isEmpty()){
			//Render a new one or check if end...
			if(endFlag || str.done()){
				atStreamEnd();
				return false; //Didn't render a new one
			}
			current_frame = str.getNextFrame();
		}
		else current_frame = buffer.poll();
		repaint();
		return true;
	}
	
	private void atStreamEnd(){
		stop();
		if(end_callback != null) end_callback.doMethod();
	}
	
	private void startPlayTimer(){
		if(timer != null && timer.isRunning()) timer.stop();
		if(src == null) return;
		
		int clen = 0;
		for(int i = 0; i < cycle.length; i++) clen += cycle[i];
		timer = new Timer(clen, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				onTickCycle();
			}});
	
		timer.start();
	}
	
	private void stopPlayTimer(){
		if(timer == null) return;
		if(timer.isRunning()) timer.stop();
		timer = null;
	}
	
	public void play(){
		if(src == null) return;
		
		//Resume or open new stream.
		if(isPlaying()) return;
		if(str == null || str.done()){
			if(str != null) str.close();
			try {str = src.openStream();} 
			catch (IOException e) {
				e.printStackTrace();
				return;
			}
			time = 0; frame_idx = 0;
		}
		
		startBufferWorker(); //Does nothing if already running or not in async mode
		startPlayTimer();
	}
	
	public void pause(){
		stopPlayTimer();
		repaint();
	}
	
	public void stop(){
		stopPlayTimer();
		stopBufferWorker();
		
		buffer.clear();
		current_frame = null;
		if(str != null){
			str.close();
			str = null;
		}
		
		repaint();
	}
	
	public void rewind(){
		stop();
		str = null; //Next time it plays, will spawn new stream from beginning.
		time = 0; frame_idx = 0;
	}
	
	public void seek(int min, int sec, int frame){
		stop();
		if(src == null) return;
		
		//Calculate frame
		int f = 0; int i = 0;
		int s = ((min*60) + sec) * 1000;
		int ms = 0;
		while(ms < s){
			if(i > cycle.length) i = 0;
			ms += cycle[i++];
			f++;
		}
		f += frame;
		
		try {str = src.openStreamAt(f);} 
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		//Pull first frame to preview
		fillBuffer();
		current_frame = buffer.peek();
		
		//Calculate time
		frame_idx = f;
		i = 0;
		for(int j = 0; j < f; j++){
			if(i > cycle.length) i = 0;
			time += cycle[i++];
		}
		
		repaint();
	}
	
	public void seek(int frame){
		stop();
		if(src == null) return;
		
		try {str = src.openStreamAt(frame);} 
		catch (IOException e){
			e.printStackTrace();
			return;
		}
		//Pull first frame to preview
		fillBuffer();
		current_frame = buffer.peek();
		
		//Calculate time
		frame_idx = frame;
		int i = 0;
		for(int f = 0; f < frame; f++){
			if(i > cycle.length) i = 0;
			time += cycle[i++];
		}
		
		repaint();
	}
	
	public void dispose(){
		stop();
		worker = null;
		src = null;
	}

}
