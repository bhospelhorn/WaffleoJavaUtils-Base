package waffleoRai_Image;

import java.awt.image.BufferedImage;

public class RecursiveTile extends Tile{
	
	private int dim_subtile; //Pixels per subtile row
	private int dim_tile;  //Subtiles per tile row
	
	private Tile[][] tiles; //Stores x,y

	public RecursiveTile(int bits, int subtile_dim, int subtiles_per_row) {
		super(bits, subtile_dim * subtiles_per_row);
		dim_subtile = subtile_dim;
		dim_tile = subtiles_per_row;
		
		tiles = new Tile[dim_tile][dim_tile];
	}
	
	public int getValueAt(int x, int y){
		if(x < 0) throw new IndexOutOfBoundsException("x coordinate out of bounds!");
		if(y < 0) throw new IndexOutOfBoundsException("y coordinate out of bounds!");
		
		int tx = x/dim_subtile;
		int ty = y/dim_subtile;
		
		int xo = x%dim_subtile;
		int yo = y&dim_subtile;
		
		Tile t = tiles[tx][ty];
		return t.getValueAt(xo, yo);
	}
	
	public void setValueAt(int x, int y, int val){
		if(x < 0) throw new IndexOutOfBoundsException("x coordinate out of bounds!");
		if(y < 0) throw new IndexOutOfBoundsException("y coordinate out of bounds!");
		
		int tx = x/dim_subtile;
		int ty = y/dim_subtile;
		
		int xo = x%dim_subtile;
		int yo = y&dim_subtile;
		
		Tile t = tiles[tx][ty];
		t.setValueAt(xo, yo, val);
	}
	
	public Tile getTileAt(int tx, int ty){
		if(tx < 0 || tx >= tiles.length) throw new IndexOutOfBoundsException("Tile x coordinate out of bounds!");
		if(ty < 0 || ty >= tiles[0].length) throw new IndexOutOfBoundsException("Tile y coordinate out of bounds!");

		return tiles[tx][ty];
	}
	
	public void setTileAt(int tx, int ty, Tile tile){
		if(tx < 0 || tx >= tiles.length) throw new IndexOutOfBoundsException("Tile x coordinate out of bounds!");
		if(ty < 0 || ty >= tiles[0].length) throw new IndexOutOfBoundsException("Tile y coordinate out of bounds!");

		//Must match dimensions! Otherwise reject!
		if(tile.getDimension() != this.dim_subtile) throw new IndexOutOfBoundsException("Tile dimension is invalid!");
		tiles[tx][ty] = tile;
	}
	
	public void copyTo(BufferedImage img, int x, int y, boolean flipx, boolean flipy, Palette p){
		
		int xx = x; int yy = y;
		
		for(int tr = 0; tr < dim_tile; tr++){
			for(int tl = 0; tl < dim_tile; tl++){
				Tile t = tiles[tl][tr];
				t.copyTo(img, xx, yy, flipx, flipy, p);
				xx += dim_subtile;
			}
			xx = x; yy += dim_subtile;
		}
		
	}

}
