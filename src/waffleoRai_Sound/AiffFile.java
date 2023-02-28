package waffleoRai_Sound;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;
import waffleoRai_Utils.MultiFileBuffer;

public class AiffFile {
	
	/*----- Constants -----*/
	
	public static final String MAGIC_FORM = "FORM";
	public static final String MAGIC_AIFF = "AIFF";
	
	public static final String MAGIC_COMM = "COMM";
	public static final String MAGIC_INST = "INST";
	public static final String MAGIC_SSND = "SSND";
	
	public static final short LOOPMODE_NONE = 0;
	public static final short LOOPMODE_FWD = 1;
	public static final short LOOPMODE_PINGPONG = 2;

	/*----- Instance Variables -----*/
	
	//COMM
	private short channelCount;
	private int frameCount = 0;
	private short bitDepth;
	private double sampleRate;
	
	//INST
	private boolean includeInst = false;
	private byte baseNote = 60;
	private byte detune = 0;
	private byte lowNote = 0;
	private byte highNote = 127;
	private byte lowVel = 0;
	private byte highVel = 127;
	private short gain = 0;
	
	private short loops_playMode = LOOPMODE_NONE;
	private short loops_start;
	private short loops_end;
	
	private short loopr_playMode = LOOPMODE_NONE;
	private short loopr_start;
	private short loopr_end;
	
	//SSND
	//Store data in memory?
	private List<float[][]> samples;
	private float[][] add_block;
	private int block_pos = 0;
	
	/*----- Init -----*/
	
	private AiffFile(){
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
	
	public short getChannelCount(){return channelCount;}
	public int getFrameCount(){return frameCount;}
	public short getBitDepth(){return bitDepth;}
	public double getSampleRate(){return sampleRate;}
	
	/*----- Setters -----*/
	
	public void setBitDepth(short val){bitDepth = val;}
	public void setSampleRate(double val){sampleRate = val;}
	
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
	
	public static AiffFile readAiff(FileBuffer inputData) throws UnsupportedFileTypeException{
		long pos = inputData.findString(0L, 4L, MAGIC_FORM);
		if(pos != 0L) throw new FileBuffer.UnsupportedFileTypeException("AiffFile.readAiff || FORM magic number not found!");
		
		pos = inputData.findString(8L, 12L, MAGIC_FORM);
		if(pos != 8L) throw new FileBuffer.UnsupportedFileTypeException("AiffFile.readAiff || AIFF magic number not found!");
		
		//From the AIFF header, read all other chunks
		Map<String, FileBuffer> chunkdata = new HashMap<String, FileBuffer>();
		long fsize = inputData.getFileSize();
		pos = 12L;
		while(pos < fsize){
			String chid = inputData.getASCII_string(pos, 4); pos += 4;
			int chsz = inputData.intFromFile(pos); pos += 4;
			FileBuffer chdat = inputData.createReadOnlyCopy(pos, pos+chsz);
			chunkdata.put(chid, chdat);
			pos += chsz;
		}
		
		//Read known blocks
		AiffFile file = new AiffFile();
		FileBuffer chunk = chunkdata.get("COMM");
		if(chunk == null) throw new FileBuffer.UnsupportedFileTypeException("AiffFile.readAiff || COMM chunk not found!");
		chunk.setCurrentPosition(0L);
		file.channelCount = chunk.nextShort();
		file.frameCount = chunk.nextInt();
		file.bitDepth = chunk.nextShort();
		try {
			file.sampleRate = AiffFile.readFloat80(chunk.createCopy(8, 18));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		chunk = chunkdata.get("SSND");
		if(chunk == null) throw new FileBuffer.UnsupportedFileTypeException("AiffFile.readAiff || SSND chunk not found!");
		float max = (float)(1 << (file.bitDepth - 1)) - 1;
		float[][] sample_dat = new float[file.channelCount][file.frameCount];
		int rawsmpl = 0;
		chunk.setCurrentPosition(8L);
		for(int f = 0; f < file.frameCount; f++){
			for(int c = 0; c < file.channelCount; c++){
				switch(file.bitDepth){
				case 8:
					rawsmpl = (int)chunk.nextByte();
					break;
				case 16:
					rawsmpl = (int)chunk.nextShort();
					break;
				case 24:
					rawsmpl = chunk.nextShortish();
					break;
				case 32:
					rawsmpl = chunk.nextInt();
					break;
				}
				sample_dat[c][f] = ((float)rawsmpl)/max;
			}
		}
		
		chunk = chunkdata.get("INST");
		if(chunk != null){
			chunk.setCurrentPosition(0L);
			file.baseNote = chunk.nextByte();
			file.detune = chunk.nextByte();
			file.lowNote = chunk.nextByte();
			file.highNote = chunk.nextByte();
			file.lowVel = chunk.nextByte();
			file.highVel = chunk.nextByte();
			file.gain = chunk.nextShort();
			
			file.loops_playMode = chunk.nextShort();
			file.loops_start = chunk.nextShort();
			file.loops_end = chunk.nextShort();
			file.loopr_playMode = chunk.nextShort();
			file.loopr_start = chunk.nextShort();
			file.loopr_end = chunk.nextShort();
			file.includeInst = true;
		}
		
		//Free all sub-buffers in the chunk map before returning
		for(FileBuffer ch : chunkdata.values()){
			try{ch.dispose();}
			catch(IOException ex){
				ex.printStackTrace();
			}
		}
		chunkdata.clear();
		
		return file;
	}
	
	/*----- Writing -----*/
	
 	public FileBuffer serializeCOMM(){
		FileBuffer out = new FileBuffer(26,true);
		out.printASCIIToFile(MAGIC_COMM);
		out.addToFile(18);
		
		out.addToFile(channelCount);
		out.addToFile(frameCount);
		out.addToFile(bitDepth);
		AiffFile.writeFloat80(sampleRate, out);
		
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
	
	public FileBuffer serializeSSND(){
		int sdat_size = frameCount * channelCount * (bitDepth >>> 3);
		FileBuffer out = new FileBuffer(sdat_size + 16, true);
		
		float max = (float)(1 << (bitDepth-1)) - 1;
		
		out.printASCIIToFile(MAGIC_SSND);
		out.addToFile(sdat_size+8);
		out.addToFile(0);
		out.addToFile(0);
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
		FileBuffer ssnd = serializeSSND();
		FileBuffer inst = null;
		if(includeInst) inst = serializeINST();
		
		size = (int)comm.getFileSize();
		size += (int)ssnd.getFileSize();
		if(inst != null){
			size += (int)inst.getFileSize();
		}
		header.printASCIIToFile(MAGIC_FORM);
		header.addToFile(size+4);
		header.printASCIIToFile(MAGIC_AIFF);
		
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
		
		double mantissa_f = (double)mantissa / (double)(1L << 63);
		return sign * mantissa_f * Math.pow(2.0, (double)(exp - 0x3fff));
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
	
}
