package waffleoRai_Containers.media.matroska;

import java.util.List;

public class MatroskaTrack {
	
	public static final int TYPE_VIDEO = 1;
	public static final int TYPE_AUDIO = 2;
	public static final int TYPE_COMPLEX = 3;
	public static final int TYPE_LOGO = 0x10;
	public static final int TYPE_SUBTITLE = 0x11;
	public static final int TYPE_BUTTONS = 0x12;
	public static final int TYPE_CONTROL = 0x20;
	public static final int TYPE_METADATA = 0x21;
	
	private int trackNumber;
	private int trackUID;
	private int trackType;
	
	private boolean flagEnabled;
	private boolean flagDefault;
	private boolean flagForced;
	private boolean flagHearingImpaired;
	private boolean flagVisualImpaired;
	private boolean flagTextDescriptions;
	private boolean flagOriginal;
	private boolean flagCommentary;
	private boolean flagLacing;
	
	private int defaultDuration;
	private int defaultDecodedFieldDuration;
	private int maxBlockAddID;
	
	private List<MatroskaBlockAddMapping> blockAdditionMappings;
	
	private String name;
	private String lan;
	private String lanBCP47;
	private String codecID;
	private byte[] codecPrivate;
	private String codecName;
	private int codecDelay;
	private int seekPreRoll;
	
	private List<MatroskaTrackTranslate> trackTranslate;
	
	private MatroskaTrackVideo videoInfo;
	private MatroskaTrackAudio audioInfo;
	
	private MatroskaTrackOperation operation;
	private List<MatroskaContentEncoding> contentEncodings;

	/*----- Getters -----*/

	public int getTrackNumber(){return trackNumber;}
	public int getTrackUID(){return trackUID;}
	public int getTrackType(){return trackType;}
	public boolean getFlagEnabled(){return flagEnabled;}
	public boolean getFlagDefault(){return flagDefault;}
	public boolean getFlagForced(){return flagForced;}
	public boolean getFlagHearingImpaired(){return flagHearingImpaired;}
	public boolean getFlagVisualImpaired(){return flagVisualImpaired;}
	public boolean getFlagTextDescriptions(){return flagTextDescriptions;}
	public boolean getFlagOriginal(){return flagOriginal;}
	public boolean getFlagCommentary(){return flagCommentary;}
	public boolean getFlagLacing(){return flagLacing;}
	public int getDefaultDuration(){return defaultDuration;}
	public int getDefaultDecodedFieldDuration(){return defaultDecodedFieldDuration;}
	public int getMaxBlockAddID(){return maxBlockAddID;}
	public List<MatroskaBlockAddMapping> getBlockAdditionMappings(){return blockAdditionMappings;}
	public String getName(){return name;}
	public String getLan(){return lan;}
	public String getLanBCP47(){return lanBCP47;}
	public String getCodecID(){return codecID;}
	public byte[] getCodecPrivate(){return codecPrivate;}
	public String getCodecName(){return codecName;}
	public int getCodecDelay(){return codecDelay;}
	public int getSeekPreRoll(){return seekPreRoll;}
	public List<MatroskaTrackTranslate> getTrackTranslate(){return trackTranslate;}
	public MatroskaTrackVideo getVideoInfo(){return videoInfo;}
	public MatroskaTrackAudio getAudioInfo(){return audioInfo;}
	public MatroskaTrackOperation getOperation(){return operation;}
	public List<MatroskaContentEncoding> getContentEncodings(){return contentEncodings;}

	/*----- Setters -----*/

	public void setTrackNumber(int value){trackNumber = value;}
	public void setTrackUID(int value){trackUID = value;}
	public void setTrackType(int value){trackType = value;}
	public void setFlagEnabled(boolean value){flagEnabled = value;}
	public void setFlagDefault(boolean value){flagDefault = value;}
	public void setFlagForced(boolean value){flagForced = value;}
	public void setFlagHearingImpaired(boolean value){flagHearingImpaired = value;}
	public void setFlagVisualImpaired(boolean value){flagVisualImpaired = value;}
	public void setFlagTextDescriptions(boolean value){flagTextDescriptions = value;}
	public void setFlagOriginal(boolean value){flagOriginal = value;}
	public void setFlagCommentary(boolean value){flagCommentary = value;}
	public void setFlagLacing(boolean value){flagLacing = value;}
	public void setDefaultDuration(int value){defaultDuration = value;}
	public void setDefaultDecodedFieldDuration(int value){defaultDecodedFieldDuration = value;}
	public void setMaxBlockAddID(int value){maxBlockAddID = value;}
	public void setBlockAdditionMappings(List<MatroskaBlockAddMapping> value){blockAdditionMappings = value;}
	public void setName(String value){name = value;}
	public void setLan(String value){lan = value;}
	public void setLanBCP47(String value){lanBCP47 = value;}
	public void setCodecID(String value){codecID = value;}
	public void setCodecPrivate(byte[] value){codecPrivate = value;}
	public void setCodecName(String value){codecName = value;}
	public void setCodecDelay(int value){codecDelay = value;}
	public void setSeekPreRoll(int value){seekPreRoll = value;}
	public void setTrackTranslate(List<MatroskaTrackTranslate> value){trackTranslate = value;}
	public void setVideoInfo(MatroskaTrackVideo value){videoInfo = value;}
	public void setAudioInfo(MatroskaTrackAudio value){audioInfo = value;}
	public void setOperation(MatroskaTrackOperation value){operation = value;}
	public void setContentEncodings(List<MatroskaContentEncoding> value){contentEncodings = value;}
	
}
