package waffleoRai_Utils;

import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;

import waffleoRai_Compression.definitions.CompressionInfoNode;

public class LinkNode extends FileNode{
	
	/* --- Instance Variables --- */
	
	private FileNode link;
	
	/*So can reference an individual file in an archive that's stored
	 * inside another archive on disk.
	 */
	//private boolean isPartial;
	//private boolean srcCompressed;
	//private long offset;
	//private long length;

	/* --- Construction --- */
	
	public LinkNode(DirectoryNode parent, FileNode target, String name) 
	{
		super(parent, name);
		link = target;
		super.setOffset(0);
		if(link != null)
		{
			super.setLength(link.getLength());
			super.setSourcePath(link.getSourcePath());	
		}
	}
	
	public LinkNode(DirectoryNode parent, FileNode target, String name, long off, long size) 
	{
		super(parent, name);
		link = target;
		//isPartial = true;
		//srcCompressed = compressed;
		//offset = off;
		//length = size;
		super.setOffset(off);
		super.setLength(size);
		super.setSourcePath(link.getSourcePath());
	}
	
	/* --- Getters --- */

	public FileNode getLink(){return link;}
	public boolean isLink(){return true;}
	//public boolean isPartialReference(){return isPartial;}
	//public boolean isTargetCompressed(){return srcCompressed;}
	
	public long getOffset(){return super.getOffset() + link.getOffset();}
	public long getRelativeOffset(){return super.getOffset();}
	
	public boolean sourceDataCompressed(){return link.sourceDataCompressed();}
	//public long getOffsetOfCompressionStart(){return link.getOffsetOfCompressionStart();}
	public List<CompressionInfoNode> getCompressionChain(){return link.getCompressionChain();}
	
	public boolean isPartial()
	{
		if(super.getOffset() != 0) return true;
		if(super.getLength() < link.getLength()) return true;
		return false;
	}
	
	/* --- Setters --- */
	
	public void setOffset(long off)
	{
		//long edpos = super.getOffset() + getLength();
		if(off < 0) off = 0;
		if(off >= link.getLength()) off = link.getLength()-1;
		long nedpos = off + getLength();
		if(nedpos >= link.getLength())
		{
			super.setLength(nedpos - link.getLength());
		}
		super.setOffset(off);
	}
	
	public void setLength(long len)
	{
		long maxlen = link.getLength() - super.getOffset();
		if(len > maxlen) len = maxlen;
		if(len < 0) len = 0;
		
		super.setLength(len);
	}
	
	public void setSourcePath(String path)
	{
		super.setSourcePath(path);
		link.setSourcePath(path);
	}
	
	public void setLink(FileNode target)
	{
		link = target;
		super.setOffset(0);
		super.setLength(link.getLength());
		super.setSourcePath(link.getSourcePath());
	}
	
	protected void setLinkOnly(FileNode target)
	{
		link = target;
		super.setSourcePath(link.getSourcePath());
	}

	/*public void addCompressionChainNode(AbstractCompDef def, long stpos, long len)
	{
		link.addCompressionChainNode(def, stpos, len);
	}
	
	public void clearCompressionChain(){link.clearCompressionChain();}*/
	
	
	/* --- Comparable --- */
	
	public boolean isDirectory()
	{
		return link.isDirectory();
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
	public TreeNode getChildAt(int childIndex) {return link.getChildAt(childIndex);}

	@Override
	public int getChildCount() {return link.getChildCount();}

	@Override
	public boolean getAllowsChildren() {return link.getAllowsChildren();}

	@Override
	public boolean isLeaf() {return link.isLeaf();}

	@Override
	public Enumeration<TreeNode> children() 
	{
		return link.children();
	}
	
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
			System.err.println(tabs + "->" + getFileName() + " ---> [" + link.getFullPath() + " (" + off + " - " + end + ")]");
		}
		else
		{
			System.err.println(tabs + "->" + getFileName() + " ---> [" + link.getFullPath() + "]");
		}
		
	}
	
	
}
