package waffleoRai_GUITools;

import java.awt.Cursor;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

public abstract class WRPanel extends JPanel{

	private static final long serialVersionUID = 320115534553680370L;
	
	protected ComponentGroup globalEnable;
	
	protected WRPanel(){
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

}
