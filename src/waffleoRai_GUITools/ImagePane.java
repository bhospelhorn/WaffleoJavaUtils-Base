package waffleoRai_GUITools;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Timer;

public class ImagePane extends JPanel{

	private static final long serialVersionUID = 8334582416340555248L;
	
	private BufferedImage[] images;
	private int frame_idx; //Current frame
	private int refresh_ms;
	
	private Timer timer;
	
	public ImagePane(BufferedImage img)
	{
		images = new BufferedImage[1];
		images[0] = img;
		frame_idx = 0;
		refresh_ms = 33;
		
		int width = img.getWidth();
		int height = img.getHeight();
		Dimension isize = new Dimension(width, height);
		this.setMinimumSize(isize);
		this.setPreferredSize(isize);
		
		timer = new Timer(refresh_ms, new TimerListener());
		
		/*BufferedImage b0 = images[0];
		for(int i = 0; i < b0.getHeight(); i++)
		{
			for(int j = 0; j < b0.getWidth(); j++)
			{
				int argb = b0.getRGB(j, i);
				System.err.print(String.format("%08x ", argb));
			}
			System.err.println();
		}*/
	}
	
	public ImagePane(BufferedImage[] img)
	{
		images = img;
		frame_idx = 0;
		refresh_ms = 33;
		
		int width = img[0].getWidth();
		int height = img[0].getHeight();
		Dimension isize = new Dimension(width, height);
		this.setMinimumSize(isize);
		this.setPreferredSize(isize);
		
		timer = new Timer(refresh_ms, new TimerListener());
		
		/*BufferedImage b0 = images[0];
		for(int i = 0; i < b0.getHeight(); i++)
		{
			for(int j = 0; j < b0.getWidth(); j++)
			{
				int argb = b0.getRGB(j, i);
				System.err.print(String.format("%08x ", argb));
			}
			System.err.println();
		}*/
	}
	
	public void setRefreshRate(int millis)
	{
		stopAnimation();
		refresh_ms = millis;
		timer = new Timer(refresh_ms, new TimerListener());
	}
	
	public boolean isAnimated()
	{
		return this.images.length > 1;
	}
	
	public void startAnimation()
	{
		if(timer.isRunning()) return;
		timer.start();
	}
	
	public void stopAnimation()
	{
		if(timer.isRunning()) timer.stop();
	}
	
	private class TimerListener implements ActionListener
	{

		public void actionPerformed(ActionEvent e) 
		{
			frame_idx++;
			if(frame_idx >= images.length) frame_idx = 0;
			repaint();
		}
		
	}
	
	public void paint(Graphics g) 
	{
        Graphics2D g2d = (Graphics2D)g;
        g2d.drawImage(images[frame_idx], 0, 0, null);
    }

}
