package waffleoRai_Containers.media.matroska;

import java.util.LinkedList;
import java.util.List;

import waffleoRai_Containers.ebml.EBMLCommon;
import waffleoRai_Containers.ebml.EBMLElement;
import waffleoRai_Containers.ebml.EBMLMasterElement;
import waffleoRai_Containers.ebml.EBMLStringElement;

public class MatroskaChapterDisplay {
	
	public static final int EBML_BASE_ID = 0x00;
	
	private static final int EBML_ID_STRING = 0x05;
	private static final int EBML_ID_LANGS = 0x37c;
	private static final int EBML_ID_LANGS_BCP47 = 0x37d;
	private static final int EBML_ID_COUNTRIES = 0x37e;
	
	private String chapterString;
	private List<String> languages;
	private List<String> languagesBCP47;
	private List<String> countries;
	
	public MatroskaChapterDisplay() {
		languages = new LinkedList<String>();
		languagesBCP47 = new LinkedList<String>();
		countries = new LinkedList<String>();
	}
	
	/*----- Getters -----*/

	public String getChapterString(){return chapterString;}
	public List<String> getLanguages(){return languages;}
	public List<String> getLanguagesBCP47(){return languagesBCP47;}
	public List<String> getCountries(){return countries;}

	/*----- Setters -----*/

	public void setChapterString(String value){chapterString = value;}
	public void setLanguages(List<String> value){languages = value;}
	public void setLanguagesBCP47(List<String> value){languagesBCP47 = value;}
	public void setCountries(List<String> value){countries = value;}
	
	/*----- Read -----*/
	
	public static MatroskaChapterDisplay fromEBML(EBMLElement element) {
		if(element == null) return null;
		if(element.getUID() != EBML_BASE_ID) return null;
		if(!(element instanceof EBMLMasterElement)) return null;
		
		MatroskaChapterDisplay item = new MatroskaChapterDisplay();
		EBMLMasterElement me = (EBMLMasterElement) element;
		
		EBMLElement e = me.getFirstChildWithId(EBML_ID_STRING);
		if(e == null) return null;
		item.chapterString = EBMLCommon.readStringElement(e);
		
		List<EBMLElement> elist = me.getChildrenWithId(EBML_ID_LANGS);
		if(elist != null) {
			for(EBMLElement ce : elist) {
				String s = EBMLCommon.readStringElement(ce);
				if(s != null) item.languages.add(s);
			}
		}
		
		elist = me.getChildrenWithId(EBML_ID_LANGS_BCP47);
		if(elist != null) {
			for(EBMLElement ce : elist) {
				String s = EBMLCommon.readStringElement(ce);
				if(s != null) item.languagesBCP47.add(s);
			}
		}
		
		elist = me.getChildrenWithId(EBML_ID_COUNTRIES);
		if(elist != null) {
			for(EBMLElement ce : elist) {
				String s = EBMLCommon.readStringElement(ce);
				if(s != null) item.countries.add(s);
			}
		}
		
		return item;
	}
	
	/*----- Write -----*/
	
	public EBMLElement toEBML() {
		if(chapterString == null) return null;

		EBMLMasterElement me = new EBMLMasterElement(EBML_BASE_ID);
		me.addChild(new EBMLStringElement(EBML_ID_STRING, chapterString, true));
		for(String s : languages) {
			me.addChild(new EBMLStringElement(EBML_ID_LANGS, s, false));
		}
		for(String s : languagesBCP47) {
			me.addChild(new EBMLStringElement(EBML_ID_LANGS_BCP47, s, false));
		}
		for(String s : countries) {
			me.addChild(new EBMLStringElement(EBML_ID_COUNTRIES, s, false));
		}

		return me;
	}


}
