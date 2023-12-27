package waffleoRai_soundbank.dls;

import waffleoRai_Utils.BufferReference;

public class DLSSampleLoop {
	
	public static final int WLOOP_TYPE_FORWARD = 0;
	public static final int WLOOP_TYPE_RELEASE = 1;
	
	private int ulLoopType;
	private int ulLoopStart; //In samples
	private int ulLoopLength; //In samples
	
	private DLSSampleLoop(){}
	
	public static DLSSampleLoop read(BufferReference data){
		if(data == null) return null;
		
		DLSSampleLoop loop = new DLSSampleLoop();
		data.add(4);
		loop.ulLoopType = data.nextInt();
		loop.ulLoopStart = data.nextInt();
		loop.ulLoopLength = data.nextInt();
		return loop;
	}
	
	public int getLoopType(){return ulLoopType;}
	public int getLoopStart(){return ulLoopStart;}
	public int getLoopLength(){return ulLoopLength;}

}
