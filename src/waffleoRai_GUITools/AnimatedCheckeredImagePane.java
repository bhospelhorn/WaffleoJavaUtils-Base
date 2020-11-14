package waffleoRai_GUITools;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Timer;

public class AnimatedCheckeredImagePane extends CheckeredImagePane{

	private static final long serialVersionUID = -1078210156885768922L;

	/*----- Constants -----*/
	
	/*----- Instance Variables -----*/
	
	private Map<Integer, List<AnimatedImagePaneDrawer>> frame_map;
	private Set<AnimatedImagePaneDrawer> active_frames;
	
	private int counter; //time
	private int loop_point;
	private boolean one_shot;
	private Timer timer;
	
	/*----- Construction -----*/
	
	public AnimatedCheckeredImagePane(int w, int h){
		super(w,h);
		
		frame_map = new HashMap<Integer, List<AnimatedImagePaneDrawer>>();
		active_frames = new HashSet<AnimatedImagePaneDrawer>();
		
		counter = 0;
		loop_point = 1000;
		
		constructTimer();
	}
	
	private void constructTimer(){
		timer = new Timer(10, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				//Check timepoint
				//Frames to remove...
				List<AnimatedImagePaneDrawer> rframes = new LinkedList<AnimatedImagePaneDrawer>();
				for(AnimatedImagePaneDrawer f : active_frames){
					f.counter -= 10;
					if(f.counter <= 0) rframes.add(f);
				}
				if(!rframes.isEmpty()){
					rframes.removeAll(rframes);
					items.removeAll(rframes);	
				}
				
				//Frames to add...
				List<AnimatedImagePaneDrawer> nframes = frame_map.get(counter);
				if(nframes != null){
					for(AnimatedImagePaneDrawer f:nframes){
						f.counter = f.getLengthInMillis();
						active_frames.add(f);
						items.add(f);
					}
				}
				
				//Repaint super
				repaint();
				
				//Increment time/ loop or stop if needed
				counter += 10;
				if(counter >= loop_point){
					if(one_shot) stopAnimation(); //See if this causes issues calling here
					else counter = 0;
				}
				
			}});
	}
	
	/*----- Getters -----*/
	
	public int getLoopPoint(){return loop_point;}
	public boolean isOneShot(){return one_shot;}
	
	/*----- Setters -----*/
	
	public void setLoopPoint(int loop){loop_point = loop;}
	public void setOneShot(boolean b){one_shot = b;}
	
	public void addFrame(AnimatedImagePaneDrawer frame){
		
		//Snap times to centisecond
		int stime = frame.getStartTime();
		int len = frame.getLengthInMillis();
		
		int mod = stime % 10;
		if(mod != 0){
			stime /= 10;
			stime *= 10;
			if(mod >= 5) stime += 10;
			frame.setStartTime(stime);
		}
		
		mod = len % 10;
		if(mod != 0){
			len /= 10;
			len *= 10;
			if(mod >= 5) len += 10;
			frame.setLength(len);
		}
		
		//Add to map
		List<AnimatedImagePaneDrawer> list = frame_map.get(stime);
		if(list == null){
			list = new LinkedList<AnimatedImagePaneDrawer>();
			frame_map.put(stime, list);
		}
		list.add(frame);
		
	}
	
	public void clearAllFrames(){
		frame_map.clear();
		active_frames.clear();
		items.clear();
	}
	
	/*----- Animation -----*/
	
	public void startAnimation(){
		if(timer != null){
			counter = 0;
			super.items.clear();
			active_frames.clear();
			timer.start();
		}
	}
	
	public void stopAnimation(){
		if(timer != null){
			timer.stop();
			active_frames.clear();
		}
	}
	
	public void setToViewAllFrames(){
		//Stops animation and rearranges frames into a 
		//grid to display all at once
		if(timer != null && timer.isRunning()) stopAnimation();
		items.clear();
		
		//Convert to list...
		LinkedList<AnimatedImagePaneDrawer> allframes = new LinkedList<AnimatedImagePaneDrawer>();
		List<Integer> keys = new LinkedList<Integer>();
		keys.addAll(frame_map.keySet());
		Collections.sort(keys);
		for(Integer k : keys){
			List<AnimatedImagePaneDrawer> flist = frame_map.get(k);
			if(flist != null){
				for(AnimatedImagePaneDrawer f : flist) allframes.add(f.getCopy());
			}
		}
		
		//Reset X-Y coordinates
		final int COLS = 4;
		final int SPACING = 4;
		int x = 0;
		int y = 0;
		int w = 0;
		int h = 0;
		int l = 0;
		int rheight = 0;
		
		for(AnimatedImagePaneDrawer f : allframes){
			f.setPosition(x, y);
			if(f.getHeight() > rheight) rheight = f.getHeight();
			
			if(++l >= COLS){
				//Check x
				int rightbound = x + f.getWidth() + SPACING;
				if(rightbound > w) w = rightbound;
				
				//Next row
				x = 0; y += rheight + SPACING;
				l = 0;
				rheight = 0;
			}
			else{
				//Next col
				x += f.getWidth() + SPACING;
			}
		}
		
		//Adjust height
		h = y + rheight + SPACING;
		
		//Send to super
		items.addAll(allframes);
		super.setDrawingAreaSize(new Dimension(w,h));
		
		super.repaint();
		
	}
	
}
