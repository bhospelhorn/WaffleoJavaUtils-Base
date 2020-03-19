package waffleoRai_Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.tree.TreeNode;

public class DirectoryNode extends FileNode{

	/* --- Instance Variables --- */
	
	private Set<FileNode> children;
	private int endIndex;
	
	/* --- Construction --- */
	
	public DirectoryNode(DirectoryNode parent, String name)
	{
		super(parent, name);
		children = new HashSet<FileNode>();
		endIndex = -1;
	}
	
	/* --- Getters --- */
	
	public int getEndIndex(){return endIndex;}
	
	public List<FileNode> getChildren()
	{
		List<FileNode> list = new ArrayList<FileNode>(children.size() + 1);
		list.addAll(children);
		Collections.sort(list);
		return list;
	}
	
	public boolean isDirectory()
	{
		return true;
	}
	
	public FileNode getNodeAt(String relative_path)
	{
		//System.err.println("relative_path: " + relative_path);
		if(relative_path == null || relative_path.isEmpty()) return null;
		if(relative_path.equals(".") || relative_path.equals("./")) return this;
		if(relative_path.equals("..") || relative_path.equals("../")) return this.parent;
		if(relative_path.equals("/"))
		{
			//Return root
			DirectoryNode p = this.parent;
			while(p != null) p = p.parent;
			return p;
		}
		
		//Chops off first slash or ./
		String[] split = relative_path.split("/");
		
		Deque<String> mypath = new LinkedList<String>();
		if(split[0].equals("."))
		{
			for (int i = 1; i < split.length; i++) mypath.addLast(split[i]);
		}
		else if(split[0].equals(".."))
		{
			for (int i = 1; i < split.length; i++) mypath.addLast(split[i]);
			if(parent == null) return null;
			return parent.getNodeAt(mypath);
		}
		else
		{
			for (int i = 0; i < split.length; i++) mypath.addLast(split[i]);
		}
		
		return getNodeAt(mypath);
	}
	
	protected FileNode getNodeAt(Deque<String> splitPath)
	{
		//Look for child with name...
		if(splitPath.isEmpty()) return this;
		
		String cname = splitPath.pop();
		while(cname.isEmpty()) cname = splitPath.pop();
		//System.err.println("looking for child with name: " + cname);
		for(FileNode child : children)
		{
			if(cname.equals(child.getFileName()))
			{
				if(splitPath.isEmpty()) return child;
				if(child.isDirectory())
				{
					return ((DirectoryNode)child).getNodeAt(splitPath);
				}
				else return null; //Files have no subfiles
			}
		}
		
		return null;
	}
	
	/* --- Setters --- */
	
	protected void addChild(FileNode node){children.add(node);}
	public void clearChildren(){children.clear();}
	public void setEndIndex(int i){endIndex = i;}
	
	public FileNode removeChild(String childname)
	{
		Set<FileNode> new_children = new HashSet<FileNode>();
		FileNode match = null;
		for(FileNode child : children)
		{
			if(child.getFileName().equals(childname)) match = child;
			else new_children.add(child);
		}
		return match;
	}
	
	public boolean removeChild(FileNode child)
	{
		return children.remove(child);
	}
	
	/* --- TreeNode --- */
	
	@Override
	public TreeNode getChildAt(int childIndex) 
	{
		List<FileNode> childlist = this.getChildren();
		return childlist.get(childIndex);
	}

	@Override
	public int getChildCount() {return children.size();}

	@Override
	public int getIndex(TreeNode node) 
	{
		if(children.contains(node))
		{
			List<FileNode> clist = this.getChildren();
			int ccount = clist.size();
			for(int i = 0; i < ccount; i++)
			{
				if(clist.get(i).equals(node)) return i;
			}
		}
		return -1;
	}

	@Override
	public boolean getAllowsChildren() {return true;}

	@Override
	public boolean isLeaf() 
	{
		return (children.isEmpty());
	}

	@Override
	public Enumeration<TreeNode> children() 
	{
		List<TreeNode> list = new ArrayList<TreeNode>(children.size()+1);
		list.addAll(getChildren());
		return new Treenumeration(list);
	}

	/* --- Other --- */
	
	public FileNode copy(DirectoryNode parent_copy)
	{
		return copyAsDir(parent_copy);
	}
	
	public DirectoryNode copyAsDir(DirectoryNode parent_copy)
	{
		DirectoryNode copy = new DirectoryNode(parent_copy, getFileName());
		copy.setOffset(this.getOffset());
		copy.setLength(this.getLength());
		copy.setSourcePath(this.getSourcePath());
		copy.setEndIndex(this.getEndIndex());
		
		//Children
		for(FileNode child : children) child.copy(copy);
		
		return copy;
	}
	
	public DirectoryNode copyDirectoryTree()
	{
		return copyDirectoryTree(null);
	}
	
	public DirectoryNode copyDirectoryTree(DirectoryNode parent_copy)
	{
		DirectoryNode copy = new DirectoryNode(parent_copy, getFileName());
		copy.setOffset(this.getOffset());
		copy.setLength(this.getLength());
		copy.setSourcePath(this.getSourcePath());
		copy.setEndIndex(this.getEndIndex());
		
		for(FileNode child : children)
		{
			if(child instanceof DirectoryNode) ((DirectoryNode)child).copyDirectoryTree(copy);
		}
		
		return copy;
	}
	
	public boolean dumpTo(String path) throws IOException
	{
		if(path == null || path.isEmpty()) return false;
		
		if(!FileBuffer.directoryExists(path)) Files.createDirectories(Paths.get(path));
		List<FileNode> children = getChildren();
	
		for(FileNode child : children)
		{
			String cpath = path + File.separator + child.getFileName();
			if(child instanceof DirectoryNode)
			{
				((DirectoryNode)child).dumpTo(cpath);
			}
			else if(child instanceof LinkNode){}
			else
			{
				FileBuffer buffer = child.loadDecompressedData();
				buffer.writeFile(cpath);
			}
		}
		
		return true;
	}
	
	public boolean dumpTo(String path, boolean auto_decomp) throws IOException
	{
		if(path == null || path.isEmpty()) return false;
		
		if(!FileBuffer.directoryExists(path)) Files.createDirectories(Paths.get(path));
		List<FileNode> children = getChildren();
	
		for(FileNode child : children)
		{
			String cpath = path + File.separator + child.getFileName();
			if(child instanceof DirectoryNode)
			{
				((DirectoryNode)child).dumpTo(cpath);
			}
			else if(child instanceof LinkNode){}
			else
			{
				FileBuffer buffer = null;
				if(auto_decomp) buffer = child.loadDecompressedData();
				else buffer = child.loadData();
				buffer.writeFile(cpath);
			}
		}
		
		return true;
	}
	
	public boolean copyDataTo(String path, boolean decompress) throws IOException
	{
		return dumpTo(path, decompress);
	}
	
	public boolean copyDataTo(OutputStream out, boolean decompress) throws IOException
	{
		return false;
	}
	
	/* --- Debug --- */
	
	public void printMeToStdErr(int indents)
	{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		System.err.println(tabs + "->" + this.getFileName());
		
		List<FileNode> clist = this.getChildren();
		for(FileNode c : clist) c.printMeToStdErr(indents+1);
		
	}
	
}
