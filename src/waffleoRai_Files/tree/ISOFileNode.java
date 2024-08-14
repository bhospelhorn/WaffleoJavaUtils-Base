package waffleoRai_Files.tree;

import java.io.IOException;
import java.util.List;

import waffleoRai_Encryption.StaticDecryption;
import waffleoRai_Encryption.StaticDecryptor;
import waffleoRai_Files.DiskDataFilter;
import waffleoRai_Files.EncryptionDefinition;
import waffleoRai_Utils.CacheFileBuffer;
import waffleoRai_Utils.EncryptedFileBuffer;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.MultiFileBuffer;

/*
 * UPDATES
 * 
 * 2020.06.15 | 1.0.0
 * 		Initial Documentation
 * 2020.06.17 | 1.1.0
 * 		Size should be in bytes, not sectors. (Might not fill last sector).
 * 2020.06.21 | 1.2.0
 * 		Added data loaders that only load file part
 * 2020.08.28 | 2.0.0
 * 		Updated loading procedure to be compatible w/ FileNode 3.0.0
 * 2020.09.30 | 2.1.0
 * 		Gutted loadDirect() to use RawDiskBuffer and handle 
 * 			decryption buffering that requires sec header/footer data
 * 2020.11.15 | 2.2.0
 * 		Now records block sizes to super class
 * 		Again gutted loadDirect() to use RawDiskBuffer
 * 2021.01.14 | 2.3.0
 * 		Added getRawDataNode()
 * 2021.01.17 | 2.3.1
 * 		A quick patch in loadDirect() that tries to guess whether coordinates
 * 			are raw or data from superclass block sizes. It's a quickfix, so
 * 			if there are coordinate or optimization issues again, maybe just don't
 * 			set the superclass blocksize?
 * 2021.01.28 | 2.3.2
 * 		Ref trace methods
 * 2021.01.31 | 2.3.3
 * 		Hopefully fixed bug recalculating length of raw node
 * 2023.11.08 | 2.4.0
 * 		Update to FileNode 3.6.1 compatibility
 */

/**
 * A FileNode subclass for referencing files in a CD image (eg. ISO file) that
 * references offsets by sector (noting sector type) instead of byte offset.
 * This way, sector checksum data can be included or excluded as needed.
 * @author Blythe Hospelhorn
 * @version 2.4.0
 * @since November 8, 2023
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
		
		super.setBlockSize(getSectorTotalSize(), sector_data_size);
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
	
	/**
	 * Get the number of sectors required to hold this file.
	 * @return File size in CD sectors.
	 * @since 1.1.0
	 */
	public int getLengthInSectors(){
		int raw_bytelen = (int)super.getLength(); //Size in bytes
		int sec_size = this.getSectorDataSize();
		int sec_len = raw_bytelen/sec_size;
		if(raw_bytelen % sec_size != 0) sec_len++;
		
		return sec_len;
	}
	
	/* --- Setters --- */
	
	/**
	 * Set the sector header, footer, and data sizes to match
	 * the specifications for CD-XA Mode 2, Form 1. (Used in PSX discs)
	 */
	public void setMode2Form1(){
		setSectorDataSizes(0x18, 0x800, 0x118);
	}
	
	/**
	 * Set the sector header, footer, and data sizes to match
	 * the specifications for CD-XA Mode 2, Form 2. (Used in PSX discs)
	 */
	public void setMode2Form2(){
		setSectorDataSizes(0x18, 0x914, 0x4);
	}

	/**
	 * Set the sector header, footer, and data sizes to match the 
	 * specifications of an audio only sector on a standard CD image.
	 * @since 1.0.0
	 */
	public void setAudioMode(){
		setSectorDataSizes(0x0, 0x930, 0x0);
	}
	
	/**
	 * Directly set the sector header, footer, and data sizes.
	 * <br>This method also updates the length to match the new
	 * data size.
	 * @param head Header size in bytes.
	 * @param dat Data size in bytes.
	 * @param foot Footer size in bytes.
	 * @since 1.0.0
	 */
	public void setSectorDataSizes(int head, int dat, int foot){
		//Get length
		long oldlen_s = this.getLengthInSectors();
		//System.err.println("");
		
		this.sector_head_size = head;
		this.sector_data_size = dat;
		this.sector_foot_size = foot;
		
		//set super loaders too
		super.setBlockSize(getSectorTotalSize(), sector_data_size);
		
		//Update length
		super.setLength(oldlen_s * sector_data_size);
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
	
	public FileNode getSubFile(long stoff, long len){
		//If stoff falls on a sector boundary, return a new ISOFileNode.
		//Else, return a node sourced by this node
		int sec_dat_size = this.getSectorDataSize();
		long stsec = stoff/sec_dat_size; //Start sector index
		long sec_stoff = stoff%sec_dat_size; // Position relative to sector data start
		
		if(sec_stoff == 0){
			ISOFileNode sub = new ISOFileNode(null, "");
			copyDataTo(sub);
			sub.setOffset(sub.getOffset() + stsec);
			
			long subst = sub.getOffset() * sec_dat_size;
			long maxed = (getOffset() * sec_dat_size) + getLength();
			long subed = subst + len;
			if(subed > maxed) subed = maxed;
			sub.setLength(subed - subst);
			
			sub.subsetEncryptionRegions(subst, sub.getLength()); //Mark encryregion offsets in bytes...
			
			return sub;
		}
		else{
			//Just make it virtually sourced...
			FileNode sub = new FileNode(null, "");
			sub.setVirtualSourceNode(this);
			sub.setOffset(stoff);
			sub.setLength(len);
			
			return sub;
		}
	}
	
	/**
	 * Get a new <code>FileNode</code> referencing the raw data referenced
	 * by this <code>ISOFileNode</code>. In other words, this new node
	 * reveals all of the data backed by this node, not skipping sector
	 * metadata.
	 * <br><b>Note:</b> In order to avoid tree breaking, <i>the new node will NOT
	 * have a parent node, and the generated name will be a copy name.</i>
	 * @return <code>FileNode</code> referencing this node's full raw data.
	 * @since 2.3.0
	 */
	public FileNode getRawDataNode(){
		int secSize = getSectorTotalSize();
		int secCount = getLengthInSectors();
		
		long newoff = (long)secSize * getOffset();
		long newlen = (long)secSize * (long)secCount;
		
		//System.err.println("secSize: 0x" + Integer.toHexString(secSize));
		//System.err.println("secCount: " + secCount);
		//System.err.println("newoff: 0x" + Long.toHexString(newoff));
		//System.err.println("newlen: 0x" + Long.toHexString(newlen));
		
		FileNode raw = new FileNode(null, super.getFileName() + "_RAW");
		if(hasVirtualSource()){
			raw.setVirtualSourceNode(getVirtualSource());
		}
		else{
			raw.setSourcePath(getSourcePath());
		}
		raw.setOffset(newoff);
		raw.setLength(newlen);
		raw.generateGUID();
		
		return raw;
	}
	
	protected FileBuffer loadDataFilterBuff(long stsec, long edsec, int options) throws IOException{
		FileBuffer raw = loadRawData(stsec, edsec - stsec + 1, options);
		return new EncryptedFileBuffer(raw, new DiskDataFilter(sector_head_size, sector_data_size, sector_foot_size));
		
		/*if(hasVirtualSource()){
			//Load into an enc byffer w/ DiskDataFilter
			FileBuffer raw = loadRawData(stsec, edsec - stsec, forceCache);
			return new EncryptedFileBuffer(raw, new DiskDataFilter(sector_head_size, sector_data_size, sector_foot_size));
		}
		else{
			long seclen = getSectorTotalSize();
			long st = stsec * seclen;
			long ed = edsec * seclen;
			return RawDiskBuffer.openReadOnly(getSourcePath(), sector_head_size, sector_data_size, 
					sector_foot_size, 8192, st, ed, true);	
		}*/
	}
	
	protected FileBuffer loadDirect(long stpos, long len, int options) throws IOException{
		//System.err.println("ISOFileNode.loadDirect || Called: stpos = 0x" + Long.toHexString(stpos) + " | len = 0x" + Long.toHexString(len));
		
		//Convert position coordinates to sectors
		long edpos = stpos + len;
		long stsec = -1L;
		long edsec = -1L;
		
		if(super.getInputBlockSize() != super.getOutputBlockSize()){
			//Assume it's in raw coordinates
			stsec = stpos/(long)super.getInputBlockSize();
			edsec = edpos/(long)super.getInputBlockSize();
		}
		else{
			//Assume it's in data coordinates
			stsec = stpos/(long)sector_data_size;
			edsec = edpos/(long)sector_data_size;
		}
		//System.err.println("ISOFileNode.loadDirect || Node start sec: " + super.getOffset() + " | rel start sec: " + stsec);
		
		//Scan encryption chain to see if sector header/footer stuff is needed
		boolean dodec = false;
		List<EncryptionDefinition> echain = null;
		long[][] reg_bounds = null;
		if(((options & FileNode.LOADOP_DECRYPT) != 0) && super.hasEncryption()){
			echain = super.getEncryptionDefChain();
			reg_bounds = super.getEncryptedRegions();
			
			int i = -1;
			for(EncryptionDefinition def : echain){
				//See if falls in region...
				//(Coordinates should be in sectors...)
				i++;
				if(edsec <= reg_bounds[i][0]) continue;
				if(stsec > reg_bounds[i][1]) continue;
				if(def.unevenIOBlocks()){
					dodec = true;
					break;
				}
			}
			
		}
		
		if(dodec){
			int ecount = echain.size();
			if(ecount == 1 && stsec <= reg_bounds[0][0] && edsec < reg_bounds[0][1]){
				//If there's only one eregion and it covers the whole loading area...
				//Check for a loaded decryptor...
				EncryptionDefinition def = echain.get(0);
				StaticDecryptor decer = StaticDecryption.getDecryptorState(def.getID());
				if(decer != null){
					super.load_flag_decwrap_direct = true; //Don't want superclass redoing decryption
					//Load raw into enc buffer
					FileBuffer raw = loadRawData(stsec, edsec - stsec, options);
					return new EncryptedFileBuffer(raw, decer.generateDecryptor(this));
				}
				else{
					//No decryption method defined. Just return a RawDiskBuffer
					return loadDataFilterBuff(stsec, edsec, options);
				}
			}
			else{
				//Else...
				MultiFileBuffer out = new MultiFileBuffer((ecount << 1) + 1);
				super.load_flag_decwrap_direct = true;
				
				int i = 0;
				long seclen = getSectorTotalSize();
				long st = stsec * seclen;
				long ed = edsec * seclen;
				long pos = st;
				for(EncryptionDefinition def : echain){
					//See if falls within load region
					long rst = reg_bounds[i][0];
					long red = reg_bounds[i][1];
					if(red <= st){i++; continue;}
					if(rst >= ed){break;}
					
					//Grab anything before it...
					if(pos < st){
						long ssec = st/sector_data_size;
						long esec = pos/sector_data_size;
						if(pos % sector_data_size != 0) esec++;
						FileBuffer reg = loadDataFilterBuff(ssec, esec, options);
						out.addToFile(reg);
					}
					
					//Get decryptor
					StaticDecryptor decer = StaticDecryption.getDecryptorState(def.getID());
					
					//Grab data within
					long ssec = st/sector_data_size;
					long esec = pos/sector_data_size;
					if(pos % sector_data_size != 0) esec++;
					if(decer != null){
						FileBuffer reg = loadRawData(ssec, esec - ssec, options);
						reg = new EncryptedFileBuffer(reg, decer.generateDecryptor(this));
						out.addToFile(reg);
					}
					else{
						FileBuffer reg = loadDataFilterBuff(ssec, esec, options);
						out.addToFile(reg);
					}
					
					i++;
				}
				
				return out;
			}
		}
		else{
			//Just load into a RawDiskBuffer
			//Superclass will handle anything else
			return loadDataFilterBuff(stsec, edsec, options);
		}

	}

	/** Load ALL data in the specified sectors, including sector header/footer data, referenced by this node
	 * into a single FileBuffer.
	 * @param sec_off Offset in sectors relative to the start of this file to begin reading.
	 * @param sec_len Number of sectors to load. If this added to the offset sector exceeds the length
	 * of the file, this method will return only what is in this file.
	 * @param forceCache Whether or not to force the data to be loaded into a <code>CacheFileBuffer</code>
	 * regardless of incoming data size.
	 * @return FileBuffer containing raw data referenced by file - all full sectors sequential.
	 * @throws IOException If the data cannot be loaded from disk.
	 * @since 2.0.0
	 */
	public FileBuffer loadRawData(long sec_off, long sec_len, int options) throws IOException{
		if(sec_len <= 0) return null;
		String path = getSourcePath();
		
		//Chain together desired sectors.
		int sec_size = this.getSectorTotalSize();
		long maxlen = this.getLengthInSectors() - sec_off;
		if(sec_len > maxlen) sec_len = maxlen;
		
		long stpos = sec_size * (this.getOffset() + sec_off);
		long edpos = stpos + (sec_size*sec_len);
		
		if(this.hasVirtualSource()){
			FileNode src = this.getVirtualSource();
			src = src.getSubFile(stpos, edpos);
			return src.loadDecompressedData(options);
		}
		else{
			if((options & FileNode.LOADOP_FORCE_CACHE) != 0) {
				return CacheFileBuffer.getReadOnlyCacheBuffer(path, sec_size, 512, stpos, edpos);
			}
			else return FileBuffer.createBuffer(path, stpos, edpos);
		}
	}
	
	 /** Load ALL data in the specified sectors, including sector header/footer data, referenced by this node
	 * into a single FileBuffer.
	 * @param sec_off Offset in sectors relative to the start of this file to begin reading.
	 * @param sec_len Number of sectors to load. If this added to the offset sector exceeds the length
	 * of the file, this method will return only what is in this file.
	 * @return FileBuffer containing raw data referenced by file - all full sectors sequential.
	 * @throws IOException If the data cannot be loaded from disk.
	 * @since 1.2.0
	 */
	public FileBuffer loadRawData(long sec_off, long sec_len) throws IOException{
		return loadRawData(sec_off, sec_len, FileNode.LOADOP_NONE);
	}
	
	/**
	 * Load ALL data, including sector header/footer data, referenced by this node
	 * into a single FileBuffer.
	 * @param forceCache Whether or not to force the data to be loaded into a <code>CacheFileBuffer</code>
	 * regardless of incoming data size.
	 * @return FileBuffer containing raw data referenced by file - all full sectors sequential.
	 * @throws IOException If the data cannot be loaded from disk.
	 * @since 2.0.0
	 */
	protected FileBuffer loadRawData(int options) throws IOException{
		String path = getSourcePath();
		//FileBuffer imgdat = FileBuffer.createBuffer(path);
		//ISO iso = new ISO(imgdat, true);
		
		//Chain together desired sectors.
		int sec_size = this.getSectorTotalSize();
		int sec_len = getLengthInSectors();
		
		long stpos = sec_size * this.getOffset();
		long edpos = stpos + (sec_size*sec_len);
		
		//System.err.println("Loading... 0x" + Long.toHexString(stpos) + " - 0x" + Long.toHexString(edpos));
		
		boolean forceCache = (options & FileNode.LOADOP_FORCE_CACHE) != 0;
		if(this.hasVirtualSource()){
			FileNode src = this.getVirtualSource();
			src = src.getSubFile(stpos, edpos);
			return src.loadDecompressedData(options);
		}
		else{
			if(forceCache) return CacheFileBuffer.getReadOnlyCacheBuffer(path, sec_size, 512, stpos, edpos);
			else return FileBuffer.createBuffer(path, stpos, edpos);
		}
	}
	
	/**
	 * Load ALL data, including sector header/footer data, referenced by this node
	 * into a single FileBuffer.
	 * @return FileBuffer containing raw data referenced by file - all full sectors sequential.
	 * @throws IOException If the data cannot be loaded from disk.
	 * @since 1.0.0
	 */
	public FileBuffer loadRawData() throws IOException{
		return loadRawData(FileNode.LOADOP_NONE);
	}
	
	/* --- View --- */
	
	public String getLocationString(){
		int endsec = (int)getOffset() + this.getLengthInSectors();
		return "sec " + this.getOffset() + " - " + endsec;
	}
	
	/* --- Debug --- */
	
	protected String getTypeString(){return "ISOFileNode";}
	protected String getOffsetString(){return "Sector " + getOffset();}
	protected String getLengthString(){return "0x" + Long.toHexString(getLength()) + " bytes";}
	
	public void printMeToStdErr(int indents)
	{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		long bytelen = this.getLength();
		long secsize = this.getSectorDataSize();
		long secct = bytelen/secsize;
		if(bytelen % secsize != 0) secct++;
		
		System.err.println(tabs + "->" + this.getFileName() + " (sec " + this.getOffset() + " - " + (this.getOffset() + secct - 1) + " -- 0x" + Long.toHexString(bytelen) + " bytes)");
	}
	
	
}
