package waffleoRai_Containers.media.matroska;

public class MatroskaChapterProcessCommand {
	
	private long time;
	private byte[] data;
	
	/*----- Getters -----*/

	public long getTime(){return time;}
	public byte[] getData(){return data;}

	/*----- Setters -----*/

	public void setTime(long value){time = value;}
	public void setData(byte[] value){data = value;}

}
