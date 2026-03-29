package waffleoRai_Containers.media.matroska;

import java.util.List;

public class MatroskaChapterAtom {
	
	private int uid;
	private String stringUID;
	private long timeStart;
	private long timeEnd;
	private boolean flagHidden;
	private boolean flagEnabled;
	private int skipType;
	private int segmentEditionUID;
	private int physEquiv;
	
	private List<Integer> trackUIDs;
	private List<MatroskaChapterDisplay> displayInfo;
	private List<MatroskaChapterProcess> processes;
	
	/*----- Getters -----*/

	public int getUid(){return uid;}
	public String getStringUID(){return stringUID;}
	public long getTimeStart(){return timeStart;}
	public long getTimeEnd(){return timeEnd;}
	public boolean getFlagHidden(){return flagHidden;}
	public boolean getFlagEnabled(){return flagEnabled;}
	public int getSkipType(){return skipType;}
	public int getSegmentEditionUID(){return segmentEditionUID;}
	public int getPhysEquiv(){return physEquiv;}
	public List<Integer> getTrackUIDs(){return trackUIDs;}
	public List<MatroskaChapterDisplay> getDisplayInfo(){return displayInfo;}
	public List<MatroskaChapterProcess> getProcesses(){return processes;}

	/*----- Setters -----*/

	public void setUid(int value){uid = value;}
	public void setStringUID(String value){stringUID = value;}
	public void setTimeStart(int value){timeStart = value;}
	public void setTimeEnd(int value){timeEnd = value;}
	public void setFlagHidden(boolean value){flagHidden = value;}
	public void setFlagEnabled(boolean value){flagEnabled = value;}
	public void setSkipType(int value){skipType = value;}
	public void setSegmentEditionUID(int value){segmentEditionUID = value;}
	public void setPhysEquiv(int value){physEquiv = value;}
	public void setTrackUIDs(List<Integer> value){trackUIDs = value;}
	public void setDisplayInfo(List<MatroskaChapterDisplay> value){displayInfo = value;}
	public void setProcesses(List<MatroskaChapterProcess> value){processes = value;}

}
