package waffleoRai_Containers.media.matroska;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLFloatElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;

public class MatroskaTrackAudio {
	
	public static final int EBML_BASE_ID = 0x61;
	
	private static final int EBML_ID_SAMPFREQ = 0x35;
	private static final int EBML_ID_SAMPFREQ_OUT = 0x38b5;
	private static final int EBML_ID_CHANNELS = 0x1f;
	private static final int EBML_ID_BITDEPTH = 0x2264;
	private static final int EBML_ID_EMPHASIS = 0x12f1;
	
	private double samplingFrequency = Double.NaN;
	private double outputSampleFrequency = Double.NaN;
	private int channels = -1;
	private int bitDepth = -1;
	private int emphasis = -1;
	
	/*----- Getters -----*/

	public double getSamplingFrequency(){return samplingFrequency;}
	public double getOutputSampleFrequency(){return outputSampleFrequency;}
	public int getChannels(){return channels;}
	public int getBitDepth(){return bitDepth;}
	public int getEmphasis(){return emphasis;}

	/*----- Setters -----*/

	public void setSamplingFrequency(double value){samplingFrequency = value;}
	public void setOutputSampleFrequency(double value){outputSampleFrequency = value;}
	public void setChannels(int value){channels = value;}
	public void setBitDepth(int value){bitDepth = value;}
	public void setEmphasis(int value){emphasis = value;}
	
	/*----- Read -----*/
	
	public static MatroskaTrackAudio fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaTrackAudio item = new MatroskaTrackAudio();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_SAMPFREQ);
		if(e == null) return null;
		item.samplingFrequency = EBMLCommon.readFloatElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_CHANNELS);
		if(e == null) return null;
		item.channels = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_SAMPFREQ_OUT);
		item.outputSampleFrequency = EBMLCommon.readFloatElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_BITDEPTH);
		item.bitDepth = EBMLCommon.readIntElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_EMPHASIS);
		item.emphasis = EBMLCommon.readIntElement(e);
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(channels < 0) return null;
		if(Double.isNaN(samplingFrequency)) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLFloatElement(EBML_ID_SAMPFREQ, samplingFrequency, false));
		if(!Double.isNaN(outputSampleFrequency)) me.addChild(new EBMLFloatElement(EBML_ID_SAMPFREQ_OUT, outputSampleFrequency, false));
		me.addChild(new EBMLIntElement(EBML_ID_CHANNELS, channels, false));
		if(bitDepth >= 0) me.addChild(new EBMLIntElement(EBML_ID_BITDEPTH, bitDepth, false));
		if(emphasis >= 0) me.addChild(new EBMLIntElement(EBML_ID_EMPHASIS, emphasis, false));
		
		return me;
	}


}
