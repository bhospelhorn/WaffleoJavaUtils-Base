package waffleoRai_GUITools;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public abstract class WRDialog extends JDialog{

	private static final long serialVersionUID = 652484581675239763L;

	protected JFrame parent;
	protected ComponentGroup globalEnable;
	
	protected WRDialog(){
		constructorCore();
	}
	
	protected WRDialog(JFrame parentFrame){
		super(parentFrame);
		parent = parentFrame;
		constructorCore();
	}
	
	protected WRDialog(JFrame parentFrame, boolean modal){
		super(parentFrame, modal);
		parent = parentFrame;
		constructorCore();
	}
	
	private void constructorCore(){
		globalEnable = new ComponentGroup();
	}
	
	public void disableAll(){
		globalEnable.setEnabling(false);
		globalEnable.repaint();
	}
	
	public void reenable(){
		globalEnable.setEnabling(true);
		globalEnable.repaint();
	}
	
	public void repaint(){
		if(globalEnable != null) globalEnable.repaint();
		super.repaint();
	}
	
	public void setWait(){
		disableAll();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	public void unsetWait(){
		reenable();
		setCursor(null);
	}
	
	public void showWarning(String text){
		JOptionPane.showMessageDialog(this, text, "Warning", JOptionPane.WARNING_MESSAGE);
	}
	
	public void showError(String text){
		JOptionPane.showMessageDialog(this, text, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	public void showInfo(String text){
		JOptionPane.showMessageDialog(this, text, "Notice", JOptionPane.INFORMATION_MESSAGE);
	}
	
	protected void dummyCallback(){
		showInfo("Sorry, this component doesn't work yet!");
	}
	
	public void showMe(Component c){
		if(c != null) setLocationRelativeTo(c);
		else{
			if(parent != null) setLocationRelativeTo(parent);
			else{
				setLocation(GUITools.getScreenCenteringCoordinates(this));
			}
		}
		
		pack();
		setVisible(true);
	}
	
	public void closeMe(){
		this.setVisible(false);
		this.dispose();
	}
	
}
