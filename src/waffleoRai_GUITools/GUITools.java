package waffleoRai_GUITools;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

public class GUITools {
	
	public static Point getScreenCenteringCoordinates(Component c)
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double sWidth = screenSize.getWidth();
		double sHeight = screenSize.getHeight();
		
		int centerX = (int)(sWidth / 2.0);
		int centerY = (int)(sHeight / 2.0);
		
		int X = centerX - ((c.getWidth()) / 2);
		int Y = centerY - (c.getHeight() / 2);
		
		return new Point(X, Y);
	}

}
