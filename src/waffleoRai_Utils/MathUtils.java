package waffleoRai_Utils;

public class MathUtils {
	
	public static final double LOG10_2 = Math.log10(2.0);

	public static int gcf(int a, int b){
		if(a < b){
			int t = a;
			a = b; b = t;
		}
		
		if(b == 0) return a;
		return gcf(b, a%b);
	}
	
	public static int lcm(int a, int b){
		long prod = (long)a * (long)b;
		int gcf = gcf(a,b);
		if(gcf == 0) return (int)prod;
		return (int)(prod/gcf(a,b));
	}
	
	public static boolean isInteger(double d){
		double floor = Math.floor(d);
		return d == floor;
	}
	
	public static double log2(double d){
		return Math.log10(d)/LOG10_2;
	}
	
}
