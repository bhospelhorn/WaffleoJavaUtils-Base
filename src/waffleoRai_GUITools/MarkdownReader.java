package waffleoRai_GUITools;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MarkdownReader {
	
	public static final String FONT_NORMAL = "Arial";
	public static final String FONT_CODE = "Courier New";
	public static final String FONT_BLOCKTEXT = "Arial";
	
	public static final int DEFAULT_FONT_SIZE = 11;
	public static final int[] FONT_SIZE_hN = {11, 24, 20, 18, 16, 14, 12};
	public static final int FONT_SIZE_BLOCKTEXT = 10;
	
	public static final char BULLET_CHAR = '-'; //Eventually change to unicode dot
	
	private static final int CHAR_BUFFER_SIZE = 128;
	private static final int SB_BUFFER_SIZE = 1024; //1kb
	
	private InputStream input;
	private StyledDocument output;
	
	private LinkedList<Character> last_chars;
	
	private int read_count;
	private boolean done;
	
	//State
	private StringBuilder buffer;
	private SimpleAttributeSet attr;
	
	//Read state flags
	private boolean hdr_line;
	private boolean esc_next;
	private boolean newline_after; //If newline should be added after this line
	private boolean newline_postfmt;
	private boolean no_reset_as_line; //DON'T reset the attribute set
	
	private boolean in_bold;
	private boolean in_italic;
	private boolean bi_check; //Flag - check to see if next character is ending bold or italic region
	
	private boolean in_code;
	private boolean in_code_multiline;
	private int bt_count;
	
	private boolean in_strikethrough;
	private boolean st_check; //strikethrough
	
	//private boolean bullet_line;
	private int point_number;
	private boolean bullet_space_flag;
	
	private MarkdownReader(InputStream in, StyledDocument doc){
		input = in;
		output = doc;
		read_count = 0;
		last_chars = new LinkedList<Character>();
		done = false;
	}
	
	public static int parseStream(InputStream in, StyledDocument doc) throws IOException{
		MarkdownReader rdr = new MarkdownReader(in, doc);
		try {
			rdr.parseInput();
		} 
		catch (IOException e) {
			throw e;
		} 
		catch (BadLocationException e) {
			e.printStackTrace();
			throw new IOException("Could not write to document!");
		}
		return rdr.read_count;
	}
	
	private static SimpleAttributeSet generateNewDefaultAttrSet(){
		SimpleAttributeSet as = new SimpleAttributeSet();
		StyleConstants.setFontFamily(as, FONT_NORMAL);
		StyleConstants.setFontSize(as, DEFAULT_FONT_SIZE);
		StyleConstants.setForeground(as, Color.black);
		return as;
	}
	
	private static SimpleAttributeSet generateAttrCopy(SimpleAttributeSet as){
		SimpleAttributeSet nas = generateNewDefaultAttrSet();
		
		StyleConstants.setAlignment(nas, StyleConstants.getAlignment(as));
		StyleConstants.setFontFamily(nas, StyleConstants.getFontFamily(as));
		StyleConstants.setFontSize(nas, StyleConstants.getFontSize(as));
		StyleConstants.setBold(nas, StyleConstants.isBold(as));
		StyleConstants.setItalic(nas, StyleConstants.isItalic(as));
		StyleConstants.setStrikeThrough(nas, StyleConstants.isStrikeThrough(as));
		StyleConstants.setForeground(nas, StyleConstants.getForeground(as));
		
		return nas;
	}
	
	private void resetAttrSet(){
		attr = generateNewDefaultAttrSet();
	}
	
	private void dumpBuffer() throws BadLocationException{
		output.insertString(output.getLength(), buffer.toString(), attr);
		buffer = new StringBuilder(SB_BUFFER_SIZE);
	}
	
	private void readLine(String line, int ct) throws BadLocationException{
		//TODO
		//TODO there seems to be an issue with newlines at formatting changes
		//	if the font of the next line is bigger, this causes issues with the previous line being spaced weird
		
		if(line.isEmpty()){
			buffer.append('\n');
			return;
		}
		
		newline_after = false;
		newline_postfmt = false;
		no_reset_as_line = false;
		esc_next = false;
		
		int llen = line.length();
		for(int i = 0; i < llen; i++){
			char c = line.charAt(i);
			
			switch(c){
			case '\\':
				if(esc_next) {esc_next = false; buffer.append(c);}
				else esc_next = true;
				break;
			case '#':
				if(i == 0){
					if(esc_next) {esc_next = false; buffer.append(c);}
					else{
						if(ct != 0) buffer.append("\n");
						hdr_line = true;
						StyleConstants.setFontSize(attr, FONT_SIZE_hN[1]);
						StyleConstants.setBold(attr, true);
						newline_after = true;
						newline_postfmt = true;
					}
				}
				else{
					if(hdr_line && i < 6)StyleConstants.setFontSize(attr, FONT_SIZE_hN[i+1]);
					else buffer.append(c);
				}
				break;
			default:
				buffer.append(c);
				break;
			}
			
			//Flag check
			if(c != '#') hdr_line = false; //Line broken
			
		}
		
		//Check for newline
		if(newline_after) buffer.append('\n');
		
		//Just always dump at end of line.
		dumpBuffer();
		if(!no_reset_as_line) resetAttrSet();
		
		//Reset flags
		hdr_line = false;
		
		//Any post format newlines
		if(newline_postfmt) buffer.append('\n');
		
	}
	
	private void parseInput() throws IOException, BadLocationException{
		if(done) return;
		
		//Wrap input in a Reader
		BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF8"));
		
		//Pull characters...
		String line = null;
		attr = generateNewDefaultAttrSet();
		buffer = new StringBuilder(SB_BUFFER_SIZE);
		int l = 0;
		while((line = br.readLine()) != null){
			readLine(line, l++);
		}
		
		br.close();
		done = true;
	}

}
