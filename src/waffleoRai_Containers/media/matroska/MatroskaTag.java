package waffleoRai_Containers.media.matroska;

import java.util.List;

public class MatroskaTag {
	
	//Targets
	private int targetTypeValue;
	private String targetType;
	private List<Integer> tagTrackUIDs;
	private List<Integer> tagEditionUIDs;
	private List<Integer> tagChapterUIDs;
	private List<Integer> tagAttachmentUIDs;
	
	//Simple Tag
	private List<MatroskaSimpleTag> simpleTags;
	
	/*----- Getters -----*/

	public int getTargetTypeValue(){return targetTypeValue;}
	public String getTargetType(){return targetType;}
	public List<Integer> getTagTrackUIDs(){return tagTrackUIDs;}
	public List<Integer> getTagEditionUIDs(){return tagEditionUIDs;}
	public List<Integer> getTagChapterUIDs(){return tagChapterUIDs;}
	public List<Integer> getTagAttachmentUIDs(){return tagAttachmentUIDs;}
	public List<MatroskaSimpleTag> getSimpleTags(){return simpleTags;}

	/*----- Setters -----*/

	public void setTargetTypeValue(int value){targetTypeValue = value;}
	public void setTargetType(String value){targetType = value;}
	public void setTagTrackUIDs(List<Integer> value){tagTrackUIDs = value;}
	public void setTagEditionUIDs(List<Integer> value){tagEditionUIDs = value;}
	public void setTagChapterUIDs(List<Integer> value){tagChapterUIDs = value;}
	public void setTagAttachmentUIDs(List<Integer> value){tagAttachmentUIDs = value;}
	public void setSimpleTags(List<MatroskaSimpleTag> value){simpleTags = value;}


}
