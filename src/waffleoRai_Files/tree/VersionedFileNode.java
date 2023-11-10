package waffleoRai_Files.tree;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import waffleoRai_Files.EncryptionDefinition;
import waffleoRai_Files.FileTypeNode;
import waffleoRai_Utils.FileBuffer;

public class VersionedFileNode extends FileNode{
	
	/* --- Instance Variables --- */
	
	private FileNode activeVersion; //Common interface methods use this
	private ArrayList<FileNode> children;
	
	/* --- Init --- */
	
	public VersionedFileNode(DirectoryNode parent, String name) {
		super(parent, name);
		children = new ArrayList<FileNode>();
	}
	
	public VersionedFileNode(DirectoryNode parent, String name, int childAlloc) {
		super(parent, name);
		children = new ArrayList<FileNode>(childAlloc);
	}
	
	/* --- Getters --- */
	
	public List<FileNode> getVersionNodes(){
		List<FileNode> copy = new ArrayList<FileNode>(children.size()+1);
		copy.addAll(children);
		return copy;
	}
	
	/* --- Getters (Common) --- */
	
	public String getSourcePath(){
		if(activeVersion != null) return activeVersion.getSourcePath();
		return super.getSourcePath();
	}
	
	public Collection<String> getAllSourcePaths(){
		if(activeVersion != null) return activeVersion.getAllSourcePaths();
		return super.getAllSourcePaths();
	}
	
	public boolean hasVirtualSource(){
		if(activeVersion != null) return activeVersion.hasVirtualSource();
		return super.hasVirtualSource();
	}
	
	public FileNode getVirtualSource(){
		if(activeVersion != null) return activeVersion.getVirtualSource();
		return super.getVirtualSource();
	}
	
	public long getOffset(){
		if(activeVersion != null) return activeVersion.getOffset();
		return super.getOffset();
	}
	
	public long getLength(){
		if(activeVersion != null) return activeVersion.getLength();
		return super.getLength();
	}
	
	public int getBlockSize(){
		if(activeVersion != null) return activeVersion.getBlockSize();
		return super.getBlockSize();
	}
	
	public int getOutputBlockSize(){
		if(activeVersion != null) return activeVersion.getOutputBlockSize();
		return super.getOutputBlockSize();
	}
	
	public int getInputBlockSize(){
		if(activeVersion != null) return activeVersion.getInputBlockSize();
		return super.getInputBlockSize();
	}
	
	public ZonedDateTime getTimestamp(){
		if(activeVersion != null) return activeVersion.getTimestamp();
		return super.getTimestamp();
	}
	
	public boolean hasEncryption(){
		if(activeVersion != null) return activeVersion.hasEncryption();
		return super.hasEncryption();
	}
	
	public List<EncryptionDefinition> getEncryptionDefChain(){
		if(activeVersion != null) return activeVersion.getEncryptionDefChain();
		return super.getEncryptionDefChain();
	}
	
	public long[][] getEncryptedRegions(){
		if(activeVersion != null) return activeVersion.getEncryptedRegions();
		return super.getEncryptedRegions();
	}
	
	public boolean hasCompression(){
		if(activeVersion != null) return activeVersion.hasCompression();
		return super.hasCompression();
	}
	
	public boolean sourceDataCompressed(){
		if(activeVersion != null) return activeVersion.sourceDataCompressed();
		return super.sourceDataCompressed();
	}
	
	public FileNode getContainer(){
		if(activeVersion != null) return activeVersion.getContainer();
		return super.getContainer();
	}
	
	public FileTypeNode getTypeChainHead(){
		if(activeVersion != null) return activeVersion.getTypeChainHead();
		return super.getTypeChainHead();
	}
	
	public FileTypeNode getTypeChainTail(){
		if(activeVersion != null) return activeVersion.getTypeChainTail();
		return super.getTypeChainTail();
	}
	
	public List<FileTypeNode> getTypeChainAsList(){
		if(activeVersion != null) return activeVersion.getTypeChainAsList();
		return super.getTypeChainAsList();
	}
	
	public boolean hasTypingMark(){
		if(activeVersion != null) return activeVersion.hasTypingMark();
		return super.hasTypingMark();
	}
	
	protected String getContainerTempPath(){
		if(activeVersion != null) return activeVersion.getContainerTempPath();
		return super.getContainerTempPath();
	}
	
	/* --- Setters --- */
	
	public void ensureChildNodeCapacity(int minCapacity){
		children.ensureCapacity(minCapacity);
	}
	
	public void addVersionNode(FileNode node){
		children.add(node);
		activeVersion = node;
	}
	
	public void clearVersionNodes(){
		children.clear();
		activeVersion = null;
	}
	
	public void clearActiveVersion(){
		activeVersion = null;
	}
	
	/* --- Setters (Common) --- */
	
	public void setSourcePath(String path){
		if(activeVersion != null) activeVersion.setSourcePath(path);
		super.setSourcePath(path);
	}
	
	public void setUseVirtualSource(boolean b){
		if(activeVersion != null) activeVersion.setUseVirtualSource(b);
		super.setUseVirtualSource(b);
	}
	
	public void setVirtualSourceNode(FileNode vsource){
		if(activeVersion != null) activeVersion.setVirtualSourceNode(vsource);
		super.setVirtualSourceNode(vsource);
	}
	
	public void setOffset(long off){
		if(activeVersion != null) activeVersion.setOffset(off);
		super.setOffset(off);
	}
	
	public void setLength(long len){
		if(activeVersion != null) activeVersion.setLength(len);
		super.setLength(len);
	}
	
	public void setBlockSize(int blockSize){
		if(activeVersion != null) activeVersion.setBlockSize(blockSize);
		super.setBlockSize(blockSize);
	}
	
	public void setBlockSize(int in_block, int out_block){
		if(activeVersion != null) activeVersion.setBlockSize(in_block, out_block);
		super.setBlockSize(in_block, out_block);
	}
	
	public void timestamp(){
		if(activeVersion != null) activeVersion.timestamp();
		super.timestamp();
	}

	public void setTimestamp(ZonedDateTime time){
		if(activeVersion != null) activeVersion.setTimestamp(time);
		super.setTimestamp(time);
	}
	
	public void addEncryption(EncryptionDefinition def){
		if(activeVersion != null) activeVersion.addEncryption(def);
		super.addEncryption(def);
	}
	
	public void addEncryption(EncryptionDefinition def, long offset, long length){
		if(activeVersion != null) activeVersion.addEncryption(def, offset, length);
		super.addEncryption(def, offset, length);
	}
	
	public void setContainerNode(FileNode container_node){
		if(activeVersion != null) activeVersion.setContainerNode(container_node);
		super.setContainerNode(container_node);
	}
	
	public void setTypeChainHead(FileTypeNode head){
		if(activeVersion != null) activeVersion.setTypeChainHead(head);
		super.setTypeChainHead(head);
	}
	
	public void pushTypeChainHead(FileTypeNode type){
		if(activeVersion != null) activeVersion.pushTypeChainHead(type);
		super.pushTypeChainHead(type);
	}
	
	public void addTypeChainNode(FileTypeNode type){
		if(activeVersion != null) activeVersion.addTypeChainNode(type);
		super.addTypeChainNode(type);
	}
	
	public void clearTypeChain(){
		if(activeVersion != null) activeVersion.clearTypeChain();
		super.clearTypeChain();
	}
	
	public int clearTempFiles(){
		if(activeVersion != null) return activeVersion.clearTempFiles();
		return super.clearTempFiles();
	}
	
	protected void setContainerTempPath(String path){
		if(activeVersion != null) activeVersion.setContainerTempPath(path);
		super.setContainerTempPath(path);
	}
	
	/* --- Utility Overrides --- */
	
	protected void copyDataTo(FileNode copy){
		super.copyDataTo(copy);
		if(copy instanceof VersionedFileNode){
			VersionedFileNode vcopy = (VersionedFileNode)copy;
			for(FileNode child : children){
				FileNode ccopy = child.copy(null);
				vcopy.children.add(ccopy);
			}
		}
	}
	
	public FileNode copy(DirectoryNode parent_copy){
		VersionedFileNode copy = new VersionedFileNode(parent_copy, this.getFileName(), children.size()+1);
		copyDataTo(copy);
		
		return copy;
	}
	
	protected void subsetEncryptionRegions(long stpos, long len){
		if(activeVersion != null) activeVersion.subsetEncryptionRegions(stpos, len);
		super.subsetEncryptionRegions(stpos, len);
	}
	
	public boolean splitNodeAt(long off){
		if(activeVersion != null) return activeVersion.splitNodeAt(off);
		return super.splitNodeAt(off);
	}
	
	public FileNode getSubFile(long stoff, long len){
		if(activeVersion != null) return activeVersion.getSubFile(stoff, len);
		return super.getSubFile(stoff, len);
	}
	
	public String getLocationString(){
		if(activeVersion != null) return activeVersion.getLocationString();
		return super.getLocationString();
	}
	
	public FileBuffer loadData(long stpos, long len, int options, String bufferPath) throws IOException{
		if(activeVersion != null) return activeVersion.loadData(stpos, len, options, bufferPath);
		return super.loadData(stpos, len, options, bufferPath);
	}

	protected FileBuffer loadDecompressedData(String bufferPath, int options) throws IOException{
		if(activeVersion != null) return activeVersion.loadDecompressedData(bufferPath, options);
		return super.loadDecompressedData(bufferPath, options);
	}
	
	public boolean copyDataTo(OutputStream out, int options) throws IOException{
		if(activeVersion != null) return activeVersion.copyDataTo(out, options);
		return super.copyDataTo(out, options);
	}
	
	/* --- Debug --- */
	
	public void printMeToStdErr(int indents) {
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();

		System.err.println(tabs + "->" + this.getFileName() + " [VERSIONS: " + children.size() + "]");
	}
	
	public void printMeTo(Writer out, int indents) throws IOException{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		out.write(tabs + "->" + this.getFileName() + " (" + getLocationString() + ") -- VERSIONS: " + children.size() + "\n");
		for(FileNode child : children){
			child.printMeTo(out, indents + 1);
		}
	}
	
	protected String getTypeString(){return "VersionedFileNode";}
	
}

