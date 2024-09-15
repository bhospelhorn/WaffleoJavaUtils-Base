package waffleoRai_Sound;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import waffleoRai_Files.AIFFReader;
import waffleoRai_Files.AIFFReader.AIFFChunk;
import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_SoundSynth.soundformats.PCMSampleStream;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;
import waffleoRai_Utils.MultiFileBuffer;

public class AiffFile implements RandomAccessSound {
	
	/*----- Constants -----*/
	
	public static final String MAGIC_FORM = "FORM";
	public static final String MAGIC_AIFF = "AIFF";
	public static final String MAGIC_AIFC = "AIFC";
	
	public static final String MAGIC_COMM = "COMM";
	public static final String MAGIC_INST = "INST";
	public static final String MAGIC_SSND = "SSND";
	public static final String MAGIC_APPL = "APPL";
	
	public static final short LOOPMODE_NONE = 0;
	public static final short LOOPMODE_FWD = 1;
	public static final short LOOPMODE_PINGPONG = 2;
	
	private static final float MAX_16 = 0x7fff;
	private static final float MAX_24 = 0x7fffff;

	/*----- Instance Variables -----*/
	
	protected AIFFReader reader; //Store reference to additional chunks.
	
	//COMM
	protected short channelCount;
	protected int frameCount = 0;
	protected short bitDepth;
	protected double sampleRate;
	protected int compressionId = 0;
	protected String compressionName = null;
	
	//INST
	protected boolean includeInst = false;
	protected byte baseNote = 60;
	protected byte detune = 0;
	protected byte lowNote = 0;
	protected byte highNote = 127;
	protected byte lowVel = 0;
	protected byte highVel = 127;
	protected short gain = 0;
	
	protected short loops_playMode = LOOPMODE_NONE;
	protected short loops_start;
	protected short loops_end;
	
	protected short loopr_playMode = LOOPMODE_NONE;
	protected short loopr_start;
	protected short loopr_end;
	
	//SSND
	//Store data in memory?
	private List<float[][]> samples;
	private float[][] add_block;
	private int block_pos = 0;
	
	private float[][] bookmark_block; //Random access
	private int bookmark_start = 0;
	
	private byte[] comprData; //Compressed raw data
	
	/*----- Init -----*/
	
	protected AiffFile(){
		samples = new LinkedList<float[][]>();
	}
	
	public static AiffFile emptyAiffFile(int channels){
		AiffFile aiff = new AiffFile();
		aiff.channelCount = (short)channels;
		aiff.bitDepth = 16;
		aiff.sampleRate = 44100.0;
		return aiff;
	}
	
	/*----- Getters -----*/
	
	public boolean isCompressed(){return compressionId != 0;}
	public short getChannelCount(){return channelCount;}
	public int getFrameCount(){return frameCount;}
	public short getRawBitDepth(){return bitDepth;}
	public double getRawSampleRate(){return sampleRate;}
	public int getCompressionId(){return compressionId;}
	public String getCompressionName(){return compressionName;}
	public boolean hasSustainLoop(){return loops_playMode != LOOPMODE_NONE;}
	public boolean hasReleaseLoop(){return loopr_playMode != LOOPMODE_NONE;}
	public int getSustainLoopMode(){return loops_playMode;}
	public int getReleaseLoopMode(){return loopr_playMode;}
	public int getSustainLoopStart(){return loops_start;}
	public int getSustainLoopEnd(){return loops_end;}
	public int getReleaseLoopStart(){return loopr_start;}
	public int getReleaseLoopEnd(){return loopr_end;}
	
	public float[] getSamples(int channel){
		if(frameCount < 1) return null;
		float[] samps = new float[frameCount];
		
		int i = 0;
		for(float[][] block : samples){
			int bsize = block[channel].length;
			for(int j = 0; j < bsize; j++){
				samps[i++] = block[channel][j];
			}
		}
		
		if(add_block != null){
			for(int j = 0; j < block_pos; j++){
				samps[i++] = add_block[channel][j];
			}
		}
		
		return samps;
	}
	
	public byte[] getAifcRawSndData() {
		return comprData;
	}
	
	/*----- Setters -----*/
	
	public void setBitDepth(short val){bitDepth = val;}
	public void setSampleRate(double val){sampleRate = val;}
	public void setCompressionId(int val) {compressionId = val;}
	public void setCompressionName(String val) {compressionName = val;}
	
	public void setAifcRawSndData(byte[] val) {comprData = val;}
	public void setFrameCountDirect(int val) {frameCount = val;}
	
	public void setSustainLoop(int mode, int start, int end){
		includeInst = true;
		this.loops_playMode = (short)mode;
		this.loops_start = (short)start;
		this.loops_end = (short)end;
	}
	
	public void setReleaseLoop(int mode, int start, int end){
		includeInst = true;
		this.loopr_playMode = (short)mode;
		this.loopr_start = (short)start;
		this.loopr_end = (short)end;
	}
	
	public int allocateFrames(int amt){
		if(amt < 1) return 0;
		if(add_block != null){
			int sz = add_block[0].length - block_pos;
			if(sz > 0) return sz;
			block_pos = 0;
			samples.add(add_block);
			add_block = new float[channelCount][amt];
			return amt;
		}
		else{
			block_pos = 0;
			add_block = new float[channelCount][amt];
			return amt;
		}
	}
	
	public boolean addFrame(int[] rawVal){
		if(rawVal == null) return false;
		if(rawVal.length != channelCount) return false;
		
		if(add_block != null){
			if(block_pos >= add_block[0].length){
				samples.add(add_block);
				//Allocate 1 second worth...
				add_block = new float[channelCount][(int)Math.ceil(sampleRate)];
				block_pos = 0;
			}
		}
		else{
			add_block = new float[channelCount][(int)Math.ceil(sampleRate)];
			block_pos = 0;
		}
		
		float max = (float)(1 << (bitDepth - 1)) - 1;
		for(int c = 0; c < rawVal.length; c++){
			add_block[c][block_pos] = ((float)rawVal[c])/max;
		}
		block_pos++;
		frameCount++;
		
		return true;
	}
	
	public void dispose(){
		samples.clear();
		add_block = null;
		block_pos = 0;
		frameCount = 0;
	}
	
	/*----- Reading -----*/
	
	protected void readCOMM(BufferReference data){
		channelCount = data.nextShort();
		frameCount = data.nextInt();
		bitDepth = data.nextShort();
		
		FileBuffer buffer = new FileBuffer(10, true);
		for(int i = 0; i < 10; i++) buffer.addToFile(data.nextByte());
		sampleRate = AiffFile.readFloat80(buffer);
		
		//Compression, if AIFC
		if(reader.getAIFFFileType().equals(MAGIC_AIFC)){
			compressionId = data.nextInt();
			int strsize = data.nextByte();
			compressionName = data.nextASCIIString(strsize);
		}
		else{
			compressionId = 0;
			compressionName = null;
		}
	}
	
	protected void readSSND(BufferReference data, int cSize){
		if(compressionId != 0) {
			//Read raw
			data.add(8);
			comprData = new byte[cSize-8];
			int i = 0;
			while(data.hasRemaining()) comprData[i++] = data.nextByte();
		}
		else {
			float max = (float)(1 << (bitDepth - 1)) - 1;
			float[][] sample_dat = new float[channelCount][frameCount];
			int rawsmpl = 0;
			for(int f = 0; f < frameCount; f++){
				for(int c = 0; c < channelCount; c++){
					switch(bitDepth){
					case 8:
						rawsmpl = (int)data.nextByte();
						break;
					case 16:
						rawsmpl = (int)data.nextShort();
						break;
					case 24:
						rawsmpl = data.next24Bits();
						break;
					case 32:
						rawsmpl = data.nextInt();
						break;
					}
					sample_dat[c][f] = ((float)rawsmpl)/max;
				}
			}
		}	
	}
	
	protected void readINST(BufferReference data){
		baseNote = data.nextByte();
		detune = data.nextByte();
		lowNote = data.nextByte();
		highNote = data.nextByte();
		lowVel = data.nextByte();
		highVel = data.nextByte();
		gain = data.nextShort();
		
		loops_playMode = data.nextShort();
		loops_start = data.nextShort();
		loops_end = data.nextShort();
		loopr_playMode = data.nextShort();
		loopr_start = data.nextShort();
		loopr_end = data.nextShort();
		includeInst = true;
	}
	
	public static AiffFile readAiff(FileBuffer inputData) throws UnsupportedFileTypeException, IOException{
		if(inputData == null) return null;
		return readAiff(inputData.getReferenceAt(0L));
	}
	
	public static AiffFile readAiff(BufferReference inputData) throws UnsupportedFileTypeException, IOException{
		if(inputData == null) return null;
		AiffFile aiff = new AiffFile();
		aiff.reader = AIFFReader.readFile(inputData, true);
		if((!aiff.reader.getAIFFFileType().equals(MAGIC_AIFF)) && (!aiff.reader.getAIFFFileType().equals(MAGIC_AIFC))){
			throw new FileBuffer.UnsupportedFileTypeException("AiffFile.readAiff || File form type not recognized!");
		}
		
		//Look for COMM (throw if not found)
		AIFFChunk chunk = aiff.reader.getFirstTopLevelChunk(MAGIC_COMM);
		if(chunk == null) throw new FileBuffer.UnsupportedFileTypeException("AiffFile.readAiff || COMM chunk not found!");
		aiff.readCOMM(chunk.open());
		chunk.clearCache();
		
		//Look for SSND (throw if not found)
		chunk = aiff.reader.getFirstTopLevelChunk(MAGIC_SSND);
		if(chunk == null) throw new FileBuffer.UnsupportedFileTypeException("AiffFile.readAiff || SSND chunk not found!");
		aiff.readSSND(chunk.open(), chunk.getDataSize());
		chunk.clearCache();
		
		//Look for INST
		chunk = aiff.reader.getFirstTopLevelChunk(MAGIC_INST);
		if(chunk != null){
			aiff.readINST(chunk.open());
			chunk.clearCache();
		}
		
		aiff.reader.clearDataCache();
		
		return aiff;
	}
	
	/*----- Writing -----*/
	
 	public FileBuffer serializeCOMM(){
 		int alloc = 26;
 		if(compressionId != 0){
 			alloc += 4;
 			if(compressionName != null) alloc += compressionName.length() + 2;
 		}
 		
 		
		FileBuffer out = new FileBuffer(alloc,true);
		out.printASCIIToFile(MAGIC_COMM);
		out.addToFile(18);
		
		out.addToFile(channelCount);
		out.addToFile(frameCount);
		out.addToFile(bitDepth);
		AiffFile.writeFloat80(sampleRate, out);
		
		//Compressed?
		if(compressionId != 0){
			out.addToFile(compressionId);
			if(compressionName != null){
				int strlen = compressionName.length();
				out.addToFile((byte)strlen);
				out.printASCIIToFile(compressionName);
				if(strlen % 2 == 0) out.addToFile((byte)0);
			}
			else{
				out.addToFile((short)0);
			}
			
			//Update size as well
			out.replaceInt((int)(out.getFileSize() - 8), 4L);
		}
		
		return out;
	}
	
	public FileBuffer serializeINST(){
		FileBuffer out = new FileBuffer(28, true);
		out.printASCIIToFile(MAGIC_INST);
		out.addToFile(20);
		
		out.addToFile(baseNote);
		out.addToFile(detune);
		out.addToFile(lowNote);
		out.addToFile(highNote);
		out.addToFile(lowVel);
		out.addToFile(highVel);
		out.addToFile(gain);
		
		out.addToFile(loops_playMode);
		out.addToFile(loops_start);
		out.addToFile(loops_end);
		
		out.addToFile(loopr_playMode);
		out.addToFile(loopr_start);
		out.addToFile(loopr_end);
		
		return out;
	}
	
	public FileBuffer serializeSSNDCompr(){
		if(comprData == null) {
			FileBuffer out = new FileBuffer(16, true);
			out.printASCIIToFile(MAGIC_SSND);
			out.addToFile(8);
			out.addToFile(0);
			out.addToFile(0);
			return out;
		}
		
		int sdat_size = comprData.length;
		FileBuffer out = new FileBuffer(sdat_size + 16, true);
		
		out.printASCIIToFile(MAGIC_SSND);
		out.addToFile(sdat_size+8);
		out.addToFile(0); //Offset (usually 0 anyway)
		out.addToFile(0); //Block size (usually 0 anyway)
		
		for(int i = 0; i < comprData.length; i++) out.addToFile(comprData[i]);
		
		return out;
	}
	
	public FileBuffer serializeSSND(){
		int sdat_size = frameCount * channelCount * (bitDepth >>> 3);
		FileBuffer out = new FileBuffer(sdat_size + 16, true);
		
		float max = (float)(1 << (bitDepth-1)) - 1;
		
		out.printASCIIToFile(MAGIC_SSND);
		out.addToFile(sdat_size+8);
		out.addToFile(0); //Offset (usually 0 anyway)
		out.addToFile(0); //Block size (usually 0 anyway)
		for(float[][] smplblock : samples){
			int fcount = smplblock[0].length;
			for(int f = 0; f < fcount; f++){
				for(int c = 0; c < channelCount; c++){
					int sample = Math.round(smplblock[c][f] * max);
					switch(bitDepth){
					case 8:
						out.addToFile((byte)sample);
						break;
					case 16:
						out.addToFile((short)sample);
						break;
					case 24:
						out.add24ToFile(sample);
						break;
					case 32:
						out.addToFile(sample);
						break;
					}
				}
			}
		}
		
		if(add_block != null){
			for(int f = 0; f < block_pos; f++){
				for(int c = 0; c < channelCount; c++){
					int sample = Math.round(add_block[c][f] * max);
					switch(bitDepth){
					case 8:
						out.addToFile((byte)sample);
						break;
					case 16:
						out.addToFile((short)sample);
						break;
					case 24:
						out.add24ToFile(sample);
						break;
					case 32:
						out.addToFile(sample);
						break;
					}
				}
			}
		}
		
		return out;
	}
	
	public FileBuffer serializeAiff(){
		int size = 0;
		FileBuffer out = new MultiFileBuffer(4);
		FileBuffer header = new FileBuffer(12, true);
		
		FileBuffer comm = serializeCOMM();
		
		FileBuffer ssnd = null;
		if(this.isCompressed()) ssnd = serializeSSNDCompr();
		else ssnd = serializeSSND();
		
		FileBuffer inst = null;
		if(includeInst) inst = serializeINST();
		
		size = (int)comm.getFileSize();
		size += (int)ssnd.getFileSize();
		if(inst != null){
			size += (int)inst.getFileSize();
		}
		header.printASCIIToFile(MAGIC_FORM);
		header.addToFile(size+4);
		if(this.isCompressed()) header.printASCIIToFile(MAGIC_AIFC);
		else header.printASCIIToFile(MAGIC_AIFF);
		
		out.addToFile(header);
		out.addToFile(comm);
		out.addToFile(ssnd);
		if(includeInst) out.addToFile(inst);
		
		return out;
	}
	
	/*----- Static Utils -----*/
	
 	public static double readFloat80(FileBuffer data){
		int exp = data.nextShort();
		long mantissa = data.nextLong();
		double sign = 1.0;
		if((exp & 0x8000) != 0) sign = -1.0;
		exp &= 0x7fff;
		if(exp == 0 && mantissa == 0) return (0.0 * sign);
		if(exp == 0 || exp == 0x7fff){
			System.err.println("AiffFile.readFloat80 || Value is infinity or denormal. Returning NaN.");
			return Double.NaN;
		}
		
		mantissa &= ~(1L << 63);
		//double mantissa_f = (double) mantissa;
		double mantissa_f = (double)mantissa / (double)~(1L << 63);
		return sign * (mantissa_f + 1.0) * Math.pow(2.0, (double)(exp - 0x3fff));
	}
	
	public static boolean writeFloat80(double value, FileBuffer data){
		long dbl_bits = Double.doubleToRawLongBits(value);
		int sign = 0;
		if((dbl_bits & (1L << 63)) != 0) sign = 1;
		if(value == 0.0){
			if(sign != 0){
				data.addToFile((byte)0x80);
				for(int i = 0; i < 9; i++) data.addToFile(FileBuffer.ZERO_BYTE);
				return true;
			}
			else{
				for(int i = 0; i < 10; i++) data.addToFile(FileBuffer.ZERO_BYTE);
				return true;
			}
		}
		dbl_bits &= ~(1L << 63);
		long exp = dbl_bits >>> 52;
		if(exp == 0 || exp == 0x7ff){
			System.err.println("AiffFile.writeFloat80 || Value is infinity or denormal. Returning without writing...");
			return false;
		}
		exp -= 1023;
		long mbits = dbl_bits & ((1L << 52) - 1);
		exp += 0x3fff;
		exp |= sign << 15;
		mbits = (1L << 63) | (mbits << (63-52));
		data.addToFile((short)exp);
		data.addToFile(mbits);
		return true;
	}
	
	/*----- Sound Interface -----*/
	
	public AudioFormat getFormat() {
		AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float)sampleRate, 
				bitDepth, 
				channelCount, (bitDepth/8) * channelCount,
				(float)sampleRate, true);
		return fmt;
	}
	
	public AudioInputStream getStream(){
		//TODO cbf
		return null;
	}
	
	public AudioSampleStream createSampleStream(){
		return new PCMSampleStream(this);
	}
	
	public AudioSampleStream createSampleStream(boolean loop){
		return new PCMSampleStream(this, loop);
	}

	public void setActiveTrack(int tidx) {}
	public int countTracks() {return 1;}
	
	public int totalFrames() {return frameCount;}
	public int totalChannels() {return channelCount;}
	
	public Sound getSingleChannel(int channel){
		AiffFile copy = new AiffFile();
		copy.channelCount = 1;
		copy.bitDepth = this.bitDepth;
		copy.sampleRate = this.sampleRate;
		copy.compressionId = this.compressionId;
		copy.compressionName = this.compressionName;
		copy.includeInst = this.includeInst;
		copy.baseNote = this.baseNote;
		copy.detune = this.detune;
		copy.lowNote = this.lowNote;
		copy.highNote = this.highNote;
		copy.lowVel = this.lowVel;
		copy.highVel = this.highVel;
		copy.gain = this.gain;
		copy.loops_playMode = this.loops_playMode;
		copy.loops_start = this.loops_start;
		copy.loops_end = this.loops_end;
		copy.loopr_playMode = this.loopr_playMode;
		copy.loopr_start = this.loopr_start;
		copy.loopr_end = this.loopr_end;
		
		copy.allocateFrames(this.frameCount);
		for(float[][] block : samples){
			int bsize = block[channel].length;
			for(int j = 0; j < bsize; j++){
				copy.addFrame(new int[] {(int)block[channel][j]});
			}
		}
		
		if(add_block != null){
			for(int j = 0; j < block_pos; j++){
				copy.addFrame(new int[] {(int)add_block[channel][j]});
			}
		}
		return copy;
	}
	
	public int[] getRawSamples(int channel){
		if(frameCount < 1) return null;
		int[] samps = new int[frameCount];

		int i = 0;
		for(float[][] block : samples){
			int bsize = block[channel].length;
			for(int j = 0; j < bsize; j++){
				samps[i++] = Math.round(block[channel][j]);
			}
		}
		
		if(add_block != null){
			for(int j = 0; j < block_pos; j++){
				samps[i++] = Math.round(add_block[channel][j]);
			}
		}
		
		return samps;
	}
	
	public int[] getSamples_16Signed(int channel){
		if(frameCount < 1) return null;
		int[] samps = new int[frameCount];

		int i = 0;
		for(float[][] block : samples){
			int bsize = block[channel].length;
			for(int j = 0; j < bsize; j++){
				samps[i++] = Math.round(block[channel][j] * MAX_16);
			}
		}
		
		if(add_block != null){
			for(int j = 0; j < block_pos; j++){
				samps[i++] = Math.round(add_block[channel][j] * MAX_16);
			}
		}
		
		return samps;
	}
	
	public int[] getSamples_24Signed(int channel){
		if(frameCount < 1) return null;
		int[] samps = new int[frameCount];

		int i = 0;
		for(float[][] block : samples){
			int bsize = block[channel].length;
			for(int j = 0; j < bsize; j++){
				samps[i++] = Math.round(block[channel][j] * MAX_24);
			}
		}
		
		if(add_block != null){
			for(int j = 0; j < block_pos; j++){
				samps[i++] = Math.round(add_block[channel][j] * MAX_24);
			}
		}
		
		return samps;
	}
	
	public BitDepth getBitDepth(){
		switch(bitDepth){
		case 8: return BitDepth.EIGHT_BIT_UNSIGNED;
		case 16: return BitDepth.SIXTEEN_BIT_SIGNED;
		case 24: return BitDepth.TWENTYFOUR_BIT_SIGNED;
		case 32: return BitDepth.THIRTYTWO_BIT_SIGNED;
		}
		return null;
	}
	
	public int getSampleRate(){return (int)sampleRate;}
	
	public boolean loops(){return loops_playMode != LOOPMODE_NONE;}
	public int getLoopFrame(){return loops_start;}
	public int getLoopEndFrame(){return loops_end;}
	
	public int getUnityNote(){return baseNote;}
	public int getFineTune(){return detune;}
	
	public int getSample(int channel, int frame){
		if(channel < 0) return 0;
		if(frame < 0) return 0;
		if(channel >= this.channelCount) return 0;
		if(frame >= this.frameCount) return 0;
		
		if(bookmark_block != null) {
			//In range?
			if((frame >= bookmark_start) && (frame < (bookmark_start + bookmark_block[0].length))) {
				return Math.round(bookmark_block[channel][frame - bookmark_start]);
			}
		}
		
		//Cache miss.
		int f = 0;
		for(float[][] block : samples){
			int bsize = block[channel].length;
			if(frame >= f) {
				if(frame < (f + bsize)) {
					bookmark_block = block;
					bookmark_start = f;
					return Math.round(bookmark_block[channel][frame - bookmark_start]);
				}
			}
			f += bsize;
		}
		
		return 0;
	}
	
	public byte[] frame2Bytes(int frame){
		int fSize = channelCount * (bitDepth >>> 3);
		FileBuffer buff = new FileBuffer(fSize, true);
		for(int c = 0; c < channelCount; c++) {
			int samp = getSample(c, frame);
			switch(bitDepth) {
			case 8:
				buff.addToFile((byte)samp);
				break;
			case 16:
				buff.addToFile((short)samp);
				break;
			case 24:
				buff.add24ToFile(samp);
				break;
			case 32:
				buff.addToFile(samp);
				break;
			}
		}

		return buff.getBytes();
	}
	
}
