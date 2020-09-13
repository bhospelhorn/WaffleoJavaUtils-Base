package waffleoRai_Files.tree;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
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
import waffleoRai_Files.FileNodeModifierCallback;
import waffleoRai_Files.NodeMatchCallback;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.Treenumeration;

/*
 * UPDATES --
 * 
 * Version 3.0.0 | Doc'd
 * 	("Initial" version number chosen arbitrarily)
 * 	Compatibility w/ FileNode 3.0.0
 * 
 * 2020.09.12 | 2.0.0 -> 2.1.0
 * 		Added getNodesThat()
 * 
 */

/**
 * A <code>FileNode</code> subtype that references no data, but is
 * used for building <code>FileNode</code> file trees. It simulates
 * a virtual directory.
 * <br> This class replaces the deprecated <code>VirDirectory</code> class.
 * @author Blythe Hospelhorn
 * @version 2.1.0
 * @since September 12, 2020
 */
public class DirectoryNode extends FileNode{

	/* --- Instance Variables --- */
	
	//private Set<FileNode> children;
	private Map<String, FileNode> children;
	private int scratch32;
	
	private FileClass fileclass;
	
	/* --- Interfaces --- */
	
	/**
	 * A simple interface wrapping a callback method for when a node
	 * dump (copy to disk) is initiated.
	 * @author Blythe Hospelhorn
	 * @version 1.0.0
	 */
	public interface TreeDumpListener{
		
		/**
		 * Callback method used to notify listeners when data for a child
		 * node is being copied to disk as part of a tree dump.
		 * @param node <code>FileNode</code> that is being copied.
		 */
		public void onStartNodeDump(FileNode node);
	}
	
	/* --- Construction --- */
	
	/**
	 * Create a new <code>DirectoryNode</code> that can be used as
	 * a container for <code>FileNode</code>s either containing other
	 * <code>FileNode</code>s or referencing data on disk.
	 * @param parent Parent node of this new node. May be left <code>null</code> and set later.
	 * @param name Name of new node. May be left <code>null</code> or empty and set later.
	 */
	public DirectoryNode(DirectoryNode parent, String name)
	{
		super(parent, name);
		//children = new HashSet<FileNode>();
		children = new HashMap<String, FileNode>();
		//endIndex = -1;
		scratch32 = -1;
	}
	
	/* --- Getters --- */
	
	/**
	 * Get the directory-specific scratch value currently set for this 
	 * <code>DirectoryNode</code>. This may be used for organization or parsing.
	 * Its value is unpredictable unless set explicitly, though it defaults to -1.
	 * @return 32-bit directory scratch value.
	 */
	public int getScratchValue(){return scratch32;}
	
	/**
	 * Get all children of this directory as an ordered list. Sort order is 
	 * set statically in the <code>FileNode</code> class.
	 * @return <code>ArrayList</code> containing all direct children of this
	 * directory. If this directory has no children, this method will return
	 * an empty list.
	 */
	public List<FileNode> getChildren(){
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
	
	/**
	 * Get all descendants of this directory in an unordered list. The sub-directory
	 * nodes themselves may or may not be included as specified.
	 * @param includeDirectories Whether or not to include the actual sub-directory
	 * nodes in the returned list. If this is false, only leaves will be included.
	 * @return Collection containing all descendants of this <code>DirectoryNode</code>
	 * on all levels and in no predictable order.
	 */
	public Collection<FileNode> getAllDescendants(boolean includeDirectories){
		List<FileNode> list = new LinkedList<FileNode>();
		getDescendants(list, includeDirectories);
		
		return list;
	}
	
	public boolean isDirectory()
	{
		return true;
	}
	
	/**
	 * Search the tree this node is a member of for the <code>FileNode</code> at
	 * the specified relative path. Paths are Unix style - delimited by a forward slash
	 * ("<code>/</code>"), where <code>.</code> refers to the current directory and
	 * <code>..</code> refers to the current directory's parent.
	 * @param relative_path Unix-style <code>String</code> path representing the relative
	 * location on this tree of the desired <code>FileNode</code>.
	 * @return The <code>FileNode</code> residing at the specified path, or <code>null</code>
	 * if none found.
	 */
	public FileNode getNodeAt(String relative_path){
		//System.err.println("relative_path: " + relative_path);
		if(relative_path == null || relative_path.isEmpty()) return null;
		if(relative_path.equals(".") || relative_path.equals("./")) return this;
		if(relative_path.equals("..") || relative_path.equals("../")) return this.parent;
		if(relative_path.equals("/")){
			//Return root
			DirectoryNode p = this.parent;
			while(p != null) p = p.parent;
			return p;
		}
		
		//Chops off first slash or ./
		String[] split = relative_path.split("/");
		
		Deque<String> mypath = new LinkedList<String>();
		if(split[0].equals(".")){
			for (int i = 1; i < split.length; i++) mypath.addLast(split[i]);
		}
		else if(split[0].equals("..")){
			for (int i = 1; i < split.length; i++) mypath.addLast(split[i]);
			if(parent == null) return null;
			return parent.getNodeAt(mypath);
		}
		else{
			for (int i = 0; i < split.length; i++) mypath.addLast(split[i]);
		}
		
		return getNodeAt(mypath);
	}
	
	/**
	 * Search the tree for a node with the specified relative path. This version of
	 * the method is intended for internal use, thus is <code>protected</code>. It searches
	 * down a pre-split stack version of the path.
	 * @param splitPath Pre split path.
	 * @return The <code>FileNode</code> residing at the specified path, or <code>null</code>
	 * if none found.
	 */
	protected FileNode getNodeAt(Deque<String> splitPath){
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
	
	/**
	 * Get the <code>FileClass</code> marked for this <code>DirectoryNode</code>. This field is
	 * mostly cosmetic. It does not affect data loading of child nodes, but it can
	 * be used to mark whether this directory was derived from a certain archive type.
	 * @return The <code>FileClass</code> set for this node, or <code>null</code> if none set.
	 */
	public FileClass getFileClass(){
		return fileclass;
	}
	
	public boolean hasTypingMark(){
		return (this.fileclass != null);
	}
	
	/* --- Setters --- */
	
	/**
	 * Update the name of a child node to keep the double link and paths intact.
	 * @param child Child to update link to.
	 * @param oldname Child's old file name. Required to find in map.
	 */
	protected void changeChildName(FileNode child, String oldname){
		children.remove(oldname);
		children.put(child.getFileName(), child);
	}
	
	/**
	 * Add a child to this directory and form double link.
	 * @param node Node to add as child node.
	 */
	protected void addChild(FileNode node){children.put(node.getFileName(), node);}
	
	/**
	 * Clear the PARENT SIDE links to all children of this directory. This method only
	 * clears the list of all children in this node; it does not unlink itself in the
	 * parent fields of all the child nodes.
	 */
	public void clearChildren(){children.clear();}
	
	/**
	 * Set the value in the directory-specific scratch field. 
	 * This may be used for organization or parsing.
	 * Its value is unpredictable unless set explicitly, though it defaults to -1.
	 * @param i Value to set as scratch value.
	 */
	public void setScratchValue(int i){scratch32 = i;}
	
	/**
	 * Break the PARENT SIDE link for the specified child, removing
	 * it from the directory's child map. This does not break the
	 * child side link.
	 * @param childname Name of the child node to remove.
	 * @return Node that was removed, if successful. <code>null</code> if no
	 * such child found.
	 */
	public FileNode removeChild(String childname){
		return children.remove(childname);
	}
	
	/**
	 * Break the PARENT SIDE link for the specified child, removing
	 * it from the directory's child map. This does not break the
	 * child side link.
	 * @param child Node to find and remove.
	 * @return True if node was successfully matched to a child and removed.
	 * <code>null</code> if no such child found or removal failed.
	 */
	public boolean removeChild(FileNode child){
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
	
	/**
	 * Set the <code>FileClass</code> marked for this <code>DirectoryNode</code>. This field is
	 * mostly cosmetic. It does not affect data loading of child nodes, but it can
	 * be used to mark whether this directory was derived from a certain archive type.
	 * @param fc <code>FileClass</code> to mark directory node.
	 */
	public void setFileClass(FileClass fc){
		fileclass = fc;
	}
	
	/**
	 * Add a child node to the tree at the provided relative (Unix-style) path.
	 * If the path references directories that do not currently exist, those
	 * directories will be generated and mounted to the tree.
	 * @param targetpath Path, in the tree this <code>DirectoryNode</code> is part
	 * of, to add the child node at.
	 * @param node Child node to add to tree.
	 * @return True if addition was successful, false if not.
	 */
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

	/* --- Recursive Alterations --- */
	
	/**
	 * Call <code>FileNode.setSourcePath(path)</code> recursively
	 * for all leaf nodes in the tree starting at this node as the
	 * local tree root. This will set the primary source
	 * path for all leaves to the provided path.
	 * <br>Note that this may not be sufficient to alter references to
	 * patchwork nodes that reference multiple files.
	 * @param path New primary source path for all leaves downstream of this node.
	 */
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
	
	/**
	 * Add the provided value to to the offset of all leaves
	 * recursively from this node downstream.
	 * <br>Note that this method only calls <code>FileNode.setOffset</code> of
	 * all target leaves, meaning the result may be unpredictable for node subclasses
	 * that references their source data differently.
	 * @param value Value, in bytes, to increment primary offsets of downstream
	 * leaves by. May be negative.
	 */
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
	
	/**
	 * Set a metadata value recursively for this node and all nodes downstream.
	 * @param key Key of metadata entry to add/set.
	 * @param value Value of metadata entry to add/set.
	 */
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
	
	/**
	 * Execute specified method recursively for every node downstream
	 * of this node.
	 * @param method Wrapped method to execute for each node.
	 */
	public void doForTree(FileNodeModifierCallback method){
		method.doToNode(this);
		List<FileNode> clist = this.getChildren();
		for(FileNode c : clist){
			if(c instanceof DirectoryNode){
				((DirectoryNode)c).doForTree(method);
			}
			else method.doToNode(c);
		}
	}
	
	/* --- Other --- */
	
	public FileNode copy(DirectoryNode parent_copy){
		return copyAsDir(parent_copy);
	}
	
	/**
	 * Copy the node data into a fresh node. This method variant
	 * is directory specific and is called recursively.
	 * @param parent_copy Copy of parent directory node (in case
	 * full tree is being copied).
	 * @return Copy of <code>DirectoryNode</code> containing
	 * copies of downstream nodes.
	 */
	public DirectoryNode copyAsDir(DirectoryNode parent_copy){
		DirectoryNode copy = new DirectoryNode(parent_copy, getFileName());
		copy.setOffset(this.getOffset());
		copy.setLength(this.getLength());
		copy.setSourcePath(this.getSourcePath());
		copy.setScratchValue(this.getScratchValue());
		
		//TODO double check after fix FileNode
		//Children
		for(FileNode child : children.values()) child.copy(copy);
		
		return copy;
	}
	
	/**
	 * Copy the node data into a fresh node. This method variant
	 * only copies the directories, and not the leaves.
	 * @return Copy of <code>DirectoryNode</code> containing
	 * empty copies of all downstream directories.
	 */
	public DirectoryNode copyDirectoryTree(){
		return copyDirectoryTree(null);
	}
	
	/**
	 * Copy the node data into a fresh node. This method variant
	 * only copies the directories, and not the leaves.
	 * @param parent_copy Copy of parent directory node (in case
	 * full tree is being copied).
	 * @return Copy of <code>DirectoryNode</code> containing
	 * empty copies of all downstream directories.
	 */
	public DirectoryNode copyDirectoryTree(DirectoryNode parent_copy){
		DirectoryNode copy = new DirectoryNode(parent_copy, getFileName());
		copy.setOffset(this.getOffset());
		copy.setLength(this.getLength());
		copy.setSourcePath(this.getSourcePath());
		copy.setScratchValue(this.getScratchValue());
		
		//TODO double check after fix FileNode
		for(FileNode child : children.values()){
			if(child instanceof DirectoryNode) ((DirectoryNode)child).copyDirectoryTree(copy);
		}
		
		return copy;
	}
	
	/**
	 * Copy the virtual file tree rooted by this <code>DirectoryNode</code> to disk
	 * at the specified path on the local file system.
	 * @param path Directory on disk to dump this virtual tree to.
	 * @return True if tree dump is successful, false otherwise.
	 * @throws IOException If there is an error reading or writing to disk.
	 */
	public boolean dumpTo(String path) throws IOException{
		return dumpTo(path, true, null);
	}
	
	/**
	 * Copy the virtual file tree rooted by this <code>DirectoryNode</code> to disk
	 * at the specified path on the local file system.
	 * @param path Directory on disk to dump this virtual tree to.
	 * @param listener Wrapped callback method that should be called whenever a new node
	 * is being processed. This can be used to monitor dump progress. May be left null.
	 * @return True if tree dump is successful, false otherwise.
	 * @throws IOException If there is an error reading or writing to disk.
	 */
	public boolean dumpTo(String path, TreeDumpListener listener) throws IOException{
		return dumpTo(path, true, listener);
	}
	
	/**
	 * Copy the virtual file tree rooted by this <code>DirectoryNode</code> to disk
	 * at the specified path on the local file system.
	 * @param path Directory on disk to dump this virtual tree to.
	 * @param auto_decomp Whether or not to attempt auto-decompression on leaf node data. If true,
	 * resulting files from the dump will be decompressed as specified by the leaf nodes. If false,
	 * resulting files will be left as-is.
	 * @return True if tree dump is successful, false otherwise.
	 * @throws IOException If there is an error reading or writing to disk.
	 */
	public boolean dumpTo(String path, boolean auto_decomp) throws IOException{
		return dumpTo(path, auto_decomp, null);
	}
	
	/**
	 * Copy the virtual file tree rooted by this <code>DirectoryNode</code> to disk
	 * at the specified path on the local file system.
	 * @param path Directory on disk to dump this virtual tree to.
	 * @param auto_decomp Whether or not to attempt auto-decompression on leaf node data. If true,
	 * resulting files from the dump will be decompressed as specified by the leaf nodes. If false,
	 * resulting files will be left as-is.
	 * @param listener Wrapped callback method that should be called whenever a new node
	 * is being processed. This can be used to monitor dump progress. May be left null.
	 * @return True if tree dump is successful, false otherwise.
	 * @throws IOException If there is an error reading or writing to disk.
	 */
	public boolean dumpTo(String path, boolean auto_decomp, TreeDumpListener listener) throws IOException{
		if(path == null || path.isEmpty()) return false;
		
		if(!FileBuffer.directoryExists(path)) Files.createDirectories(Paths.get(path));
		List<FileNode> children = getChildren();
	
		for(FileNode child : children){
			if(listener != null) listener.onStartNodeDump(child);
			String cpath = path + File.separator + child.getFileName();
			if(child instanceof DirectoryNode){
				((DirectoryNode)child).dumpTo(cpath, auto_decomp, listener);
			}
			else if(child instanceof LinkNode){}
			else{
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
	
	private void getNodesThat(NodeMatchCallback cond, Collection<FileNode> col){
		//Check this node
		if(cond.meetsCondition(this)) col.add(this);
		
		List<FileNode> children = this.getChildren();
		for(FileNode child : children){
			if(child instanceof DirectoryNode){
				((DirectoryNode)child).getNodesThat(cond, col);
			}
			else{
				if(cond.meetsCondition(child)) col.add(child);
			}
		}
		
	}
	
	/**
	 * Search the tree and return all nodes that fulfill the specified condition.
	 * @param cond Wrapped method that determines whether a node fulfills the condition.
	 * @return Collection of nodes in this tree (including directories) that fulfill
	 * the condition, in no particular order. If none are found, the returned collection is empty.
	 * @since 2.1.0
	 */
	public Collection<FileNode> getNodesThat(NodeMatchCallback cond){
		List<FileNode> nodes = new LinkedList<FileNode>();
		getNodesThat(cond, nodes);
		return nodes;
	}
	
	/**
	 * Quick cast a <code>FileNode</code> to a <code>DirectoryNode</code>. This method
	 * simply does a Java cast, but is provided here as an alternative.
	 * @param node Node to cast. An exception may be thrown if it isn't a <code>DirectoryNode</code>.
	 * @return Node cast as <code>DirectoryNode</code>.
	 */
	public static DirectoryNode castFileNode(FileNode node){
		return (DirectoryNode)node;
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
	
	public void printMeTo(Writer out, int indents) throws IOException{
		StringBuilder sb = new StringBuilder(128);
		for(int i = 0; i < indents; i++) sb.append("\t");
		String tabs = sb.toString();
		
		out.write(tabs + "->" + this.getFileName() + "\n");
		
		List<FileNode> clist = this.getChildren();
		for(FileNode c : clist) c.printMeTo(out, indents+1);
		
	}
	
}
