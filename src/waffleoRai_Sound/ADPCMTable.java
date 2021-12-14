package waffleoRai_Sound;

public abstract class ADPCMTable {

	/*----- Instance Variables -----*/
	
	protected int order;
	protected int gain;
	
	//Ordered from most to least recent
	protected int[] backsamps_start;
	protected int[] backsamps_loop;
	
	protected int shamt_start;
	protected int shamt_loop;
	protected int cidx_start;
	protected int cidx_loop;
	
	/*----- Init -----*/
	
	protected ADPCMTable(int order){
		this.order = order;
		backsamps_start = new int[order];
		backsamps_loop = new int[order];
	}
	
	/*----- Getters -----*/
	
	public int getOrder(){return order;}
	public int getGain(){return gain;}
	public int getStartBackSample(int amt_back){return backsamps_start[amt_back];}
	public int getLoopBackSample(int amt_back){return backsamps_loop[amt_back];}
	
	public int getStartShift(){return shamt_start;}
	public int getStartCoeffIndex(){return cidx_start;}
	public int getLoopShift(){return shamt_loop;}
	public int getLoopCoeffIndex(){return cidx_loop;}
	
	public abstract int getCoefficient(int idx_1d);
	
	/*----- Setters -----*/
	
	public void setStartBacksample(int idx, int value){backsamps_start[idx] = value;}
	public void setLoopBacksample(int idx, int value){backsamps_loop[idx] = value;}
	
}
