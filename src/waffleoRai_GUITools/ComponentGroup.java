package waffleoRai_GUITools;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

public class ComponentGroup {

	private Map<String, Component> compMap;
	private List<ComponentGroup> children;
	
	public ComponentGroup(){
		compMap = new HashMap<String, Component>();
		children = new LinkedList<ComponentGroup>();
	}
	
	public void addComponent(String name, Component comp){
		compMap.put(name, comp);
	}
	
	public void removeComponent(String name){
		if (compMap.containsKey(name)){
			compMap.remove(name);
		}
	}
	
	public ComponentGroup newChild(){
		ComponentGroup child = new ComponentGroup();
		children.add(child);
		return child;
	}
	
	public void clearChildren(){
		children.clear();
	}
	
	public Component getComponent(String name){
		return compMap.get(name);
	}
	
	public JTextField getTextBox(String name){
		Component c = getComponent(name);
		if (c == null) return null;
		if (!(c instanceof JTextField)) return null;
		JTextField t = (JTextField)c;
		return t;
	}
	
	public JLabel getLabel(String name){
		Component c = getComponent(name);
		if (c == null) return null;
		if (!(c instanceof JLabel)) return null;
		JLabel l = (JLabel)c;
		return l;
	}
	
	public JCheckBox getCheckbox(String name){
		Component c = getComponent(name);
		if (c == null) return null;
		if (!(c instanceof JCheckBox)) return null;
		JCheckBox cb = (JCheckBox)c;
		return cb;
	}
	
	public JToggleButton getToggle(String name){
		Component c = getComponent(name);
		if (c == null) return null;
		if (!(c instanceof JToggleButton)) return null;
		JToggleButton tb = (JToggleButton)c;
		return tb;
	}
	
	public JButton getButton(String name){
		Component c = getComponent(name);
		if (c == null) return null;
		if (!(c instanceof JButton)) return null;
		JButton b = (JButton)c;
		return b;
	}
	
	public boolean hasComponent(String name){
		return compMap.containsKey(name);
	}
	
	public void setEnabling(boolean enabled){
		Collection<Component> allComps = compMap.values();
		for (Component c : allComps) c.setEnabled(enabled);
		for(ComponentGroup g : children) g.setEnabling(enabled);
	}
	
	public void repaint(){
		Collection<Component> allComps = compMap.values();
		for (Component c : allComps) c.repaint();
		for(ComponentGroup g : children) g.repaint();
	}
	
}
