package waffleoRai_Containers.media.matroska;

import java.util.List;

public class MatroskaEditionDisplay {
	
	private String editionString;
	private List<String> languages;
	
	/*----- Getters -----*/

	public String getEditionString(){return editionString;}
	public List<String> getLanguages(){return languages;}

	/*----- Setters -----*/

	public void setEditionString(String value){editionString = value;}
	public void setLanguages(List<String> value){languages = value;}

}
