package waffleoRai_Files;

import java.io.IOException;

import waffleoRai_Utils.DirectoryNode;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileNode;
import waffleoRai_Utils.MultiFileBuffer;

/*
 * UPDATES
 * 
 * 2020.06.15 | 1.0.0
 * 		Initial Documentation
 * 
 */

/**
 * A FileNode subclass for referencing files in a CD image (eg. ISO file) that
 * references offsets by sector (noting sector type) instead of byte offset.
 * This way, sector checksum data can be included or excluded as needed.
 * @author Blythe Hospelhorn
 * @version 1.0.0
 * @since June 15, 2020
 */
public class ISOFileNode extends FileNode{
	
	/* --- Instance Variables --- */
	
	//Offset and size of superclass are used, but are measured instead in sectors
	private int sector_head_size; //Size in bytes before sector data
	private int sector_data_size;
	private int sector_foot_size; //Size in bytes after sector data
	
	/* --- Construction --- */

	/**
	 * Construct a new ISOFileNode with the provided parent and file name.
	 * @param parent Parent directory. May use null if there is none.
	 * @param name Filename to initialize node with.
	 * @since 1.0.0
	 */
	public ISOFileNode(DirectoryNode parent, String name) {
		super(parent, name);
		//Default ISO values for head and foot...
		sector_head_size = 0x10;
		sector_data_size = 0x800;
		sector_foot_size = 0x120;
	}
	
	/* --- Getters --- */
	
	/**
	 * Get the total size of a sector on the CD image this node
	 * references. This includes the data and any EC headers or footers.
	 * @return Total size in bytes of a sector on referenced CD image.
	 * @since 1.0.0
	 */
	public int getSectorTotalSize(){
		return sector_data_size + sector_head_size + sector_foot_size;
	}
	
	/**
	 * Get the size in bytes of the data section on a sector of the
	 * CD image this node is referencing. This excludes any EC header
	 * or footer data.
	 * @return Size in bytes of data in a single sector.
	 * @since 1.0.0
	 */
	public int getSectorDataSize(){return sector_data_size;}
	
	/**
	 * Get the size in bytes of the header on a sector of the
	 * CD image this node is referencing.
	 * @return Size in bytes of header of a single sector.
	 * @since 1.0.0
	 */
	public int getSectorHeadSize(){return sector_head_size;}
	
	/**
	 * Get the size in bytes of the footer on a sector of the
	 * CD image this node is referencing.
	 * @return Size in bytes of footer of a single sector.
	 * @since 1.0.0
	 */
	public int getSectorFootSize(){return sector_foot_size;}
	
	/* --- Setters --- */
	
	/**
	 * Set the sector header, footer, and data sizes to match
	 * the specifications for CD-XA Mode 2, Form 1. (Used in PSX discs)
	 */
	public void setMode2Form1(){
		sector_head_size = 0x18;
		sector_data_size = 0x800;
		sector_foot_size = 0x118;
	}
	
	/**
	 * Set the sector header, footer, and data sizes to match
	 * the specifications for CD-XA Mode 2, Form 2. (Used in PSX discs)
	 */
	public void setMode2Form2(){
		sector_head_size = 0x18;
		sector_data_size = 0x914;
		sector_foot_size = 0x4;
	}

	/**
	 * Set the sector header, footer, and data sizes to match the 
	 * specifications of an audio only sector on a standard CD image.
	 * @since 1.0.0
	 */
	public void setAudioMode(){
		sector_head_size = 0;
		sector_data_size = 0x930;
		sector_foot_size = 0;
	}
	
	/**
	 * Directly set the sector header, footer, and data sizes.
	 * @param head Header size in bytes.
	 * @param dat Data size in bytes.
	 * @param foot Footer size in bytes.
	 * @since 1.0.0
	 */
	public void setSectorDataSizes(int head, int dat, int foot){
		this.sector_head_size = head;
		this.sector_data_size = dat;
		this.sector_foot_size = foot;
	}
	
	/* --- Data Handling --- */
	
	protected void copyDataTo(FileNode copy){
		super.copyDataTo(copy);
		if(copy instanceof ISOFileNode){
			ISOFileNode other = (ISOFileNode)copy;
			other.sector_data_size = this.sector_data_size;
			other.sector_foot_size = this.sector_foot_size;
			other.sector_head_size = this.sector_head_size;
		}
	}
	
	public FileBuffer loadData() throws IOException{
		//This strips any header/footer data
		FileBuffer raw = loadRawData();
		if(sector_head_size == 0 && sector_foot_size == 0) return raw;
		
		//Strip
		int sec_count = (int)super.getLength(); //Size in sectors
		int sec_size = this.getSectorTotalSize();
		long cpos = 0; long datend = sector_head_size + sector_data_size;
		MultiFileBuffer dat = new MultiFileBuffer(sec_count);
		for(int s = 0; s < sec_count; s++){
			dat.addToFile(raw, cpos + sector_head_size, cpos + datend);
			cpos += sec_size;
		}
		
		return dat;
	}
	
	/**
	 * Load ALL data, including sector header/footer data, referenced by this node
	 * into a single FileBuffer.
	 * @return FileBuffer containing raw data referenced by file - all full sectors sequential.
	 * @throws IOException If the data cannot be loaded from disk.
	 * @since 1.0.0
	 */
	public FileBuffer loadRawData() throws IOException{
		String path = getSourcePath();
		//FileBuffer imgdat = FileBuffer.createBuffer(path);
		//ISO iso = new ISO(imgdat, true);
		
		//Chain together desired sectors.
		int sec_count = (int)super.getLength(); //Size in sectors
		int sec_size = this.getSectorTotalSize();
		int bytelen = sec_count * sec_size;
		
		long stpos = sec_size * this.getOffset();
		long edpos = stpos + bytelen;
		
		return FileBuffer.createBuffer(path, stpos, edpos);
	}
	
	
}
