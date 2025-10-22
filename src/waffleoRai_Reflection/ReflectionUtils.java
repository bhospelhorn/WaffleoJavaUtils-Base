package waffleoRai_Reflection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import waffleoRai_Utils.FileBuffer;

public class ReflectionUtils {
	
	private static List<ExtURLClassLoader> my_loaders;
	private static List<ClassLoader> other_loaders;
	private static Set<String> app_class_names;
	
	public static String classNameFromURL(URL url, String base) {
		if(url == null) return null;
		String f = url.getFile();
		
		int jarRoot = f.toLowerCase().indexOf(".jar!/");
		if(jarRoot >= 0) {
			f = f.substring(jarRoot + 6);
		}
		else {
			if(base != null) f = f.replace(base, "");
		}
		
		if(f.startsWith("/")) f= f.substring(1);
		f = f.replace(".class", "");
		f = f.replace("/", ".");
		
		return f;
	}
	
	public static Path getLoadedClassFileSource(Class<?> myclass) throws URISyntaxException{
		//https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
		return Paths.get(myclass.getProtectionDomain().getCodeSource().getLocation().toURI());
	}
	
	public static Collection<URL> scanJAR(String jarpath) throws IOException{
		JarFile myjar = new JarFile(jarpath);
		Enumeration<JarEntry> elist = myjar.entries();
		List<URL> urllist = new LinkedList<URL>();
		while(elist.hasMoreElements()){
			JarEntry e = elist.nextElement();
			if(e.isDirectory()) continue; //Skip if dir
			if(!e.getName().endsWith(".class")) continue; //Skip if not class
			
			//Derive class name
			String ename = e.getName();
			//String classname = ename.substring(0, ename.length()-6).replace('/', '.');
			
			//Derive URL
			URL url = new URL("jar:file:" + jarpath + "!/" + ename);
			urllist.add(url);
		}
		myjar.close();
		
		return urllist;
	}
	
	public static Collection<URL> scanDir(Path dirpath) throws IOException{
		List<URL> urllist = new LinkedList<URL>();
		DirectoryStream<Path> dirstr = Files.newDirectoryStream(dirpath);
		for(Path p : dirstr){
			String pstr = p.toAbsolutePath().toString();
			if(FileBuffer.directoryExists(pstr)){
				//Recurse
				urllist.addAll(scanDir(p));
			}
			else{
				if(pstr.endsWith(".jar")){
					urllist.addAll(scanJAR(pstr));
				}
				else if(pstr.endsWith(".class")){
					urllist.add(p.toUri().toURL());
				}
			}
		}
		return urllist;
	}
	
	public static Collection<Class<?>> loadClassesFrom(Path filePath) throws IOException{
		//return loadClassesFrom(filePath, ReflectionUtils.class);
		return loadClassesFrom(filePath, null);
	}
	
	public static Collection<Class<?>> loadClassesFrom(Path filePath, Class<?> parent) throws IOException{
		//Loads classes from a file
		URL[] urls = null;
		if(filePath.endsWith(".jar")){
			//Need to scan jar
			//Different URLs between internal and external JAR?
			//Write method for extracting URLs from JAR?
			String jarpath = filePath.toAbsolutePath().toString();
			Collection<URL> urllist = scanJAR(jarpath);
			
			if(urllist.isEmpty()) return null;
			urls = new URL[urllist.size()];
			int i = 0;
			for(URL url : urllist) urls[i++] = url;
		}
		else if(filePath.endsWith(".class")){
			URL url = filePath.toUri().toURL();
			urls = new URL[]{url};
		}
		else return null;
		
		
		return loadClassesFrom(urls, parent);
	}
	
	public static Collection<Class<?>> loadClassesFrom(URL[] urls, Class<?> parent){
		//Loads classes from disk
		if(my_loaders == null) my_loaders = new LinkedList<ExtURLClassLoader>();
		ExtURLClassLoader cl = null;
		if(parent != null) cl = new ExtURLClassLoader(urls, parent.getClassLoader());
		else cl = new ExtURLClassLoader(urls);
		my_loaders.add(cl);
		return cl.loadAll();
	}
	
	public static Collection<Class<?>> loadClassesFromDir(String dirPath) throws IOException{
		//Loads classes from a directory (scanning it for jar/class files)
		return loadClassesFromDir(dirPath, ReflectionUtils.class);
	}
	
	public static Collection<Class<?>> loadClassesFromDir(String dirPath, Class<?> parent) throws IOException{
		//Loads classes from a directory (scanning it for jar/class files)
		Collection<URL> urllist = scanDir(Paths.get(dirPath));
		if(urllist.isEmpty()) return null;
		URL[] urls = new URL[urllist.size()];
		int i = 0;
		for(URL url: urllist) urls[i++] = url;
		
		return loadClassesFrom(urls, parent);
	}

	public static void scanForAppClasses(Class<?> referenceClass, int packagesUp){
		if(referenceClass == null) return;
		
		try {
			Path refpath = getLoadedClassFileSource(referenceClass);
			String fname = refpath.getFileName().toString();
			if(!Files.isDirectory(refpath)) {
				refpath = refpath.getParent();
			}
			if(!fname.endsWith(".jar")) {
				for(int i = 0; i < packagesUp; i++) {
					refpath = refpath.getParent();
				}
			}
			Collection<URL> urls = scanDir(refpath);
			if(urls.isEmpty()) return;
			
			String refbase = refpath.toAbsolutePath().toString();
			refbase = refbase.replace('\\', '/');
			if(!refbase.startsWith("/")) refbase = "/" + refbase;
			if(app_class_names == null) app_class_names = new HashSet<String>();
			for(URL url : urls) {
				String cname = classNameFromURL(url, refbase);
				if(cname != null) app_class_names.add(cname);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static Collection<Class<?>> findSubclassesOf(Class<?> superclass, boolean allowAbstract){
		List<Class<?>> clist = new LinkedList<Class<?>>();
		
		//Scans system loader first. Then additional loaders.
		if(app_class_names != null) {
			ClassLoader sysLoader = ClassLoader.getSystemClassLoader();
			for(String className : app_class_names) {
				try {
					Class<?> c = Class.forName(className, false, sysLoader);
					if(c != null) {
						if(!allowAbstract){
							if(c.isInterface()) continue;
						}
						if (superclass.isAssignableFrom(c)){
							//System.err.println("Subclass of " + superclass.getName() + " found: " + c.getName());
							clist.add(c);
						}	
					}
				}
				catch(ClassNotFoundException ex) {
					System.err.println("Class \"" + className + "\" expected to be found by app loader, but was not.");
				}
			}	
		}
		
		if(my_loaders != null){
			for(ExtURLClassLoader cl : my_loaders){
				Collection<Class<?>> lclasses = cl.getAll();
				for(Class<?> c : lclasses){
					if(!allowAbstract){
						if(c.isInterface()) continue;
					}
					if (superclass.isAssignableFrom(c)){
						//System.err.println("Subclass of " + superclass.getName() + " found: " + c.getName());
						clist.add(c);
					}
				}
			}
		}
		
		if(other_loaders != null){
			//TODO eh I'll do this later
			//Can I get a system loader?
		}
		
		return clist;
	}
	
	public static boolean registerClassLoader(ClassLoader cl){
		//Allows external code to add a class loaded to the list for ReflectionUtils 
		// to scan when looking for classes
		if(other_loaders == null) other_loaders = new LinkedList<ClassLoader>();
		other_loaders.add(cl);
		return true;
	}
	
	public static boolean clearAddedClassLoaders(){
		if(other_loaders == null) return false;
		other_loaders.clear();
		return true;
	}
	
	public static boolean clearAllClassLoaders(){
		boolean b = clearAddedClassLoaders();
		if(my_loaders == null) return false;
		my_loaders.clear();
		return b;
	}
	
}
