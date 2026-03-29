package waffleoRai_Containers.media.matroska;

import java.util.List;

public class MatroskaChapterProcess {
	
	private int codecID;
	private byte[] privateData;
	private List<MatroskaChapterProcessCommand> commands;
	
	/*----- Getters -----*/

	public int getCodecID(){return codecID;}
	public byte[] getPrivateData(){return privateData;}
	public List<MatroskaChapterProcessCommand> getCommands(){return commands;}

	/*----- Setters -----*/

	public void setCodecID(int value){codecID = value;}
	public void setPrivateData(byte[] value){privateData = value;}
	public void setCommands(List<MatroskaChapterProcessCommand> value){commands = value;}

}
