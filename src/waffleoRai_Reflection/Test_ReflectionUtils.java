package waffleoRai_Reflection;

import java.util.Collection;

import waffleoRai_Utils.FileBuffer;

public class Test_ReflectionUtils {

	public static void main(String[] args) {
		try {
			/*Collection<URL> jarurls = ReflectionUtils.scanJAR("C:\\Users\\Blythe\\Documents\\GitHub\\WaffleoJavaUtils-Base\\jar\\waffleoFilesBase.jar");
			for(URL url : jarurls) {
				ReflectionUtils.classNameFromURL(url, "");
			}*/
			
			ReflectionUtils.scanForAppClasses(ReflectionUtils.class, 0);
			Collection<Class<?>> ccol = ReflectionUtils.findSubclassesOf(FileBuffer.class, false);
			System.err.println("Debug");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

}
