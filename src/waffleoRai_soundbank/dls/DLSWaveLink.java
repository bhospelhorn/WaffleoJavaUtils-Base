package waffleoRai_soundbank.dls;

import waffleoRai_Utils.BufferReference;

public class DLSWaveLink {
	
	private short fusOptions;
	private short usPhaseGroup;
	private int ulChannel;
	private int ulTableIndex;
	
	private DLSWaveLink(){}
	
	public static DLSWaveLink read(BufferReference data){
		if(data == null) return null;
		DLSWaveLink wlnk = new DLSWaveLink();
		wlnk.fusOptions = data.nextShort();
		wlnk.usPhaseGroup = data.nextShort();
		wlnk.ulChannel = data.nextInt();
		wlnk.ulTableIndex = data.nextInt();
		return wlnk;
	}
	
	public short getOptions(){return fusOptions;}
	public short getPhaseGroup(){return usPhaseGroup;}
	public int getChannel(){return ulChannel;}
	public int getTableIndex(){return ulTableIndex;}

}
