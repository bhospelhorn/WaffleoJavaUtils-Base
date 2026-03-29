package waffleoRai_Containers.media.matroska;

import java.util.List;

public class MatroskaCluster {
	
	private long timestamp; //In segment ticks
	private long position;
	private long prevSize;
	
	private List<MatroskaClusterBlock> blocks;
	
	private MatroskaSegment parentSegment;
	
	/*----- Getters -----*/

	public long getTimestamp(){return timestamp;}
	public long getPosition(){return position;}
	public long getPrevSize(){return prevSize;}
	public List<MatroskaClusterBlock> getBlocks(){return blocks;}
	public MatroskaSegment getParentSegment(){return parentSegment;}

	/*----- Setters -----*/

	public void setTimestamp(long value){timestamp = value;}
	public void setPosition(long value){position = value;}
	public void setPrevSize(long value){prevSize = value;}
	public void setBlocks(List<MatroskaClusterBlock> value){blocks = value;}
	public void setParentSegment(MatroskaSegment value){parentSegment = value;}

}
