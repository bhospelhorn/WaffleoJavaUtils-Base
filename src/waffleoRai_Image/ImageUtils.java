package waffleoRai_Image;

public class ImageUtils {
	
	public static int[] getDefaultPalette2() {
		int[] plt = new int[4];
		for(int i = 0; i < 4; i++) {
			int val = 0;
			for(int j = 0; j < 12; j++) {
				val <<= 2;
				val |= (i & 0x3);
			}
			val |= 0xff000000;
			plt[i] = val;
		}
		
		return plt;
	}

	public static int[] getDefaultPalette4() {
		int[] plt = new int[16];
		for(int i = 0; i < 16; i++) {
			int val = 0;
			for(int j = 0; j < 6; j++) {
				val <<= 4;
				val |= (i & 0xf);
			}
			val |= 0xff000000;
			plt[i] = val;
		}
		
		return plt;
	}
	
	public static int[] getDefaultPalette8() {
		int[] plt = new int[256];
		for(int i = 0; i < 256; i++) {
			int val = 0;
			for(int j = 0; j < 3; j++) {
				val <<= 8;
				val |= (i & 0xff);
			}
			val |= 0xff000000;
			plt[i] = val;
		}
		
		return plt;
	}
	
	public static int[] simplifyPalette(Palette plt) {
		if(plt == null) return null;
		int[] out = new int[1 << plt.getBitDepth()];
		for(int i = 0; i < out.length; i++) {
			int val = plt.getRGBA(i);
			int alpha = val & 0xff;
			val >>>= 8;
			val |= alpha << 24;
			out[i] = val;
		}
		return out;
	}
	
	public static int scale5BitColor(int val5){
		if (val5 < 0 || val5 >= 32) return val5;
		if (val5 == 0) return 0;
		if (val5 == 1) return 8;
		if (val5 == 2) return 16;
		if (val5 == 31) return 255;
		double l2 = Math.log10((double)val5 / 2.0) / Math.log10(2.0);
		l2 *= 8.0;
		//Now scale to 255
		double factor = 255.0/32.0;
		double temp = factor * l2;
		return (int)Math.round(temp);
	}
	
}
