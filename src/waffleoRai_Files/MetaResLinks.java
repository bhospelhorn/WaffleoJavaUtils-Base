package waffleoRai_Files;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import waffleoRai_Utils.DirectoryNode;
import waffleoRai_Utils.FileNode;

public class MetaResLinks {
	
	public static interface WeightFactor{
		public int compareNodes(FileNode t1, FileNode t2);
	}
	
	private static class NodeCand implements Comparable<NodeCand>{

		public FileNode target;
		
		public List<WeightFactor> custom;
		
		public int relation_score; //# dirs away (lower is better)
		public int name_score_1; //How many char in common from start (higher is better)

		public int compareTo; //nodename.compareTo(targetname)
		public String remnants;
		
		public static void weighNames(FileNode node, NodeCand target){
			String n0 = node.getFileName().toLowerCase();
			String n1 = target.target.getFileName().toLowerCase();
			
			//Chop extensions
			int lastdot = n0.lastIndexOf('.');
			if(lastdot >= 0) n0 = n0.substring(0, lastdot);
			lastdot = n1.lastIndexOf('.');
			if(lastdot >= 0) n1 = n1.substring(0, lastdot);
			
			//Count char in common
			int len0 = n0.length(); int len1 = n1.length();
			int minlen = len0;
			if(len1 < len0) minlen = len1;
			for(int i = 0; i < minlen; i++){
				if(n0.charAt(i) != n1.charAt(i)){
					target.name_score_1 = i;
					break;
				}
			}
			
			target.compareTo = n0.compareTo(n1);
			target.remnants = n1.substring(target.name_score_1);
			
		}
		
		public boolean equals(Object o){
			return this==o;
		}

		public int hashCode(){
			return target.hashCode();
		}
		
		public int compareTo(NodeCand o) {

			if(custom != null){
				for(WeightFactor f : custom){
					int comp = f.compareNodes(target, o.target);
					if(comp != 0) return comp;
				}
			}
			
			int diff = this.relation_score - o.relation_score;
			if(diff != 0) return diff;
			
			diff = o.name_score_1 - this.name_score_1;
			if(diff != 0) return diff;
			
			//
			int comp = remnants.compareTo(o.remnants);
			//If comp is positive, this is after o
			if(this.compareTo < 0 && o.compareTo < 0){
				//Both after node name, first is closer
				//Return negative value to prioritize o
				if(comp != 0) return comp * -1;
			}
			else if(this.compareTo > 0 && o.compareTo > 0){
				//Both before node name - later is closer
				if(comp != 0) return comp;
			}
			else{
				//They straddle the original string
				return comp;
			}
			
			return 0;
		}
		
	}
	
	public static boolean linkResource(FileNode node, FileNode target, String linkkey, String uidkey){
		if(node == null || target == null) return false;
		
		//See if target already has UID. Assign one if not.
		String puid = target.getMetadataValue(uidkey);
		if(puid == null){
			int i = target.getFullPath().hashCode();
			puid = Integer.toHexString(i);
			target.setMetadataValue(uidkey, puid);
		}
		
		//Link UID back to node
		node.setMetadataValue(uidkey, puid);
		
		//Convert to relative link
		String rellink = node.findNodeThat(new NodeMatchCallback(){

			public boolean meetsCondition(FileNode n) {
				return n == target;
			}
			
		});
		
		if(rellink == null) rellink = target.getFullPath();
		node.setMetadataValue(linkkey, rellink);
		
		return true;
	}

	public static FileNode findLinkedResource(FileNode node, String linkkey, String uidkey){

		if(node.getParent() == null) return null;
		
		//Look for link
		String reslink = node.getMetadataValue(linkkey);
		if(reslink != null){
			//Look for res at that path
			FileNode target = node.getParent().getNodeAt(reslink);
			if(target != null) return target;
		}
		
		//No link or link broken
		//Match UID
		String uid = node.getMetadataValue(uidkey);
		if(uid != null){
			reslink = node.findNodeThat(new NodeMatchCallback(){

				public boolean meetsCondition(FileNode n) {
					String mypuid = n.getMetadataValue(uidkey);
					if(mypuid == null) return false;
					return mypuid.equals(uid);
				}
				
			});	
			
			//If find match...
			if(reslink != null){
				node.setMetadataValue(linkkey, reslink);
				//Load new link
				FileNode target = node.getParent().getNodeAt(reslink);
				if(target != null) return target;
			}
		}
		
		return null;
	}
	
	private static void findMatchCandidates(List<NodeCand> clist, FileNode node, 
			DirectoryNode dir, List<NodeMatchCallback> filters, 
			List<WeightFactor> factors, int dist){
		
		//Parent
		DirectoryNode parent = dir.getParent();
		if(parent != null) findMatchCandidates(clist, node, parent, filters, factors, dist+1);
		
		//Children
		List<FileNode> children = dir.getChildren();
		for(FileNode child : children){
			if(child instanceof DirectoryNode){
				DirectoryNode cdir = (DirectoryNode)child;
				findMatchCandidates(clist, node, cdir, filters, factors, dist+1);
			}
			else{
				//Thread through filters
				boolean pass = true;
				if(filters != null){
					for(NodeMatchCallback filter : filters){
						if(!filter.meetsCondition(child)){
							pass = false;
							break;
						}
					}
					if(!pass) continue;
				}
				
				//Generate candidate node
				NodeCand cand = new NodeCand();
				cand.target = child;
				cand.custom = factors;
				cand.relation_score = dist;
				NodeCand.weighNames(node, cand);
				
				clist.add(cand);
			}
		}
		
	}
	
	public static List<FileNode> findMatchCandidates(FileNode node, List<NodeMatchCallback> filters){
		return findMatchCandidates(node, filters, null);
	}
	
	public static List<FileNode> findMatchCandidates(FileNode node, List<NodeMatchCallback> filters, List<WeightFactor> factors){

		List<NodeCand> clist = new LinkedList<NodeCand>();
		findMatchCandidates(clist, node, node.getParent(), filters, factors, 0);
		
		Collections.sort(clist);
		List<FileNode> matches = new LinkedList<FileNode>();
		for(NodeCand c : clist){
			matches.add(c.target);
		}
		
		return matches;
	}
	
}
