package waffleoRai_Containers.media.matroska;

import java.util.List;

public class MatroskaEdition {
	
	private int uid;
	private boolean flagHidden;
	private boolean flagDefault;
	private boolean flagOrdered;
	
	private List<MatroskaEditionDisplay> displayInfo;
	private List<MatroskaChapterAtom> chapters;
	
	/*----- Getters -----*/

	public int getUid(){return uid;}
	public boolean getFlagHidden(){return flagHidden;}
	public boolean getFlagDefault(){return flagDefault;}
	public boolean getFlagOrdered(){return flagOrdered;}
	public List<MatroskaEditionDisplay> getDisplayInfo(){return displayInfo;}

	/*----- Setters -----*/

	public void setUid(int value){uid = value;}
	public void setFlagHidden(boolean value){flagHidden = value;}
	public void setFlagDefault(boolean value){flagDefault = value;}
	public void setFlagOrdered(boolean value){flagOrdered = value;}
	public void setDisplayInfo(List<MatroskaEditionDisplay> value){displayInfo = value;}

}
