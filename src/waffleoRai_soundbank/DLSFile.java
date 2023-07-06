package waffleoRai_soundbank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import waffleoRai_Files.RIFFReader;
import waffleoRai_Files.RIFFReader.RIFFChunk;
import waffleoRai_Files.RIFFReader.RIFFList;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;
import waffleoRai_soundbank.dls.DLSInstrument;
import waffleoRai_soundbank.dls.DLSSample;

/*
 * (cdl)
 * (vers)
 * (dlid)
 * colh
 * lins
 * ptbl
 * wvpl
 * (INFO)
 */

public class DLSFile {
	
	/*----- Constants -----*/
	
	public static final String RIFF_ID = "DLS ";
	
	public static final String MAGIC_LINS = "lins"; //Inst list
	public static final String MAGIC_WVPL = "wvpl"; //Wave pool list
	public static final String MAGIC_LRGN = "lrgn"; //Instrument region list
	public static final String MAGIC_LART = "lart"; //Instrument articulator list (V1)
	public static final String MAGIC_LAR2 = "lar2"; //Instrument articulator list (V2)
	public static final String MAGIC_INS = "ins "; //Instrument (stored as a list because it contains lists)
	public static final String MAGIC_RGN = "rgn "; //V1 Region (stored as a list because it contains lists)
	public static final String MAGIC_RGN2 = "rgn2"; //V2 Region (stored as a list because it contains lists)
	
	public static final String MAGIC_COLH = "colh"; //Collection header
	public static final String MAGIC_PTBL = "ptbl"; //Pool table
	public static final String MAGIC_WAVE = "wave"; //Sound sample
	public static final String MAGIC_WSMP = "wsmp"; //Wave sample
	public static final String MAGIC_FMT = "fmt "; //Wave format
	public static final String MAGIC_DATA = "data"; //Wave data
	public static final String MAGIC_INSH = "insh"; //Instrument header
	public static final String MAGIC_RGNH = "rgnh"; //Region header
	public static final String MAGIC_WLNK = "wlnk"; //Wave link
	public static final String MAGIC_ART1 = "art1"; //Articulator V1
	public static final String MAGIC_ART2 = "art2"; //Articulator V2
	
	public static final String MAGIC_VERS = "vers"; //Version
	public static final String MAGIC_DLID = "dlid"; //GUID chunk
	public static final String MAGIC_CDL = "cdl "; //Conditional block
	public static final String MAGIC_INFO = "INFO";
	
	/*----- Inner Classes -----*/
	
	public static class DLSID{
		public int ulData1;
		public short usData2;
		public short usData3;
		public byte[] abData4;
		
		public DLSID(){abData4 = new byte[8];}
		
		public void readIn(BufferReference data){
			ulData1 = data.nextInt();
			usData2 = data.nextShort();
			usData3 = data.nextShort();
			for(int i = 0; i < 7; i++) abData4[i] = data.nextByte();
		}
	}
	
	/*----- Instance Variables -----*/
	
	private DLSID id; //Optional
	private Map<String, String> info;
	
	private ArrayList<DLSInstrument> instruments;
	private ArrayList<DLSSample> samples; //If these seem wonkily mapped, try loading in the pool table too
	
	private int[] poolTable;
	
	/*----- Init -----*/
	
	private DLSFile(){}
	
	/*----- Getters -----*/
	
	public DLSID getID(){return id;}
	
	public String getNameTag(){
		return getInfoTag("INAM");
	}
	
	public String getInfoTag(String key){
		if(info == null) return null;
		return info.get(key);
	}
	
	public int getInstrumentCount(){
		if(instruments == null) return 0;
		return instruments.size();
	}
	
	public int getSampleCount(){
		if(samples == null) return 0;
		return samples.size();
	}
	
	/*----- Setters -----*/
	
	/*----- Reader -----*/
	
	public static DLSFile readDLS(String path) throws IOException, UnsupportedFileTypeException{
		RIFFReader riff_rdr = RIFFReader.readFile(path, true);
		
		//colh - this pretty much only contains the intrument count
		RIFFChunk chunk = riff_rdr.getFirstTopLevelChunk(MAGIC_COLH);
		if(chunk == null){
			riff_rdr.clearDataCache();
			return null;
		}
		BufferReference ptr = chunk.open();
		int icount = ptr.nextInt();
		
		DLSFile dls = new DLSFile();
		dls.instruments = new ArrayList<DLSInstrument>(icount+1);
		
		//lins
		chunk = riff_rdr.getFirstTopLevelChunk(MAGIC_LINS);
		if(chunk == null || !chunk.isList()){
			riff_rdr.clearDataCache();
			return null;
		}
		RIFFList childList = chunk.getListContents();
		List<RIFFChunk> grandchildren = childList.getAllItems();
		for(RIFFChunk gc : grandchildren){
			DLSInstrument inst = DLSInstrument.read(gc);
			if(inst != null) dls.instruments.add(inst);
		}
		
		//ptbl
		Map<Integer, Integer> soffmap = new HashMap<Integer, Integer>();
		chunk = riff_rdr.getFirstTopLevelChunk(MAGIC_PTBL);
		if(chunk == null){
			riff_rdr.clearDataCache();
			return null;
		}
		ptr = chunk.open();
		ptr.add(4);
		int tsize = ptr.nextInt();
		dls.poolTable = new int[tsize];
		for(int i = 0; i < tsize; i++) {
			dls.poolTable[i] = ptr.nextInt();
			soffmap.put(dls.poolTable[i], i);
			dls.poolTable[i] = -1;
		}
		
		//wvpl
		chunk = riff_rdr.getFirstTopLevelChunk(MAGIC_WVPL);
		if(chunk == null || !chunk.isList()){
			riff_rdr.clearDataCache();
			return null;
		}
		childList = chunk.getListContents();
		grandchildren = childList.getAllItems();
		int pos = 0;
		int i = 0;
		for(RIFFChunk gc : grandchildren){
			DLSSample smpl = DLSSample.read(gc);
			dls.samples.add(smpl);
			
			Integer t = soffmap.get(pos);
			if(t != null){
				dls.poolTable[t] = i;
			}
			
			i++;
			pos += gc.getFullChunkSize();
		}
		
		//dlid
		chunk = riff_rdr.getFirstTopLevelChunk(MAGIC_DLID);
		if(chunk != null){
			dls.id = new DLSID();
			dls.id.readIn(chunk.open());
		}
		
		//INFO
		chunk = riff_rdr.getFirstTopLevelChunk(MAGIC_INFO);
		if(chunk != null && chunk.isList()){
			dls.info = RIFFReader.readINFOList(chunk);
		}
		
		riff_rdr.clearDataCache();
		
		return dls;
	}

}
