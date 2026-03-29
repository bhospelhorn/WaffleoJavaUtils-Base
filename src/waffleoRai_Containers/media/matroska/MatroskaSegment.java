package waffleoRai_Containers.media.matroska;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

public class MatroskaSegment {
	
	private byte[] segUUID;
	private String filename;
	
	private byte[] prevSegUUID;
	private String prevSegFilename;
	private byte[] nextSegUUID;
	private String nextSegFilename;
	private byte[] segFamily;
	
	private List<MatroskaChapterTranslate> chapterTranslate;
	
	private int timescale;
	private double duration;
	
	private ZonedDateTime timestamp;
	private String title;
	private String muxingApp;
	private String writingApp;
	
	private List<MatroskaCluster> clusters;
	private List<MatroskaTrack> tracks;
	private List<MatroskaCuePoint> cues;
	private List<MatroskaAttachment> attachments;
	private List<MatroskaEdition> editions;
	private List<MatroskaChapterAtom> chapters;
	private List<MatroskaTag> tags;

	/*----- Init -----*/
	
	public MatroskaSegment() {
		chapterTranslate = new LinkedList<MatroskaChapterTranslate>();
		clusters = new LinkedList<MatroskaCluster>();
		tracks = new LinkedList<MatroskaTrack>();
		cues = new LinkedList<MatroskaCuePoint>();
		attachments = new LinkedList<MatroskaAttachment>();
		editions = new LinkedList<MatroskaEdition>();
		chapters = new LinkedList<MatroskaChapterAtom>();
		tags = new LinkedList<MatroskaTag>();
	}
	
	/*----- Getters -----*/

	public byte[] getSegUUID(){return segUUID;}
	public String getFilename(){return filename;}
	public byte[] getPrevSegUUID(){return prevSegUUID;}
	public String getPrevSegFilename(){return prevSegFilename;}
	public byte[] getNextSegUUID(){return nextSegUUID;}
	public String getNextSegFilename(){return nextSegFilename;}
	public byte[] getSegFamily(){return segFamily;}
	public int getTimescale(){return timescale;}
	public double getDuration(){return duration;}
	public ZonedDateTime getTimestamp(){return timestamp;}
	public String getTitle(){return title;}
	public String getMuxingApp(){return muxingApp;}
	public String getWritingApp(){return writingApp;}

	/*----- Setters -----*/

	public void setSegUUID(byte[] value){segUUID = value;}
	public void setFilename(String value){filename = value;}
	public void setPrevSegUUID(byte[] value){prevSegUUID = value;}
	public void setPrevSegFilename(String value){prevSegFilename = value;}
	public void setNextSegUUID(byte[] value){nextSegUUID = value;}
	public void setNextSegFilename(String value){nextSegFilename = value;}
	public void setSegFamily(byte[] value){segFamily = value;}
	public void setTimescale(int value){timescale = value;}
	public void setDuration(double value){duration = value;}
	public void setTimestamp(ZonedDateTime value){timestamp = value;}
	public void setTitle(String value){title = value;}
	public void setMuxingApp(String value){muxingApp = value;}
	public void setWritingApp(String value){writingApp = value;}
	
	/*----- Read -----*/
	
	/*----- Write -----*/
	
}
