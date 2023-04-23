package waffleoRai_Sound.wav;

import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.MultiFileBuffer;

public class WAVFormat {
	
	/*----- Constants -----*/
	
	public static final int WAV_CODEC_PCM = 1;
	
	/*----- Instance Variables -----*/
	
	private short wFormatTag;
	private short wChannels;
	private int dwSamplesPerSec;
	private int dwAvgBytesPerSec;
	private short wBlockAlign;
	
	private WAVFormatSpecific codecInfo;
	
	/*----- Init -----*/
	
	/*----- Getters -----*/
	
	public short getCodecID(){return this.wFormatTag;}
	public short getChannelCount(){return wChannels;}
	public int getSamplesPerSecond(){return this.dwSamplesPerSec;}
	public int getAvgBytesPerSecond(){return this.dwAvgBytesPerSec;}
	public short getBlockAlign(){return this.wBlockAlign;}
	public WAVFormatSpecific getCodecSpecificInfo(){return codecInfo;}
	
	/*----- Setters -----*/
	
	/*----- Read -----*/
	
	public static WAVFormat readFmtBlock(BufferReference input){
		WAVFormat fmt = new WAVFormat();
		fmt.wFormatTag = input.nextShort();
		fmt.wChannels = input.nextShort();
		fmt.dwSamplesPerSec = input.nextInt();
		fmt.dwAvgBytesPerSec = input.nextInt();
		fmt.wBlockAlign = input.nextShort();
		
		//Type specific
		switch(fmt.wFormatTag){
		case WAVFormat.WAV_CODEC_PCM:
			WAVFmtSpecPCM pcmspec = new WAVFmtSpecPCM();
			pcmspec.readIn(input);
			fmt.codecInfo = pcmspec;
			break;
		default:
			fmt.codecInfo = null;
			break;
		}
		
		return fmt;
	}
	
	/*----- Write -----*/
	
	public FileBuffer serializeMe(){
		FileBuffer fmt = new MultiFileBuffer(2);
		FileBuffer main_chunk = new FileBuffer(16, false);
		
		main_chunk.addToFile(wFormatTag);
		main_chunk.addToFile(wChannels);
		main_chunk.addToFile(dwSamplesPerSec);
		main_chunk.addToFile(dwAvgBytesPerSec);
		main_chunk.addToFile(wBlockAlign);
		
		fmt.addToFile(main_chunk);
		
		if(codecInfo != null){
			fmt.addToFile(codecInfo.serializeMe());
		}
		
		return fmt;
	}

}
