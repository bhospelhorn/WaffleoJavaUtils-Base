package waffleoRai_GUITools;

public abstract class AnimatedImagePaneDrawer implements ImagePaneDrawer{

	private int start_time;
	private int len_millis = 1000;
	
	//Scratch fields
	//protected int map_id;
	protected int counter;
	
	public int getStartTime(){
		return start_time;
	}
	
	public int getLengthInMillis(){
		return len_millis;
	}
	
	public void setStartTime(int millis){
		start_time = millis;
	}
	
	public void setLength(int millis){
		len_millis = millis;
	}
	
	public abstract void setPosition(int x, int y);
	public abstract AnimatedImagePaneDrawer getCopy();
	
	public boolean equals(Object o){return this==o;}
	
	
}
