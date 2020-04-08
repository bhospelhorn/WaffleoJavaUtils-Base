package waffleoRai_soundbank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.tree.TreeNode;

import waffleoRai_Utils.Treenumeration;

public class SoundbankNode implements TreeNode, Comparable<SoundbankNode>{
	
	public static final int NODETYPE_UNKNOWN = -1;
	public static final int NODETYPE_BANK = 1;
	public static final int NODETYPE_PROGRAM = 2;
	public static final int NODETYPE_TONE = 3;
	
	private int type;
	private String name;
	
	private SoundbankNode parent;
	private Map<String, SoundbankNode> children;
	
	private Map<String, String> metadata;
	
	private ArrayList<SoundbankNode> ordered_children;
	
	public SoundbankNode(SoundbankNode parentnode, String nodename){
		this(parentnode, nodename, NODETYPE_UNKNOWN);
	}
	
	public SoundbankNode(SoundbankNode parentnode, String nodename, int nodetype){
		
		type = nodetype;
		name = nodename;
		parent = parentnode;
		if(parent != null) parent.addChild(this);
		children = new TreeMap<String, SoundbankNode>();
		metadata = new HashMap<String, String>();
	}

	public String toString(){return name;}
	public int getBankNodeType(){return type;}
	
	public SoundbankNode getNodeAt(String relative_path)
	{
		//System.err.println("relative_path: " + relative_path);
		if(relative_path == null || relative_path.isEmpty()) return null;
		if(relative_path.equals(".") || relative_path.equals("./")) return this;
		if(relative_path.equals("..") || relative_path.equals("../")) return this.parent;
		if(relative_path.equals("/"))
		{
			//Return root
			SoundbankNode p = this.parent;
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
	
	protected SoundbankNode getNodeAt(Deque<String> splitPath)
	{
		//Look for child with name...
		if(splitPath.isEmpty()) return this;
		
		String cname = splitPath.pop();
		while(cname.isEmpty()) cname = splitPath.pop();
		SoundbankNode child = children.get(cname);
		if(child != null){
			if(splitPath.isEmpty()) return child;
			if(!child.isLeaf())
			{
				return child.getNodeAt(splitPath);
			}
			else return null; //Files have no subfiles
		}
		
		return null;
	}
	
	protected void addChild(SoundbankNode child){
		children.put(child.name, child);
		ordered_children = null;
	}
	
	private void orderChildren(){
		ordered_children = new ArrayList<SoundbankNode>(children.size());
		ordered_children.addAll(children.values());
		Collections.sort(ordered_children);
	}
	
	@Override
	public TreeNode getChildAt(int childIndex) {
		if(ordered_children == null){orderChildren();}
		return ordered_children.get(childIndex);
	}

	@Override
	public int getChildCount() {
		return children.size();
	}

	@Override
	public TreeNode getParent() {
		return parent;
	}

	public int getIndex(TreeNode node) {
		if(ordered_children == null){orderChildren();}
		int ccount = ordered_children.size();
		for(int i = 0; i < ccount; i++){
			if(ordered_children.get(i) == node) return i;
		}
		return -1;
	}

	public boolean getAllowsChildren() {
		return true;
	}

	public boolean isLeaf() {
		return !children.isEmpty();
	}

	public Enumeration<TreeNode> children() {
		if(ordered_children == null){orderChildren();}
		List<TreeNode> list = new ArrayList<TreeNode>(ordered_children.size()+1);
		list.addAll(ordered_children);
		return new Treenumeration(list);
	}

	public boolean equals(Object o){
		return (this == o);
	}
	
	public int hashCode(){
		return name.hashCode();
	}
	
	public int compareTo(SoundbankNode o) {
		if(o == null) return 1;
		if(this.name == null){
			if(o.name == null) return 0;
			return -1;
		}
		return this.name.compareTo(o.name);
	}

	public void addMetadataEntry(String key, String value){
		metadata.put(key, value);
	}
	
	public List<String> getMetadataKeys(){
		List<String> keys = new ArrayList<String>(metadata.size() + 1);
		keys.addAll(metadata.keySet());
		Collections.sort(keys);
		return keys;
	}
	
	public String getMetadataValue(String key){
		return metadata.get(key);
	}
	
}
