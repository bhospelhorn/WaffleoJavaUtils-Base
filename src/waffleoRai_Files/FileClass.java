package waffleoRai_Files;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum FileClass {
	
	COMPRESSED(1),
	EMPTY_FILE(2),
	EMPTY_DIR(3),
	
	ARCHIVE(100),
	
	SYSTEM(200),
	CONFIG_FILE(201),
	TEXT_FILE(202),
	XML(203),
	MARKUP_SCRIPT(204),
	
	EXECUTABLE(300),
	CODELIB(301),
	CODESCRIPT(302),
	
	SOUND_ARC(400),
	SOUND_STREAM(401),
	SOUND_WAVE(402),
	SOUND_WAVEARC(403),
	SOUND_WAVECOL(404), //such as brwsd
	SOUND_SEQ(405),
	SOUNDBANK(406),
	SOUND_MISC(407),
	
	IMG_ANIM_2D(501),
	IMG_FONT(502),
	IMG_ICON(503),
	IMG_IMAGE(504),
	IMG_PALETTE(505),
	IMG_SPRITE_SHEET(506),
	IMG_TILE(507),
	IMG_TEXTURE(508),
	IMG_TILEMAP(509),
	
	_3D_ANIM_3D(601),
	_3D_LIGHTING_DAT(602),
	_3D_MODEL(603),
	_3D_MORPH_DAT(604),
	_3D_MESH(605),
	_3D_RIG_DAT(606),
	_3D_UVMAP(607),
	_3D_MAT_ANIM(608),
	_3D_TXR_ANIM(609),
	_3D_UV_ANIM(610),
	
	DAT_COLLISION(701),
	DAT_LAYOUT(702),
	DAT_STRINGTBL(703),
	DAT_TABLE(704),
	DAT_BANNER(705),
	DAT_HASHTABLE(706),
	
	MOV_MOVIE(801),
	MOV_VIDEO(802),
	
	;
	
	private int val;
	
	private FileClass(int i){
		val = i;
	}
	
	public int getIntegerValue(){return val;}

	//-----------------
	
	private static Map<Integer, FileClass> imap;
	
	public static FileClass getFromInteger(int val){

		if(imap == null){
			imap = new ConcurrentHashMap<Integer, FileClass>();
			for(FileClass v : FileClass.values()) imap.put(v.getIntegerValue(), v);
		}
		
		return imap.get(val);
	}
	
}
