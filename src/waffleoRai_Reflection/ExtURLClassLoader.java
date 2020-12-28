package waffleoRai_Reflection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

//https://stackoverflow.com/questions/1781091/java-how-to-load-class-stored-as-byte-into-the-jvm

public class ExtURLClassLoader extends URLClassLoader{
	
	private static final int LOAD_ALLOC = 0x100000;
	
	private Map<String, URL> name_map;
	private Map<String, Class<?>> class_map;
	
	private Set<String> search_paths;
	
	public ExtURLClassLoader(URL[] urls) {
	     super(urls);
	     mapNames();
	     class_map = new HashMap<String, Class<?>>();
	}

	public ExtURLClassLoader(URL[] urls, ClassLoader parent) {
	     super(urls, parent);
	     mapNames();
	     class_map = new HashMap<String, Class<?>>();
	}
	
	private byte[] loadFromURL(URL u) throws IOException{
		ArrayList<Byte> llist = new ArrayList<Byte>(LOAD_ALLOC); //1MB
		InputStream is = u.openStream();
		int b = -1;
		while((b = is.read()) != -1) llist.add((byte)b);
		is.close();
		
		//Copy to single byte array
		int sz = llist.size();
		byte[] data = new byte[sz];
		for(int j = 0; j < sz; j++) data[j] = llist.get(j);
		
		return data;
	}
	
	private Class<?> loadClassFromURL(URL url) throws IOException{
		byte[] data = loadFromURL(url);
		Class<?> c = super.defineClass(null, data, 0, data.length);
		return c;
	}
	
	private void mapNames(){
		name_map = new HashMap<String, URL>();
		search_paths = new HashSet<String>();
		
		//Extract the class names from all the provided files
		URL[] urls = super.getURLs();
		if(urls == null) return;
		int count = urls.length;
		for(int i = 0; i < count; i++){
			URL u = urls[i];
			if(u == null) continue;
			
			String ustr = u.toString();
			int lastslash = ustr.lastIndexOf('/');
			if(lastslash >= 0) ustr = ustr.substring(0, lastslash);
			search_paths.add(ustr+"/");
			
			try {
				//Read as class and extract name
				Class<?> c = loadClassFromURL(u);
				if(c == null) continue;
				name_map.put(c.getName(), u);
			} 
			catch (IOException e) {
				e.printStackTrace();
				continue;
			}	
		}
	}

	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		//If already loaded, return that.
		Class<?> c = class_map.get(name);
		if(c != null) return c;
		
		URL url = name_map.get(name);
		if(url == null) throw new ClassNotFoundException("Class \"" + name + "\" not found by this loader!");
		try {
			c = loadClassFromURL(url);
			class_map.put(name, c);
			//System.err.println("--DEBUG-- ExtURLClassLoader.findClass || loaded class " + c.getName());
			return c;
		} 
		catch (IOException e) {
			e.printStackTrace();
			throw new ClassNotFoundException("Class \"" + name + "\" found, but could not be loaded (I/O error)");
		}
	}
	
	public Collection<String> getAllNames(){
		List<String> nlist = new LinkedList<String>();
		nlist.addAll(name_map.keySet());
		return nlist;
	}
	
	public Collection<Class<?>> getAll(){
		List<Class<?>> clist = new LinkedList<Class<?>>();
		List<String> nlist = new LinkedList<String>();
		nlist.addAll(name_map.keySet());
		

		for(String n : nlist){
			try{
				Class<?> c = findClass(n); //NOT happy if you try to reload
				clist.add(c);
			}
			catch(ClassNotFoundException x){
				x.printStackTrace();
			}
		}
		
		return clist;
	}
	
	public Collection<Class<?>> loadAll(){
		List<Class<?>> clist = new LinkedList<Class<?>>();
		List<String> nlist = new LinkedList<String>();
		nlist.addAll(name_map.keySet());

		for(String n : nlist){
			try{
				//System.err.println("--DEBUG-- ExtURLClassLoader.loadAll || looking for " + n);
				Class<?> c = loadClass(n);
				clist.add(c);
				class_map.put(n, c);
			}
			catch(ClassNotFoundException x){
				x.printStackTrace();
			}
		}
		
		return clist;
	}
	
	 public URL findResource(final String name) {
		 
		 //Alright, well it looks like the path is always the launch path.
		 //So I'll have to do some manual messing around to get it to look relative to the classes here...
		 URL out = super.findResource(name);
		 if(out == null){
			 //Look through URLs here.
			 for(String rpath : search_paths){
				 //String add = name;
				 //if(!add.startsWith("/")) add = "/" + add;
				 String tpath = rpath + name;
				 
				 //DEBUG
				// System.err.println("Trying " + tpath);
				 
				 try{
					 URL url = new URL(tpath);
					 InputStream test = url.openStream();
					 if(test != null){
						 test.close();
						 //System.err.println("Match found: " + tpath);
						 return url;
					 }
				 }
				 catch(IOException x){
					// System.err.println("Nothing found at " + tpath);
					 continue;
				 }
				 
			 }
		 }
		 
		 return out;
	 }
	
	
}
