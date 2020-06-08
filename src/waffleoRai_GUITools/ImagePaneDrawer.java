package waffleoRai_GUITools;

import java.awt.Graphics;

public interface ImagePaneDrawer {

	public int getX();
	public int getY();
	public int getWidth();
	public int getHeight();
	public void drawMe(Graphics g, int x, int y);
	
}
