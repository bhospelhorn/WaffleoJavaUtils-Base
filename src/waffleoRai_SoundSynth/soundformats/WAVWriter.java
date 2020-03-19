package waffleoRai_SoundSynth.soundformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import waffleoRai_Sound.WAV;
import waffleoRai_SoundSynth.AudioSampleStream;
import waffleoRai_Utils.FileBuffer;

public class WAVWriter {
	
	private AudioSampleStream source;
	private int sampleRate;
	private int bitDepth;
	private int channelCount;
	
	private int byPerSample;

	private boolean closeme;
	private OutputStream output;
	
	private String temppath;
	private OutputStream temp_out;
	
	public WAVWriter(AudioSampleStream input, String outpath) throws IOException
	{
		output = new BufferedOutputStream(new FileOutputStream(outpath));
		closeme = true;
		construct(input);
	}
	
	public WAVWriter(AudioSampleStream input, OutputStream stream) throws IOException
	{
		output = stream;
		construct(input);
	}
	
	private void construct(AudioSampleStream input) throws IOException
	{
		source = input;
		sampleRate = (int)Math.round(source.getSampleRate());
		bitDepth = source.getBitDepth();
		byPerSample = bitDepth >>> 3;
		channelCount = source.getChannelCount();
		
		temppath = FileBuffer.generateTemporaryPath("wavwriter_data");
		temp_out = new BufferedOutputStream(new FileOutputStream(temppath));
	}
	
	private FileBuffer generateFMT()
	{
		int fmt_sz = 16;
		FileBuffer fmt = new FileBuffer(fmt_sz + 8, false);
		
		fmt.printASCIIToFile(WAV.MAG_FMT);
		fmt.addToFile(fmt_sz); //Chunk size. Fixed for now since only handle one format
		fmt.addToFile((short)1); //Compression code. Fixed for now since only handle one format
		fmt.addToFile((short)channelCount); //Number of channels
		fmt.addToFile(sampleRate); //Sample rate
		int ba = (bitDepth/8)/channelCount;
		int abps = sampleRate * ba;
		fmt.addToFile(abps);
		fmt.addToFile((short)ba);
		fmt.addToFile((short)bitDepth);
		
		return fmt;
	}
	
	private void writeSample(int sample) throws IOException
	{
		int s = sample;
		for(int i = 0; i < byPerSample; i++)
		{
			int b = s & 0xFF;
			temp_out.write(b);
			s = s >>> 8;
		}
	}
	
	public void write(int frames) throws InterruptedException, IOException
	{
		for(int i = 0; i < frames; i++)
		{
			//System.err.println("frame = " + i);
			int[] samps = source.nextSample();
			for(int c = 0; c < channelCount; c++){
				//System.err.println("channel = " + c);
				writeSample(samps[c]);
			}
		}
	}
	
	public void complete() throws IOException
	{
		temp_out.close();
		int datasize = (int)FileBuffer.fileSize(temppath);
		
		FileBuffer fmt = generateFMT();
		//Calculate total size
		int wavsize = (int)fmt.getFileSize() + datasize + 8;
		
		//RIFF header
		FileBuffer wavhead = new FileBuffer(12, false);
		wavhead.printASCIIToFile(WAV.MAG0);
		wavhead.addToFile(wavsize);
		wavhead.printASCIIToFile(WAV.MAG1);
		wavhead.writeToStream(output);
		
		//FMT
		fmt.writeToStream(output);
		
		//DATA
		FileBuffer datahead = new FileBuffer(8, false);
		datahead.printASCIIToFile(WAV.MAG_DATA);
		datahead.addToFile(datasize);
		datahead.writeToStream(output);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(temppath));
		int b = -1;
		while((b = bis.read()) != -1) output.write(b);
		bis.close();
		
		if(closeme) output.close();
		Files.deleteIfExists(Paths.get(temppath));
	}
	
}
