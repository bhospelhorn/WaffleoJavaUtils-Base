package waffleoRai_Containers.media.matroska;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import waffleoRai_Containers.ebml.EBMLBigIntElement;
import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLExtBlobElement;
import waffleoRai_Containers.ebml.EBMLIntElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLRawElement;
import waffleoRai_Files.tree.FileNode;

public class MatroskaBlockGroup extends MatroskaClusterBlock{
	
	public static final int EBML_BASE_ID = 0x20;
	
	private static final int EBML_ID_BLOCK = 0x21;
	private static final int EBML_ID_BLOCK_ADD = 0x35a1;
	private static final int EBML_ID_DURATION = 0x1b;
	private static final int EBML_ID_PRIORITY = 0x7a;
	private static final int EBML_ID_REFBLOCK = 0x7b;
	private static final int EBML_ID_CODEC_STATE = 0x24;
	private static final int EBML_ID_DISCARD_PADDING = 0x35a2;
	
	private List<MatroskaBlockMore> additions;
	private long duration;
	private int refPriority;
	private List<Integer> refBlocks;
	private byte[] codecState;
	private int discardPadding;
	
	public MatroskaBlockGroup() {
		additions = new LinkedList<MatroskaBlockMore>();
		refBlocks = new LinkedList<Integer>();
	}
	
	/*----- Getters -----*/

	public List<MatroskaBlockMore> getAdditions(){return additions;}
	public long getDuration(){return duration;}
	public int getRefPriority(){return refPriority;}
	public byte[] getCodecState(){return codecState;}
	public int getDiscardPadding(){return discardPadding;}

	/*----- Setters -----*/

	public void setAdditions(List<MatroskaBlockMore> value){additions = value;}
	public void setDuration(long value){duration = value;}
	public void setRefPriority(int value){refPriority = value;}
	public void setCodecState(byte[] value){codecState = value;}
	public void setDiscardPadding(int value){discardPadding = value;}
	
	/*----- Read -----*/
	
	public static MatroskaBlockGroup fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		MatroskaBlockGroup item = new MatroskaBlockGroup();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		//Mandatory
		EBMLElement e = me.getFirstChildWithId(EBML_ID_BLOCK);
		if(e == null) return null;
		FileNode blockSrc = EBMLCommon.readExternalBlobElement(e);
		if(blockSrc == null) return null;
		try {item.block = MatroskaBlock.fromBlob(blockSrc);} 
		catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		
		//Technically mandatory
		e = me.getFirstChildWithId(EBML_ID_PRIORITY);
		item.refPriority = EBMLCommon.readIntElement(e);
		
		//Optional
		e = me.getFirstChildWithId(EBML_ID_DURATION);
		item.duration = EBMLCommon.readIntElementLong(e);
		
		e = me.getFirstChildWithId(EBML_ID_CODEC_STATE);
		item.codecState = EBMLCommon.loadBlobElement(e);
		
		e = me.getFirstChildWithId(EBML_ID_DISCARD_PADDING);
		item.discardPadding = EBMLCommon.readIntElement(e);
		
		List<EBMLElement> elist = me.getChildrenWithId(EBML_ID_REFBLOCK);
		for(EBMLElement ee : elist) {
			int i = EBMLCommon.readIntElement(ee);
			if(i >= 0) item.refBlocks.add(i);
		}
		
		e = me.getFirstChildWithId(EBML_ID_BLOCK_ADD);
		if((e != null) && (e instanceof EBMLMasterElement)) {
			EBMLMasterElement child = (EBMLMasterElement)e;
			elist = child.getChildrenWithId(MatroskaBlockMore.EBML_BASE_ID);
			for(EBMLElement ee : elist) {
				MatroskaBlockMore m = MatroskaBlockMore.fromEBML(ee);
				if(m != null) item.additions.add(m);
			}
		}
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(block == null) return null;
		
		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLExtBlobElement(EBML_ID_BLOCK, block.toEBMLBlob()));
		
		if(!additions.isEmpty()) {
			EBMLMasterElement cme = new EBMLMasterElement(EBML_ID_BLOCK_ADD);
			for(MatroskaBlockMore add : additions) {
				cme.addChild(add.toEBML());
			}
			me.addChild(cme);
		}
		
		if(duration >= 0) me.addChild(new EBMLBigIntElement(EBML_ID_DURATION, duration, false));
		me.addChild(new EBMLIntElement(EBML_ID_PRIORITY, refPriority, false));
		if(!refBlocks.isEmpty()) {
			for(Integer b : refBlocks) {
				me.addChild(new EBMLIntElement(EBML_ID_REFBLOCK, b, true));
			}
		}
		if(codecState != null) me.addChild(new EBMLRawElement(EBML_ID_CODEC_STATE, codecState));
		if(discardPadding != 0) me.addChild(new EBMLIntElement(EBML_ID_DISCARD_PADDING, discardPadding, true));
		
		return me;
	}

}
