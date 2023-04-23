package waffleoRai_soundbank.dls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import waffleoRai_DataContainers.MultiValMap;
import waffleoRai_Files.RIFFReader.RIFFChunk;
import waffleoRai_Files.RIFFReader.RIFFList;
import waffleoRai_Utils.BufferReference;
import waffleoRai_soundbank.DLSFile;
import waffleoRai_soundbank.Region;

/*
 * (cdl)
 * rgnh
 * wsmp
 * (wlnk)
 * (lart) lvl 1 only
 * (lar2)
 * (INFO)
 */

public class DLSRegion extends Region{
	
	/*----- Instance Variables -----*/
	
	private short fusOptions;
	private short usKeyGroup;
	private int wsmpOptions;
	
	private DLSSampleLoop loop; //null if one shot
	
	private DLSWaveLink waveLink; //Optional
	private ArrayList<DLSArticulator> articulators; //Optional
	
	/*----- Init -----*/
	
	private DLSRegion(){
		super.resetDefaults();
	}
	
	/*----- Getters -----*/
	
	/*----- Readers -----*/
	
	private void readHeader(BufferReference data){
		if(data == null) return;
		super.setMinKey(data.nextShort());
		super.setMaxKey(data.nextShort());
		super.setMinVelocity(data.nextShort());
		super.setMaxVelocity(data.nextShort());
		fusOptions = data.nextShort();
		usKeyGroup = data.nextShort();
	}
	
	private void readWSMP(BufferReference data){
		if(data == null) return;
		data.add(4); //cbSize
		super.setUnityKey(data.nextShort()); //usUnityNote
		super.setFineTune(data.nextShort());
		super.setVolume(data.nextInt()); //lgain
		wsmpOptions = data.nextInt(); //fulOptions
		int loop_count = data.nextInt();
		
		if(loop_count == 1){
			//Only 0 and 1 are definted fwr
			loop = DLSSampleLoop.read(data);
		}
	}
	
	public static DLSRegion read(RIFFChunk chunk) throws IOException{
		if(chunk == null) return null;
		if(!chunk.isList()) return null;
		
		//Chunk passed should be a "rgn " or "rgn2"
		String magic = chunk.getMagicNumber();
		int ver = 1;
		if(magic.equals(DLSFile.MAGIC_RGN2)) ver = 2;
		else{
			if(!magic.equals(DLSFile.MAGIC_RGN)) return null;
		}
		
		RIFFList chunkList = chunk.getListContents();
		MultiValMap<String, RIFFChunk> listMap = chunkList.mapContentsById();
		DLSRegion reg = new DLSRegion();
		
		//Look for rgnh
		RIFFChunk rgnh = listMap.getFirstValueWithKey(DLSFile.MAGIC_RGNH);
		if(rgnh == null) return null;
		BufferReference ptr = rgnh.open();
		reg.readHeader(ptr);
		
		//Look for wsmp
		RIFFChunk wsmp = listMap.getFirstValueWithKey(DLSFile.MAGIC_WSMP);
		if(wsmp == null) return null;
		ptr = wsmp.open();
		reg.readWSMP(ptr);
		
		//Look for wlnk
		RIFFChunk wlnk = listMap.getFirstValueWithKey(DLSFile.MAGIC_WLNK);
		if(wlnk != null){
			ptr = wlnk.open();
			reg.waveLink = DLSWaveLink.read(ptr);
		}
		
		//Look for lart (if V1)
		if(ver == 1){
			RIFFChunk lart = listMap.getFirstValueWithKey(DLSFile.MAGIC_LART);
			if(lart != null && lart.isList()){
				RIFFList artList = lart.getListContents();
				List<RIFFChunk> artChunks = artList.getAllItems();
				reg.articulators = new ArrayList<DLSArticulator>(artChunks.size()+1);
				
				for(RIFFChunk artChunk : artChunks){
					if(!artChunk.getMagicNumber().equals(DLSFile.MAGIC_ART1)) continue;
					ptr = artChunk.open();
					DLSArticulator artobj = DLSArticulator.read(ptr, 1);
					if(artobj != null){
						reg.articulators.add(artobj);
					}
				}
			}
		}
		
		//Look for lar2
		RIFFChunk lar2 = listMap.getFirstValueWithKey(DLSFile.MAGIC_LAR2);
		if(lar2 != null && lar2.isList()){
			RIFFList artList = lar2.getListContents();
			List<RIFFChunk> artChunks = artList.getAllItems();
			reg.articulators = new ArrayList<DLSArticulator>(artChunks.size()+1);
			
			for(RIFFChunk artChunk : artChunks){
				if(!artChunk.getMagicNumber().equals(DLSFile.MAGIC_ART2)) continue;
				ptr = artChunk.open();
				DLSArticulator artobj = DLSArticulator.read(ptr, 2);
				if(artobj != null){
					reg.articulators.add(artobj);
				}
			}
		}
		
		return reg;
	}

}
