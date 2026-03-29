package waffleoRai_Containers.media.matroska;

import java.io.IOException;
import java.io.OutputStream;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.media.MatroskaCommon;
import waffleoRai_Files.tree.FileNode;
import waffleoRai_Files.tree.MemFileNode;
import waffleoRai_Files.tree.PatchworkFileNode;
import waffleoRai_Utils.BufferReference;
import waffleoRai_Utils.FileBuffer;

public class MatroskaBlock {

	private int trackNumber = -1;
	private int timeOffset = 0;
	private int lacing = MatroskaCommon.BLOCKLACE_NONE;
	private boolean flagKeyframe = false;
	private boolean flagInvisible = false;
	private boolean flagDiscardable = false;
	
	private FileNode dataRef;
	
	private MatroskaCluster parentCluster;
	private MatroskaTrack parentTrack;
	
	/*----- Init -----*/
	
	public MatroskaBlock() {}
	
	/*----- Getters -----*/

	public int getTrackNumber(){return trackNumber;}
	public int getTimeOffset(){return timeOffset;}
	public int getLacing(){return lacing;}
	public boolean getFlagKeyframe(){return flagKeyframe;}
	public boolean getFlagInvisible(){return flagInvisible;}
	public boolean getFlagDiscardable(){return flagDiscardable;}
	public FileNode getDataRef(){return dataRef;}
	public MatroskaCluster getParentCluster(){return parentCluster;}
	public MatroskaTrack getParentTrack(){return parentTrack;}

	/*----- Setters -----*/

	public void setTrackNumber(int value){trackNumber = value;}
	public void setTimeOffset(int value){timeOffset = value;}
	public void setLacing(int value){lacing = value;}
	public void setFlagKeyframe(boolean value){flagKeyframe = value;}
	public void setFlagInvisible(boolean value){flagInvisible = value;}
	public void setFlagDiscardable(boolean value){flagDiscardable = value;}
	public void setDataRef(FileNode value){dataRef = value;}
	public void setParentCluster(MatroskaCluster value){parentCluster = value;}
	public void setParentTrack(MatroskaTrack value){parentTrack = value;}
	
	/*----- Read -----*/
	
	public void readHeader(BufferReference data) {
		trackNumber = EBMLCommon.parseNextVLQ(data);
		timeOffset = (int)data.nextShort();
		int flags = Byte.toUnsignedInt(data.nextByte());
		lacing = (flags & 0x60) >>> 5;
		flagKeyframe = (flags & 0x1) != 0;
		flagInvisible = (flags & 0x10) != 0;
		flagDiscardable = (flags & 0x80) != 0;
	}
	
	public static MatroskaBlock fromBlob(FileNode source) throws IOException {
		if(source == null) return null;
		
		MatroskaBlock block = new MatroskaBlock();
		
		FileBuffer data = source.loadDecompressedData();
		BufferReference ref = data.getReferenceAt(0L);
		block.readHeader(ref);
		
		long hsize = ref.getBufferPosition();
		block.dataRef = source.getSubFile(hsize, data.getFileSize() - hsize);
		data.dispose();
		
		return block;
	}
	
	/*----- Write -----*/
	
	public FileNode toEBMLBlob() {
		if(dataRef == null) return null;
		
		MemFileNode hdrNode = new MemFileNode(null, "_matroska_blockhdr");
		PatchworkFileNode blockNode = new PatchworkFileNode(null, "_matroska_block_blob", 2);
		hdrNode.setData(serializeHeader());
		blockNode.addBlock(hdrNode);
		blockNode.addBlock(dataRef);
		
		return blockNode;
	}
	
	public FileBuffer serializeHeader() {
		int hsize = EBMLCommon.calcVLQLength(trackNumber) + 3;
		FileBuffer hdr = new FileBuffer(hsize, true);
		byte[] vlq = EBMLCommon.encodeVLQ(trackNumber);
		for(int i= 0; i < vlq.length; i++) hdr.addToFile(vlq[i]);
		hdr.addToFile((short)timeOffset);
		
		int flags = 0;
		if(flagKeyframe) flags |= 0x1;
		if(flagInvisible) flags |= 0x10;
		if(flagDiscardable) flags |= 0x80;
		flags |= (lacing & 0x3) << 5;
		hdr.addToFile((byte)flags);
		
		return hdr;
	}
	
	public long getTotalSize() {
		long datSize = 0;
		if(dataRef != null) datSize = dataRef.getLength();
		return datSize + EBMLCommon.calcVLQLength(trackNumber) + 3;
	}
	
	public long serializeTo(OutputStream output) throws IOException {
		if(output == null) return 0L;
		FileBuffer hdr = serializeHeader();
		long sz = hdr.getFileSize();
		hdr.writeToStream(output);
		hdr.dispose();
		
		if(dataRef != null) {
			FileBuffer dat = dataRef.loadDecompressedData();
			dat.writeToStream(output);
			sz += dat.getFileSize();
			dat.dispose();
		}
		
		return sz;
	}
	
}
