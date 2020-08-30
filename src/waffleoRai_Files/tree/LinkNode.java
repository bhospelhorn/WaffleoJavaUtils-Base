package waffleoRai_Files.tree;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;

import waffleoRai_Files.EncryptionDefinition;
import waffleoRai_Files.FileTypeNode;

/*
 * UPDATES
 * 
 * 2020.08.29 | 2.0.0 
 * 	Initial documentation
 * 	Update for compatibility w/ FileNode 3.0.0
 * 	Uses the superclass virtual source for link instead of its own instance var
 * 
 */

/**
 * A <code>FileNode</code> that exists as its own node on a <code>FileNode</code>
 * tree with its own unique path and UID, but references another <code>FileNode</code>
 * as a data source.
 * @author Blythe Hospelhorn
 * @version 2.0.0
 * @since August 29, 2020
 */
public class LinkNode extends FileNode{
	
	/* --- Instance Variables --- */
	
	//private FileNode link;
	
	/*So can reference an individual file in an archive that's stored
	 * inside another archive on disk.
	 */

	/* --- Construction --- */
	
	/**
	 * Create a new <code>LinkNode</code> with the provided parent <code>DirectoryNode</code>,
	 * target, and file name.
	 * @param parent Parent directory to set for node. May be left <code>null</code> and set
	 * later.
	 * @param target Link target node. May be any kind of <code>FileNode</code>, including
	 * a directory. This parameter may be left <code>null</code> and set later.
	 * @param name Name to set for node. May be left <code>null</code> or empty and set later.
	 */
	public LinkNode(DirectoryNode parent, FileNode target, String name) {
		super(parent, name);
		super.setVirtualSourceNode(target);
		/*link = target;
		super.setOffset(0);
		if(link != null){
			super.setLength(link.getLength());
			super.setSourcePath(link.getSourcePath());	
		}*/
		super.setOffset(0);
		if(target != null){
			super.setLength(target.getLength());
			super.setSourcePath(target.getSourcePath());
		}
	}
	
	/**
	 * Create a new <code>LinkNode</code> with the provided parent <code>DirectoryNode</code>,
	 * target, and file name.
	 * @param parent Parent directory to set for node. May be left <code>null</code> and set
	 * later.
	 * @param target Link target node. May be any kind of <code>FileNode</code>, including
	 * a directory. This parameter may be left <code>null</code> and set later.
	 * @param name Name to set for node. May be left <code>null</code> or empty and set later.
	 * @param off Offset of referenced link data relative to start of link target node.
	 * @param size Size of referenced link data.
	 */
	public LinkNode(DirectoryNode parent, FileNode target, String name, long off, long size) {
		super(parent, name);
		/*link = target;
		super.setOffset(off);
		super.setLength(size);
		super.setSourcePath(link.getSourcePath());*/
		super.setVirtualSourceNode(target);
		super.setOffset(off);
		super.setLength(size);
		if(target != null) super.setSourcePath(target.getSourcePath());
	}
	
	/* --- Getters --- */
	
	public Collection<String> getAllSourcePaths(){
		return super.getVirtualSource().getAllSourcePaths();
	}

	/**
	 * Get the node this node references in its link.
	 * @return Linked <code>FileNode</code>
	 */
	public FileNode getLink(){return super.getVirtualSource();}
	
	public boolean isLink(){return true;}
	
	public boolean hasEncryption(){return getLink().hasEncryption();}
	
	public List<EncryptionDefinition> getEncryptionDefChain(){return getLink().getEncryptionDefChain();}
	
	public long[][] getEncryptedRegions(){return getLink().getEncryptedRegions();}
	
	public boolean hasCompression(){return getLink().hasCompression();}
	
	public boolean sourceDataCompressed(){return super.getVirtualSource().sourceDataCompressed();}
	
	public FileNode getContainer(){return getLink().getContainer();}
	
	public FileTypeNode getTypeChainHead(){return getLink().getTypeChainHead();}
	
	public FileTypeNode getTypeChainTail(){return getLink().getTypeChainTail();}
	
	public List<FileTypeNode> getTypeChainAsList(){return getLink().getTypeChainAsList();}
	
	public boolean hasTypingMark(){return getLink().hasTypingMark();}
	
	/**
	 * Get whether this <code>LinkNode</code> is partial reference, that is, whether
	 * it references only part of the source data referenced by its target or the entire
	 * target.
	 * @return True if <code>LinkNode</code> is a partial reference, false if not.
	 */
	public boolean isPartial(){
		if(super.getOffset() != 0) return true;
		if(super.getLength() < super.getVirtualSource().getLength()) return true;
		return false;
	}
	
	/* --- Setters --- */
	
	/**
	 * Set the provided node as the target of this link. This method
	 * also updates the source path, offset, and size relative to the
	 * specified parameters.
	 * @param target Node to set as link target.
	 */
	public void setLink(FileNode target){
		/*link = target;
		super.setOffset(0);
		super.setLength(link.getLength());
		super.setSourcePath(link.getSourcePath());*/
		super.setVirtualSourceNode(target);
		super.setOffset(0);
		if(target != null){
			super.setLength(target.getLength());
			super.setSourcePath(target.getSourcePath());
		}
	}
	
	/**
	 * Set only the link instance variable and update the source path in
	 * the superclass to the link.
	 * @param target Node to set as link target.
	 */
	protected void setLinkOnly(FileNode target){
		//link = target;
		//super.setSourcePath(link.getSourcePath());
		super.setVirtualSourceNode(target);
		if(target != null) super.setSourcePath(target.getSourcePath());
	}
	
	/* --- Comparable --- */
	
	public boolean isDirectory(){
		return super.getVirtualSource().isDirectory();
	}
	
	public boolean equals(Object o)
	{
		if(o == this) return true;
		if(o == null) return false;
		if(!(o instanceof LinkNode)) return false;
		LinkNode fn = (LinkNode)o;
		if(this.isDirectory() != fn.isDirectory()) return false;
		return super.getFileName().equals(fn.getFileName());
	}
	
	public int compareTo(FileNode other)
	{
		if(other == this) return 0;
		if(other == null) return 1;
		
		if(this.isDirectory() && !other.isDirectory()) return -1;
		if(!this.isDirectory() && other.isDirectory()) return 1;
		
		return getFileName().compareTo(other.getFileName());
	}

	/* --- TreeNode --- */
	
	@Override
	public TreeNode getChildAt(int childIndex) {return getLink().getChildAt(childIndex);}

	@Override
	public int getChildCount() {return getLink().getChildCount();}

	@Override
	public boolean getAllowsChildren() {return getLink().getAllowsChildren();}

	@Override
	public boolean isLeaf() {return getLink().isLeaf();}

	@Override
	public Enumeration<TreeNode> children() {return getLink().children();}
	
	/* --- Other --- */
	
	public FileNode copy(DirectoryNode parent_copy)
	{
		return copy(parent_copy, null);
	}
	
	public FileNode copy(DirectoryNode parent_copy, FileNode link_copy)
	{
		LinkNode copy = new LinkNode(parent_copy, link_copy, super.getFileName());
		
		copy.setSourcePath(this.getSourcePath());
		copy.setOffset(this.getOffset());
		copy.setLength(this.getLength());
		
		return copy;
	}
	
	/* --- Debug --- */
	
	public void printMeToStdErr(int indents)
	{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		if(isPartial())
		{
			String off = "0x" + Long.toHexString(getOffset());
			String end = "0x" + Long.toHexString(getOffset() + getLength());
			System.err.println(tabs + "->" + getFileName() + " ---> [" + getLink().getFullPath() + " (" + off + " - " + end + ")]");
		}
		else
		{
			System.err.println(tabs + "->" + getFileName() + " ---> [" + getLink().getFullPath() + "]");
		}
		
	}
	
	
}
