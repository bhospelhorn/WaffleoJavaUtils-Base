package waffleoRai_soundbank.dls;

import java.io.IOException;

import waffleoRai_DataContainers.MultiValMap;
import waffleoRai_Files.RIFFReader.RIFFChunk;
import waffleoRai_Files.RIFFReader.RIFFList;
import waffleoRai_Sound.wav.WAVFormat;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.MultiFileBuffer;
import waffleoRai_soundbank.DLSFile;
import waffleoRai_soundbank.DLSFile.DLSID;

/*
 * (dlid)
 * fmt
 * data
 * (wsmp)
 * (INFO)
 */


public class DLSSample {
	
	/*----- Instance Variables -----*/
	
	private DLSID id;
	private WAVFormat fmt;
	private byte[] sampleData;
	
	//wsmp
	private boolean useWsmp = false;
	private short usUnityNote = 60;
	private short sFineTune;
	private int lGain;
	private int fulOptions;
	private DLSSampleLoop loop;
	
	/*----- Init -----*/
	
	private DLSSample(){}
	
	/*----- Getters -----*/
	
	public DLSID getID(){return id;}
	public WAVFormat getWaveFormatInfo(){return fmt;}
	public byte[] getData(){return sampleData;}
	public boolean hasWSMP(){return this.useWsmp;}
	public short getUnityNote(){return usUnityNote;}
	public short getFineTune(){return sFineTune;}
	public int getGain(){return lGain;}
	public int getOptions(){return fulOptions;}
	public DLSSampleLoop getLoop(){return loop;}
	
	/*----- Setters -----*/
	
	/*----- Read -----*/
	
	public static DLSSample read(RIFFChunk waveChunk) throws IOException{
		if(waveChunk == null) return null;
		if(!waveChunk.isList()) return null;
		
		String magic = waveChunk.getMagicNumber();
		if(!magic.equals(DLSFile.MAGIC_WAVE)) return null;
		
		RIFFList chunkList = waveChunk.getListContents();
		MultiValMap<String, RIFFChunk> listMap = chunkList.mapContentsById();
		DLSSample samp = new DLSSample();
		
		//fmt
		RIFFChunk child = listMap.getFirstValueWithKey(DLSFile.MAGIC_FMT);
		if(child == null) return null;
		BufferReference ptr = child.open();
		samp.fmt = WAVFormat.readFmtBlock(ptr);
		
		//data
		child = listMap.getFirstValueWithKey(DLSFile.MAGIC_DATA);
		if(child == null) return null;
		FileBuffer buff = child.openBuffer();
		if(buff != null){
			samp.sampleData = buff.getBytes(0, buff.getFileSize());
		}
		
		//dlid
		child = listMap.getFirstValueWithKey(DLSFile.MAGIC_DLID);
		if(child != null){
			samp.id = new DLSID();
			samp.id.readIn(child.open());
		}
		
		//wsmp
		child = listMap.getFirstValueWithKey(DLSFile.MAGIC_WSMP);
		if(child != null){
			samp.useWsmp = true;
			ptr = child.open();
			ptr.add(4); //cbSize
			samp.usUnityNote = ptr.nextShort();
			samp.sFineTune = ptr.nextShort();
			samp.lGain = ptr.nextInt();
			samp.fulOptions = ptr.nextInt();
			int loopcount = ptr.nextInt();
			if(loopcount == 1){
				samp.loop = DLSSampleLoop.read(ptr);
			}
		}
		
		return samp;
	}
	
	/*----- Write -----*/
	
	public boolean exportAsWAV(String path) throws IOException{
		if(fmt == null) return false;
		if(sampleData == null) return false;
		
		if(fmt.getCodecID() != WAVFormat.WAV_CODEC_PCM) return false;
		
		FileBuffer outbuff = new MultiFileBuffer(4);
		
		FileBuffer riffhdr = new FileBuffer(20, false); //Put fmt header here
		FileBuffer fmtchunk = fmt.serializeMe();
		FileBuffer datachunk = new FileBuffer(sampleData.length + 8, false);
		int total_sz = 4;
		
		datachunk.printASCIIToFile("data");
		datachunk.addToFile(sampleData.length);
		for(int i = 0; i < sampleData.length; i++) datachunk.addToFile(sampleData[i]);
		total_sz += datachunk.getFileSize();
		total_sz += fmtchunk.getFileSize() + 8;
		
		//smpl, if applicable
		FileBuffer smplchunk = null;
		if(useWsmp){
			int smplsize = (9*4) + 24;
			smplchunk = new FileBuffer(smplsize + 8, false);
			smplchunk.printASCIIToFile("smpl");
			smplchunk.addToFile(smplsize);
			smplchunk.addToFile(0); //Manufacturer 
			smplchunk.addToFile(0); //Product
			int sper = 1000000000/(fmt.getSamplesPerSecond()); //Sample period (time passed for one sample in ns)
			smplchunk.addToFile(sper);
			smplchunk.addToFile((int)usUnityNote); //Unity note
			smplchunk.addToFile((int)sFineTune); //Pitch Fraction
			smplchunk.addToFile(0); //SMPTE Format
			smplchunk.addToFile(0); //SMPTE Offset
			
			smplchunk.addToFile(1); //Loop count
			smplchunk.addToFile(0); //Sampler data
			
			if(loop != null){
				smplchunk.addToFile(0); //ID
				smplchunk.addToFile(0); //Type (Forward)
				smplchunk.addToFile(loop.getLoopStart());
				smplchunk.addToFile(loop.getLoopStart() + loop.getLoopLength());
				smplchunk.addToFile(0);
				smplchunk.addToFile(0);
			}
			else{
				smplchunk.addToFile(0); //ID
				smplchunk.addToFile(0); //Type (Forward)
				smplchunk.addToFile(0);
				smplchunk.addToFile(0);
				smplchunk.addToFile(0);
				smplchunk.addToFile(0);
			}
			
			total_sz += smplchunk.getFileSize();
		}
		
		riffhdr.printASCIIToFile("RIFF");
		riffhdr.addToFile(total_sz);
		riffhdr.printASCIIToFile("WAVE");
		riffhdr.printASCIIToFile("fmt ");
		riffhdr.addToFile((int)fmtchunk.getFileSize());
		
		outbuff.addToFile(riffhdr);
		outbuff.addToFile(fmtchunk);
		outbuff.addToFile(datachunk);
		if(smplchunk != null) outbuff.addToFile(smplchunk);
		
		outbuff.writeFile(path);
		
		return true;
	}
	
	public boolean exportAsAIFF(String path){
		//TODO
		return false;
	}

}
