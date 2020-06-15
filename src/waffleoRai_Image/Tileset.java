package waffleoRai_Image;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

public class Tileset {
	
	/*----- Instance Variables -----*/
	
	private int bitDepth;
	private int tiledim;
	
	private int width; //In tiles
	private int height; //In tiles
	
	private Tile[] tiles;
	private Palette linked_plt;
	
	/*----- Construction -----*/
	
	protected Tileset(){
		bitDepth = 32;
		tiledim = 8;
		width = -1;
		height = -1;
	}
	
	public Tileset(int bits, int tile_dim, int tile_count){
		bitDepth = bits;
		tiledim = tile_dim;
		width = -1;
		height = -1;
		tiles = new Tile[tile_count];
		for(int i = 0; i < tile_count; i++){
			tiles[i] = new Tile(bitDepth, tiledim);
		}
	}
	
	/*----- Getters -----*/
	
	public int getBitDepth(){return bitDepth;}
	public boolean is4Bit(){return (bitDepth == 4);}
	public boolean is8Bit(){return (bitDepth == 8);}
	public int getWidthInTiles(){return width;}
	public int getHeightInTiles(){return height;}
	public int getTileDimension(){return tiledim;}
	public Palette getLinkedPalette(){return linked_plt;}
	
	public int getTileCount(){
		if(tiles == null) return 0;
		return tiles.length;
	}
	
	public Tile getTile(int idx){
		if(tiles == null) return null;
		if(idx < 0 || idx >= tiles.length) return null;
		return tiles[idx];
	}
	
	/*----- Setters -----*/
	
	protected void setBitDepth(int bd){bitDepth = bd;}
	public void setTileDim(int dim){tiledim = dim; tiles = null;}
	public void setDimensionInTiles(int w, int h){width = w; height = h;}
	protected void setTile(int idx, Tile t){tiles[idx] = t;}
	
	public void reallocateTileArray(int tile_count){
		tiles = new Tile[tile_count];
		for(int i = 0; i < tile_count; i++){
			tiles[i] = new Tile(bitDepth, tiledim);
		}
	}
	
	public void setLinkedPalette(Palette p){linked_plt = p;}
	
	/*----- Rendering -----*/
	
	public BufferedImage renderTile(int idx){
		if(linked_plt == null) return renderTileData(idx); 
		
		BufferedImage img = new BufferedImage(tiledim, tiledim, BufferedImage.TYPE_INT_ARGB);
		tiles[idx].copyTo(img, 0, 0, false, false, linked_plt);
		
		return img;
	}
	
	public BufferedImage renderTileData(int idx){

		BufferedImage img = new BufferedImage(tiledim, tiledim, BufferedImage.TYPE_INT_ARGB);
		tiles[idx].copyTo(img, 0, 0, false, false);
		
		return img;
	}
		
	public List<BufferedImage> renderTileData(){

		List<BufferedImage> list = new LinkedList<BufferedImage>();
		for(int i = 0; i < tiles.length; i++){
			list.add(renderTileData(i));
		}
		
		return list;
	}
	
	public BufferedImage renderImage(){
		return renderImage(width);
	}
	
	public BufferedImage renderImageData(){
		return renderImageData(width);
	}
	
	public BufferedImage renderImage(int tilewidth){
		if(linked_plt == null) return renderImageData(tilewidth); 
		
		int w = tilewidth * tiledim;
		int h = (tiles.length/tilewidth) * tiledim;

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);int l = 0;
		int x = 0; int y = 0;
		for(int t = 0; t < tiles.length; t++){
			tiles[t].copyTo(img, x, y, false, false, linked_plt);
			
			x += tiledim;
			
			if(++l >= tilewidth){
				l = 0; x = 0;
				y += tiledim;
			}
			
		}
		
		return img;
	}
	
	public BufferedImage renderImageData(int tilewidth){

		int w = tilewidth * tiledim;
		int h = (tiles.length/tilewidth) * tiledim;

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);int l = 0;
		int x = 0; int y = 0;
		for(int t = 0; t < tiles.length; t++){
			tiles[t].copyTo(img, x, y, false, false);
			
			x += tiledim;
			
			if(++l >= tilewidth){
				l = 0; x = 0;
				y += tiledim;
			}
			
		}
		
		return img;
	}
	
}
