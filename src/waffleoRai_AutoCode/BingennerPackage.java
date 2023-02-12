package waffleoRai_AutoCode;

import java.util.LinkedList;
import java.util.List;

import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.EnumDef;

public class BingennerPackage {

	/*----- Instance Variables -----*/
	
	private BingennerPackage parent;
	private String name;
	
	private List<BingennerPackage> child_packages;
	private List<BingennerTypedef> child_types;
	private List<EnumDef> child_enums;
	
	/*----- Init -----*/
	
	public BingennerPackage(BingennerPackage parentPkg, String pkgName){
		parent = parentPkg;
		name = pkgName;
		child_packages = new LinkedList<BingennerPackage>();
		child_types = new LinkedList<BingennerTypedef>();
		child_enums = new LinkedList<EnumDef>();
		if(parent != null){
			parent.addChildPackage(this);
		}
	}
	
	/*----- Getters -----*/
	
	public String getName(){return name;}
	public BingennerPackage getParent(){return parent;}
	
	public String getFullPackageString(){
		if(parent != null){
			return parent.getFullPackageString() + "." + name;
		}
		return name;
	}
	
	public BingennerPackage[] getChildPackages(){
		if(child_packages.isEmpty()) return null;
		int ccount = child_packages.size();
		BingennerPackage[] children = new BingennerPackage[ccount];
		int i = 0;
		for(BingennerPackage child : child_packages){
			children[i++] = child;
		}
		return children;
	}
	
	public BingennerTypedef[] getChildTypes(){
		if(child_types.isEmpty()) return null;
		int ccount = child_types.size();
		BingennerTypedef[] children = new BingennerTypedef[ccount];
		int i = 0;
		for(BingennerTypedef child : child_types){
			children[i++] = child;
		}
		return children;
	}
	
	public EnumDef[] getChildEnums(){
		if(child_enums.isEmpty()) return null;
		int ccount = child_enums.size();
		EnumDef[] children = new EnumDef[ccount];
		int i = 0;
		for(EnumDef child : child_enums){
			children[i++] = child;
		}
		return children;
	}
	
	/*----- Setters -----*/
	
	public void setName(String value){name = value;}
	
	public void addChildPackage(BingennerPackage pkg){
		child_packages.add(pkg);
		pkg.parent = this;
	}
	
	public void addChildType(BingennerTypedef def){
		child_types.add(def);
	}
	
	public void addChildEnumType(EnumDef def){
		child_enums.add(def);
	}
	
	public void clearChildren(){
		child_packages.clear();
		child_types.clear();
		child_enums.clear();
	}
	
	/*----- Interface -----*/
	
	/*----- Debug -----*/
	
	public void debug_printToStderr(int tabs){
		for(int i = 0; i < tabs; i++) System.err.print("\t");
		System.err.print("-> [PACKAGE] " + name + "\n");
		for(BingennerTypedef child : child_types){
			child.debug_printToStderr(tabs+1);
		}
		for(BingennerPackage child : child_packages){
			child.debug_printToStderr(tabs+1);
		}
		for(int i = 0; i < tabs+1; i++) System.err.print("\t");
		System.err.print("Enum Defs: " + child_enums.size() + "\n");
	}
	
}
