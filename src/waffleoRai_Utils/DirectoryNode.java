package waffleoRai_Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.tree.TreeNode;

import waffleoRai_Files.FileClass;
import waffleoRai_Files.NodeMatchCallback;

public class DirectoryNode extends FileNode{

	/* --- Instance Variables --- */
	
	//private Set<FileNode> children;
	private Map<String, FileNode> children;
	private int endIndex;
	
	private FileClass fileclass;
	
	/* --- Interfaces --- */
	
	public interface TreeDumpListener{
		public void onStartNodeDump(FileNode node);
	}
	
	/* --- Construction --- */
	
	public DirectoryNode(DirectoryNode parent, String name)
	{
		super(parent, name);
		//children = new HashSet<FileNode>();
		children = new HashMap<String, FileNode>();
		endIndex = -1;
	}
	
	/* --- Getters --- */
	
	public int getEndIndex(){return endIndex;}
	
	public List<FileNode> getChildren()
	{
		List<FileNode> list = new ArrayList<FileNode>(children.size() + 1);
		list.addAll(children.values());
		Collections.sort(list);
		return list;
	}
	
	private void getDescendants(Collection<FileNode> col, boolean includeDirectories){
		for(FileNode child : children.values()){
			if(child instanceof DirectoryNode){
				if(includeDirectories)col.add(child);
				((DirectoryNode)child).getDescendants(col, includeDirectories);
			}
			else col.add(child);
		}
	}
	
	public Collection<FileNode> getAllDescendants(boolean includeDirectories){
		List<FileNode> list = new LinkedList<FileNode>();
		getDescendants(list, includeDirectories);
		
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
		/*for(FileNode child : children)
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
		}*/
		FileNode child = children.get(cname);
		if(child != null){
			if(splitPath.isEmpty()) return child;
			if(child.isDirectory())
			{
				return ((DirectoryNode)child).getNodeAt(splitPath);
			}
			else return null; //Files have no subfiles
		}
		
		return null;
	}
	
	public FileClass getFileClass(){
		return fileclass;
	}
	
	public boolean hasTypingMark(){
		return (this.fileclass != null);
	}
	
	/* --- Setters --- */
	
	protected void changeChildName(FileNode child, String oldname){
		children.remove(oldname);
		children.put(child.getFileName(), child);
	}
	
	protected void addChild(FileNode node){children.put(node.getFileName(), node);}
	public void clearChildren(){children.clear();}
	public void setEndIndex(int i){endIndex = i;}
	
	public FileNode removeChild(String childname)
	{
		return children.remove(childname);
	}
	
	public boolean removeChild(FileNode child)
	{
		FileNode match = children.remove(child.getFileName());
		if(match != null) return true;
		
		//Look through values for it...
		String rkey = null;
		for(String k : children.keySet()){
			match = children.get(k);
			if(match == child){
				rkey = k;
				break;
			}
		}
		
		if(rkey != null){
			children.remove(rkey);
			return true;
		}
		
		return false;
	}
	
	public void setFileClass(FileClass fc){
		fileclass = fc;
	}
	
	public boolean addChildAt(String targetpath, FileNode node){
		//Move slashes
		String slash = "/";
		targetpath = targetpath.replace("\\", "/");
		
		String[] paths = targetpath.split(slash);
		if(paths == null) return false;
		//Isolate name
		node.setFileName(paths[paths.length-1]);
		if(paths.length == 1){
			node.setParent(this);
			return true;
		}
		
		//Put in a nicer structure
		Deque<String> pathdeque = new LinkedList<String>();
		for(int i = 0; i < paths.length-1; i++) pathdeque.add(paths[i]);
		
		//Navigate dirs and make any subdirectories needed.
		DirectoryNode p = this;
		while(!pathdeque.isEmpty()){
			if(p == null) return false;
			String dname = pathdeque.pop();
			//Look for directory
			if(dname.isEmpty()) continue;
			if(dname.equals(".")) continue;
			if(dname.equals("..")){
				p = p.getParent();
			}
			
			FileNode child = p.children.get(dname);
			if(child == null){
				//Make
				DirectoryNode d = new DirectoryNode(p, dname);
				p = d;
			}
			else{
				if(child instanceof DirectoryNode){
					p = (DirectoryNode)child;
				}
				else return false;	
			}
		}
		
		node.setParent(p);
		return true;
	}
	
	public void setSourcePathForTree(String path){
		super.setSourcePath(path);
		List<FileNode> clist = this.getChildren();
		for(FileNode c : clist){
			if(c instanceof DirectoryNode){
				((DirectoryNode)c).setSourcePathForTree(path);
			}
			else c.setSourcePath(path);
		}
	}
	
	public void incrementTreeOffsetsBy(long value){
		List<FileNode> clist = this.getChildren();
		for(FileNode c : clist){
			if(c instanceof DirectoryNode){
				((DirectoryNode)c).incrementTreeOffsetsBy(value);
			}
			else{
				long off = c.getOffset();
				c.setOffset(off + value);
			}
		}
	}
	
	public void setMetaValueForTree(String key, String value){
		List<FileNode> clist = this.getChildren();
		for(FileNode c : clist){
			if(c instanceof DirectoryNode){
				((DirectoryNode)c).setMetaValueForTree(key, value);
			}
			else{
				c.setMetadataValue(key, value);
			}
		}
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
		if(children.containsValue(node))
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
		
		//TODO double check after fix FileNode
		//Children
		for(FileNode child : children.values()) child.copy(copy);
		
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
		
		//TODO double check after fix FileNode
		for(FileNode child : children.values())
		{
			if(child instanceof DirectoryNode) ((DirectoryNode)child).copyDirectoryTree(copy);
		}
		
		return copy;
	}
	
	public boolean dumpTo(String path) throws IOException
	{
		return dumpTo(path, true, null);
	}
	
	public boolean dumpTo(String path, TreeDumpListener listener) throws IOException
	{
		return dumpTo(path, true, listener);
	}
	
	public boolean dumpTo(String path, boolean auto_decomp) throws IOException
	{
		return dumpTo(path, auto_decomp, null);
	}
	
	public boolean dumpTo(String path, boolean auto_decomp, TreeDumpListener listener) throws IOException
	{
		if(path == null || path.isEmpty()) return false;
		
		if(!FileBuffer.directoryExists(path)) Files.createDirectories(Paths.get(path));
		List<FileNode> children = getChildren();
	
		for(FileNode child : children)
		{
			if(listener != null) listener.onStartNodeDump(child);
			String cpath = path + File.separator + child.getFileName();
			if(child instanceof DirectoryNode)
			{
				((DirectoryNode)child).dumpTo(cpath, auto_decomp, listener);
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
	
	private String findNodeThat(String pstem, NodeMatchCallback cond){

		List<DirectoryNode> cdirs = new LinkedList<DirectoryNode>();
		List<FileNode> children = getChildren();
		
		//Search this level first
		for(FileNode child : children){
			if(child instanceof DirectoryNode){
				cdirs.add((DirectoryNode)child);
			}
			else{
				if(cond.meetsCondition(child)) return pstem + child.getFileName();
			}
		}
		
		//Search dirs if nothing found
		for(DirectoryNode child : cdirs){
			String result = child.findNodeThat(pstem + child.getFileName() + "/", cond);
			if(result != null) return result;
		}
		
		return null;
	}
	
	public String findNodeThat(NodeMatchCallback cond){
		
		String path = "";
		DirectoryNode dir = this;
		while(dir != null){
			String match = dir.findNodeThat(path, cond);
			if(match != null) return match;
			path = "../" + path;
			dir = dir.getParent();
		}
		return null;
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
