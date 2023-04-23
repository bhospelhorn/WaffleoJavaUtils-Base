package waffleoRai_soundbank.dls;

import java.util.ArrayList;

import waffleoRai_Utils.BufferReference;

public class DLSArticulator {
	
	/*----- Inner Classes -----*/
	
	public static class DLSConBlock{
		public short usSource;
		public short usControl;
		public short usDestination;
		public short usTransform;
		public int lScale;
	}
	
	/*----- Instance Variables -----*/
	
	private ArrayList<DLSConBlock> blocks;
	
	/*----- Init -----*/
	
	private DLSArticulator(int blockAlloc){
		blocks = new ArrayList<DLSConBlock>(blockAlloc);
	}
	
	/*----- Readers -----*/
	
	public static DLSArticulator read(BufferReference data, int version){
		if(data == null) return null;
		
		//Skip cbSize
		data.add(4);
		int bcount = data.nextInt();
		
		DLSArticulator art = new DLSArticulator(bcount);
		for(int i = 0; i < bcount; i++){
			DLSConBlock block = new DLSConBlock();
			block.usSource = data.nextShort();
			block.usControl = data.nextShort();
			block.usDestination = data.nextShort();
			block.usTransform = data.nextShort();
			block.lScale = data.nextInt();
			art.blocks.add(block);
		}
		
		return art;
	}
	
	/*----- Getters -----*/
	
	public int blockCount(){return blocks.size();}
	
	public ArrayList<DLSConBlock> getBlocks(){
		ArrayList<DLSConBlock> copy = new ArrayList<DLSConBlock>(blocks.size()+1);
		return copy;
	}

}
