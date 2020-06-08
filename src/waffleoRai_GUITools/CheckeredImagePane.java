package waffleoRai_GUITools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

public class CheckeredImagePane extends JPanel{

	/*----- Constants -----*/
	
	private static final long serialVersionUID = -8459938256582412945L;
	
	public static final Color CHECKER_CLR_1 = new Color(255, 255, 255);
	public static final Color CHECKER_CLR_2 = new Color(245, 245, 245);
	public static final int CHECKER_DIM = 4;
	
	public static final int ANCHOR_CENTER = 0;
	public static final int ANCHOR_NORTH = 1;
	public static final int ANCHOR_SOUTH = 2;
	public static final int ANCHOR_EAST = 3;
	public static final int ANCHOR_WEST = 4;
	public static final int ANCHOR_NORTHWEST = 5;
	public static final int ANCHOR_NORTHEAST = 6;
	public static final int ANCHOR_SOUTHWEST = 7;
	public static final int ANCHOR_SOUTHEAST = 8;
	
	public static final int DEFO_WIDTH = 640;
	public static final int DEFO_HEIGHT = 480;
	
	/*----- Instance Variables -----*/
	
	private int anchor;
	
	//Drawing area
	private int area_w;
	private int area_h;
	
	private int draw_w;
	private int draw_h;
	private int draw_x;
	private int draw_y;
	
	private List<ImagePaneDrawer> items;
	
	private int refresh_time;
	private Timer timer;
	
	/*----- Construction -----*/
	
	public CheckeredImagePane(){
		this(DEFO_WIDTH, DEFO_HEIGHT);
	}
	
	public CheckeredImagePane(Dimension dim){
		this(dim.width, dim.height);
	}
	
	public CheckeredImagePane(int w, int h){
		items = new LinkedList<ImagePaneDrawer>();
		area_w = w;
		area_h = h;
		
		super.setMinimumSize(getDrawingAreaSize());
		super.setPreferredSize(getDrawingAreaSize());
		
		draw_w = 0; draw_h = 0;
		recalculateDrawingStart();
		
		refresh_time = 0;
	}
	
	/*----- Getters -----*/
	
	public int getDrawingAnchor(){
		return anchor;
	}
	
	public Dimension getDrawingAreaSize(){
		return new Dimension(area_w, area_h);
	}
	
	public int getAnimationRefreshTime(){
		return refresh_time;
	}
	
	/*----- Setters -----*/
	
	public void setDrawingAnchor(int a){
		anchor = a;
		recalculateDrawingStart();
		repaint();
	}
	
	public void setDrawingAreaSize(Dimension dim){
		area_w = dim.width;
		area_h = dim.height;
		
		super.setMinimumSize(getDrawingAreaSize());
		super.setPreferredSize(getDrawingAreaSize());
		
		recalculateDrawingStart();
		
		repaint();
	}
	
	public void addItem(ImagePaneDrawer drawable){
		items.add(drawable);
		recalculateDrawingSize();
		recalculateDrawingStart();
		repaint();
	}
	
	public void clearItems(){
		items.clear();
		draw_w = 0; draw_h = 0;
		recalculateDrawingStart();
		repaint();
	}
	
	public void setAnimationRefreshTime(int millis){
		if(timer != null) timer.stop();
		
		timer = new Timer(millis, new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				repaint();
			}
			
		});
		
	}
	
	public void startAnimation(){
		if(timer != null) timer.start();
	}
	
	public void stopAnimation(){
		if(timer != null) timer.stop();
	}
	
	/*----- Draw -----*/
	
	private void genBackground(Graphics g){
		
		boolean rowc = false;
		for(int y = 0; y < area_h; y+=CHECKER_DIM){
			boolean colc = rowc;
			for(int x = 0; x < area_w; x+=CHECKER_DIM){
				if(colc) g.setColor(CHECKER_CLR_2);
				else g.setColor(CHECKER_CLR_1);
				g.fillRect(x, y, CHECKER_DIM, CHECKER_DIM);
				colc = !colc;
			}
			rowc = !rowc;
		}
		
	}
	
	private void recalculateDrawingSize(){

		draw_w = 0; draw_h = 0;
		for(ImagePaneDrawer d : items){
			int X = d.getX() + d.getWidth();
			if(X > draw_w) draw_w = X;
			int Y = d.getY() + d.getHeight();
			if(Y > draw_h) draw_h = Y;
		}
		
	}
	
	private void recalculateDrawingStart(){
		int anc_x = 0;
		int anc_y = 0;
		
		switch(anchor){
		case ANCHOR_NORTH: anc_x = 0; anc_y = -1; break;
		case ANCHOR_SOUTH: anc_x = 0; anc_y = 1; break;
		case ANCHOR_NORTHWEST: anc_x = -1; anc_y = -1; break;
		case ANCHOR_NORTHEAST: anc_x = 1; anc_y = -1; break;
		case ANCHOR_SOUTHWEST: anc_x = -1; anc_y = 1; break;
		case ANCHOR_SOUTHEAST: anc_x = 1; anc_y = 1; break;
		case ANCHOR_WEST: anc_x = -1; anc_y = 0; break;
		case ANCHOR_EAST: anc_x = 1; anc_y = 0; break;
		}
		
		switch(anc_x){
		case -1: 
			//Left
			draw_x = 0; break;
		case 0:
			//Center
			int cenx = area_w/2;
			draw_x = cenx - (draw_w/2); 
			break;
		case 1:
			//Right
			draw_x = (area_w - draw_w); break;
		}
		
		switch(anc_y){
		case -1: 
			//Top
			draw_y = 0; break;
		case 0:
			//Center
			int ceny = area_h/2;
			draw_y = ceny - (draw_h/2);
			break;
		case 1:
			//Bottom
			draw_y = (area_h - draw_h); break;
		}
		
	}
	
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		genBackground(g);
		
		//Paint items, taking anchor into account
		//int c = 0;
		for(ImagePaneDrawer d : items){
			//System.err.println("Drawing item " + c++);
			d.drawMe(g, draw_x, draw_y);
		}
	}

}
