package waffleoRai_ProgramManage;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import waffleoRai_Utils.FileBuffer;

public abstract class ProgramSettingsManager {

	/*----- Constants -----*/
	
	private static final char SEP = File.separatorChar;
	
	public static final String IKEY_UNIFONT_NAME = "UNICODE_FONT";
	private static final String[] TRYFONTS = {"Arial Unicode MS", "MS PGothic", "MS Gothic", 
			"AppleGothic", "Takao PGothic",
			"Hiragino Maru Gothic Pro", "Hiragino Kaku Gothic Pro"};
	
	/*----- Instance Variables -----*/
	
	protected String root_dir;
	protected String settings_file_name;
	
	protected String my_unifont;
	protected Map<String, String> settings;
	
	/*----- Init -----*/
	
	protected ProgramSettingsManager(){
		settings_file_name = "settings.ini";
		settings = new HashMap<String, String>();
	}
	
	/*----- Getters -----*/
	
	public String getProgramRootDirectory(){return root_dir;}
	public String getSetting(String key){return settings.get(key);}
	
	public String getProgramFilePath(String local_path){
		if(local_path == null) return null;
		return root_dir + SEP + local_path.replace('/', SEP);
	}
	
	public Font getUnicodeFont(int style, int size){
		if(my_unifont != null) return new Font(my_unifont, style, size);
		
		//Try the key...
		String fontkey = getSetting(IKEY_UNIFONT_NAME);
		
		if(fontkey != null) my_unifont = fontkey;
		else{
			//See what's on this system
			String[] flist = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			for(String name : TRYFONTS){
				if(my_unifont != null) break;
				for(String f : flist){
					if(f.equalsIgnoreCase(name)){
						my_unifont = name;
						//System.err.println("Unicode font detected: " + my_unifont);
						break;
					}
				}
			}
			setSetting(IKEY_UNIFONT_NAME, my_unifont);
		}
		
		return new Font(my_unifont, style, size);
	}
	
	/*----- Setters -----*/
	
	public void setSetting(String key, String value){settings.put(key, value);}
	
	/*----- Parent Internal -----*/
	
	private void loadSettingsFile() throws IOException{
		settings.clear();
		if(settings_file_name == null) return;
		String filepath = getProgramFilePath(settings_file_name);
		if(!FileBuffer.fileExists(filepath)) return;
		
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line = null;
		while((line = br.readLine()) != null){
			if(line.isEmpty()) continue;
			String[] fields = line.split("=");
			if(fields.length > 1){
				settings.put(fields[0], fields[1]);
			}
			else{
				settings.put(fields[0], "");
			}
		}
		br.close();
	}
	
	private void saveSettingsFile() throws IOException{
		if(settings_file_name == null) return;
		String filepath = getProgramFilePath(settings_file_name);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(filepath));
		for(Entry<String, String> setting : settings.entrySet()){
			bw.write(setting.getKey() + "=" + setting.getValue() + "\n");
		}
		bw.close();
	}
	
	/*----- Internal Interface -----*/
	
	protected abstract boolean boot_internal() throws IOException;
	protected abstract boolean shutdown_internal() throws IOException;
	
	protected void makeFolders(String[] dir_names) throws IOException{
		for(String dir : dir_names){
			String dirpath = getProgramFilePath(dir);
			if(!FileBuffer.directoryExists(dirpath)){
				Files.createDirectories(Paths.get(dirpath));
			}
		}
	}
	
	/*----- Interface -----*/
	
	public boolean boot(String program_dir) throws IOException{
		root_dir = program_dir;
		loadSettingsFile();
		return boot_internal();
	}
	
	public boolean shutdown() throws IOException{
		saveSettingsFile();
		return shutdown_internal();
	}
	
	public abstract boolean install(String target_dir);
	public abstract boolean uninstall();
	
}
