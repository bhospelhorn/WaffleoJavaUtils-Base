package waffleoRai_soundbank.dls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import waffleoRai_DataContainers.MultiValMap;
import waffleoRai_Files.RIFFReader;
import waffleoRai_Files.RIFFReader.RIFFChunk;
import waffleoRai_Files.RIFFReader.RIFFList;
import waffleoRai_Utils.BufferReference;
import waffleoRai_soundbank.DLSFile;
import waffleoRai_soundbank.DLSFile.DLSID;

/*
 * (dlid)
 * insh
 * lrgn
 * (lart)
 * (lar2)
 * (INFO)
 */

public class DLSInstrument {
	
	/*----- Instance Variables -----*/
	
	private DLSID id; //Optional
	private Map<String, String> info;
	
	private int ulBank; //Program index
	private int ulInstrument; //Program index
	private ArrayList<DLSRegion> regions;
	private ArrayList<DLSArticulator> globalArt;
	
	/*----- Init -----*/
	
	private DLSInstrument(){this(4,4);}
	
	private DLSInstrument(int regAlloc, int artAlloc){
		regions = new ArrayList<DLSRegion>(regAlloc);
		globalArt = new ArrayList<DLSArticulator>(artAlloc);
	}
	
	/*----- Getters -----*/
	
	public String getNameTag(){
		return getInfoTag("INAM");
	}
	
	public String getInfoTag(String key){
		if(info == null) return null;
		return info.get(key);
	}
	
	public int getRegionCount(){
		if(regions == null) return 0;
		return regions.size();
	}
	
	public int getArticulatorCount(){
		if(globalArt == null) return 0;
		return globalArt.size();
	}
	
	public int getBankIndex(){
		return ulBank;
	}
	
	public int getInstrumentIndex(){
		return ulInstrument;
	}
	
	public List<DLSRegion> getAllRegions(){
		if(regions == null || regions.isEmpty()) return new LinkedList<DLSRegion>();
		ArrayList<DLSRegion> copy = new ArrayList<DLSRegion>(regions.size());
		copy.addAll(regions);
		return copy;
	}
	
	public List<DLSArticulator> getAllGlobalArticulators(){
		if(globalArt == null || globalArt.isEmpty()) return new LinkedList<DLSArticulator>();
		ArrayList<DLSArticulator> copy = new ArrayList<DLSArticulator>(globalArt.size());
		copy.addAll(globalArt);
		return copy;
	}
	
	/*----- Readers -----*/
	
	private void readHeader(BufferReference data){
		if(data == null) return;
		int rcount = data.nextInt();
		ulBank = data.nextInt();
		ulInstrument = data.nextInt();
		
		if(rcount > 0) regions = new ArrayList<DLSRegion>(rcount);
	}
	
	public static DLSInstrument read(RIFFChunk insChunk) throws IOException{
		if(insChunk == null) return null;
		if(!insChunk.isList()) return null;
		
		String magic = insChunk.getMagicNumber();
		if(!magic.equals(DLSFile.MAGIC_INS)) return null;
		
		RIFFList chunkList = insChunk.getListContents();
		MultiValMap<String, RIFFChunk> listMap = chunkList.mapContentsById();
		DLSInstrument inst = new DLSInstrument();
		
		//insh
		RIFFChunk child = listMap.getFirstValueWithKey(DLSFile.MAGIC_INSH);
		if(child == null) return null;
		BufferReference ptr = child.open();
		inst.readHeader(ptr);
		
		//lrgn
		child = listMap.getFirstValueWithKey(DLSFile.MAGIC_LRGN);
		if(child == null) return null;
		if(!child.isList()) return null;
		RIFFList childList = child.getListContents();
		List<RIFFChunk> grandchildren = childList.getAllItems();
		for(RIFFChunk gc : grandchildren){
			if(gc.getMagicNumber().equals(DLSFile.MAGIC_RGN) || gc.getMagicNumber().equals(DLSFile.MAGIC_RGN2)){
				DLSRegion reg = DLSRegion.read(gc);
				if(reg != null) inst.regions.add(reg);
			}
		}
		
		//dlid
		child = listMap.getFirstValueWithKey(DLSFile.MAGIC_DLID);
		if(child != null){
			inst.id = new DLSID();
			inst.id.readIn(child.open());
		}
		
		//lart
		child = listMap.getFirstValueWithKey(DLSFile.MAGIC_LART);
		if(child != null && child.isList()){
			RIFFList artList = child.getListContents();
			List<RIFFChunk> artChunks = artList.getAllItems();
			inst.globalArt = new ArrayList<DLSArticulator>(artChunks.size()+1);
			
			for(RIFFChunk artChunk : artChunks){
				if(!artChunk.getMagicNumber().equals(DLSFile.MAGIC_ART1)) continue;
				ptr = artChunk.open();
				DLSArticulator artobj = DLSArticulator.read(ptr, 1);
				if(artobj != null){
					inst.globalArt.add(artobj);
				}
			}
		}
		
		//lar2
		child = listMap.getFirstValueWithKey(DLSFile.MAGIC_LAR2);
		if(child != null && child.isList()){
			RIFFList artList = child.getListContents();
			List<RIFFChunk> artChunks = artList.getAllItems();
			inst.globalArt = new ArrayList<DLSArticulator>(artChunks.size()+1);
			
			for(RIFFChunk artChunk : artChunks){
				if(!artChunk.getMagicNumber().equals(DLSFile.MAGIC_ART2)) continue;
				ptr = artChunk.open();
				DLSArticulator artobj = DLSArticulator.read(ptr, 2);
				if(artobj != null){
					inst.globalArt.add(artobj);
				}
			}
		}
		
		//Info
		child = listMap.getFirstValueWithKey(DLSFile.MAGIC_INFO);
		if(child != null && child.isList()){
			inst.info = RIFFReader.readINFOList(child);
		}
		
		return inst;
	}

}
