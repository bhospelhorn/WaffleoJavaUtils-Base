package waffleoRai_AutoCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.EnumDef;
import waffleoRai_Files.XMLReader;

public abstract class Bingenner {
	
	/*----- Constants -----*/
	
	public static final String[] PRIM_TYPES = {"s8", "u8", "s16", "u16", "s32", "u32", "s64", "u64", "f32", "f64"};
	
	public static final int BYTEORDER_SYSTEM = 0;
	public static final int BYTEORDER_BIG = 1;
	public static final int BYTEORDER_LITTLE = 2;
	public static final int BYTEORDER_PARENT = 3;
	
	/*----- Static Variables -----*/
	
	private static boolean sys_byte_order = false;
	
	/*----- Instance Variables -----*/
	
	private int force_pad; //Force padding to a multiple of this many bytes
	
	private boolean byte_order = true;
	
	protected Path output_root = null;
	protected String current_output_dir = null;
	
	protected BingennerPackage base_package = null;
	protected Map<String, BingennerTypedef> loaded_types;
	protected Map<String, EnumDef> loaded_enums;
	
	/*----- Init -----*/
	
	protected Bingenner(){
		force_pad = 0;
		loaded_types = new HashMap<String, BingennerTypedef>();
		loaded_enums = new HashMap<String, EnumDef>();
	}
	
	/*----- Getters -----*/
	
	public static boolean getTargetSystemByteOrder(){return sys_byte_order;}
	
	public int getPadding(){return this.force_pad;}
	public boolean getByteOrder(){return this.byte_order;}
	public BingennerTypedef getDefinition(String typename){return loaded_types.get(typename);}
	public EnumDef getEnumDef(String typename){return loaded_enums.get(typename);}
	
	/*----- Setters -----*/
	
	public static void setTargetSystemByteOrder(boolean big_endian){sys_byte_order = big_endian;}
	
	public void setByteOrder(boolean value){this.byte_order = true;}
	public void setPadding(int value){this.force_pad = value;}
	
	/*----- Internal -----*/
	
	protected void readSubclassDefinedTopNode(Element node){}
	
	protected BingennerPackage readInXML(BingennerPackage parent_pack, String xmlpath) throws ParserConfigurationException, SAXException, IOException{
		String docname = xmlpath.substring(xmlpath.lastIndexOf(File.separatorChar)+1);
		docname = docname.substring(0, docname.indexOf(".xml"));
		BingennerPackage xml_pkg = new BingennerPackage(parent_pack, docname);
		
		Document xmldoc = XMLReader.readXMLStatic(xmlpath);
		Node pkgnode = xmldoc.getFirstChild();
		while(pkgnode != null && !pkgnode.getNodeName().equals("DefPackage")){
			pkgnode = pkgnode.getNextSibling();
		}
		if(pkgnode == null){
			System.err.println("Bingenner.readInXML || DefPackage node was not found!");
			return null;
		}
		
		NodeList all_children = pkgnode.getChildNodes();
		
		//By default, "DataStruct" is the only top level struct type it recognizes.
		int ccount = all_children.getLength();
		for(int i = 0; i < ccount; i++){
			Node n = all_children.item(i);
			if(n.getNodeType() == Node.ELEMENT_NODE){
				Element child = (Element)n;
				String cname = child.getNodeName();
				if(cname.equals("DataStruct")){
					if(!child.hasAttribute("Name")) continue;
					BingennerTypedef ntype = new BingennerTypedef(xml_pkg, child);
					this.loaded_types.put(ntype.getName(), ntype);
				}
				else if(cname.equals("EnumDef")){
					if(!child.hasAttribute("Name")) continue;
					EnumDef etype = new EnumDef(child);
					etype.setParentPackage(xml_pkg);
					this.loaded_enums.put(etype.getName(), etype);
				}
				else{
					readSubclassDefinedTopNode(child);
				}
			}
		}
		return xml_pkg;
	}
	
	protected BingennerPackage readInXMLsDir(BingennerPackage parent_pack, Path dir) throws IOException, ParserConfigurationException, SAXException{
		String myname = dir.getFileName().toString();
		BingennerPackage my_pack = new BingennerPackage(parent_pack, myname);
		
		DirectoryStream<Path> dirstr = Files.newDirectoryStream(dir);
		for(Path childpath : dirstr){
			String child_name = childpath.getFileName().toString();
			if(Files.isDirectory(childpath)){
				readInXMLsDir(my_pack, childpath);
			}
			else{
				if(child_name.endsWith(".xml")){
					readInXML(my_pack, childpath.toAbsolutePath().toString());
				}
			}
		}
		dirstr.close();
		return my_pack;
	}
	
	/*----- Inherited Interface -----*/
	
	protected abstract void newPackage(BingennerPackage pkg) throws IOException;
	protected abstract void outputType(BingennerTypedef def) throws IOException;
	protected abstract void outputEnumType(EnumDef def) throws IOException;
	
	protected void packageOut(BingennerPackage pkg) throws IOException{
		newPackage(pkg);
		
		//Do types first...
		BingennerTypedef[] ptypes = pkg.getChildTypes();
		if(ptypes != null){
			for(BingennerTypedef def : ptypes) outputType(def);
		}
		
		//Do enum...
		EnumDef[] etypes = pkg.getChildEnums();
		if(etypes != null){
			for(EnumDef def : etypes) outputEnumType(def);
		}
		
		//Then child packages...
		BingennerPackage[] cpkg = pkg.getChildPackages();
		if(cpkg != null){
			for(BingennerPackage child : cpkg) packageOut(child);
		}
	}
	
	/*----- Public Interface -----*/
	
	public void readFrom(String indir) throws IOException{
		try{
			Path indir_path = Paths.get(indir);
			if(!Files.isDirectory(indir_path)) throw new IOException("Directory \"" + indir + "\" does not exist!");
			//String root_name = indir_path.getFileName().toString();
			base_package = readInXMLsDir(null, indir_path);
		}
		catch(ParserConfigurationException | SAXException ex){
			ex.printStackTrace();
			throw new IOException("XML parsing error encountered!");
		}
	}
	
	public void outputTo(String outdir) throws IOException{
		if(outdir == null) return;
		
		output_root = Paths.get(outdir);
		/*String pack_name = output_root.getFileName().toString();
		if(!Files.isDirectory(output_root)){
			Files.createDirectories(output_root);
		}
		base_package.setName(pack_name);*/
		packageOut(base_package);
		output_root = null;
		current_output_dir = null;
	}
	
	public void DEBUG_printToStderr(){
		System.err.println("Type Count: " + this.loaded_types.size());
		this.base_package.debug_printToStderr(0);
	}
	
}