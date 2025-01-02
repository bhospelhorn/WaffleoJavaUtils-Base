package waffleoRai_Files;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLReader {
	
	private static XMLReader static_reader;

	//https://mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
	private DocumentBuilderFactory factory;
	
	public XMLReader(){
		try{
			factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public Document readXML_DOM(String path) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilder db = factory.newDocumentBuilder();
		Document doc = db.parse(new File(path));
		doc.getDocumentElement().normalize();
		return doc;
	}
	
	public static Document readXMLStatic(String path) throws ParserConfigurationException, SAXException, IOException{
		if(static_reader == null) static_reader = new XMLReader();
		return static_reader.readXML_DOM(path);
	}
	
	public static void clearStatic(){
		static_reader = null;
	}
	
	public static Map<String, String> getAttributes(Node node){
		Map<String, String> map = new HashMap<String, String>();
		NamedNodeMap allattr = node.getAttributes();
		int attrcount = allattr.getLength();
		for(int i = 0; i < attrcount; i++){
			Node a = allattr.item(i);
			if(a.getNodeType() == Node.ATTRIBUTE_NODE){
				String k = a.getNodeName();
				String v = a.getNodeValue();
				map.put(k, v);
			}
		}
		return map;
	}
	
	public static Element getFirstChildElementWithTag(Element node, String tag) {
		if(node == null || tag == null) return null;
		NodeList nl = node.getElementsByTagName(tag);
		if(nl == null) return null;
		
		int childCount = nl.getLength();
		if(childCount == 0) return null;
		for(int i = 0; i < childCount; i++) {
			Node child = nl.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE) {
				return (Element)child;
			}
		}
		
		return null;
	}
	
	public static Element getFirstChildElementWithAttribute(Element node, String attrKey, String attrVal) {
		if(node == null || attrKey == null) return null;
		NodeList nl = node.getChildNodes();
		if(nl == null) return null;
		
		int childCount = nl.getLength();
		if(childCount == 0) return null;
		for(int i = 0; i < childCount; i++) {
			Node child = nl.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element)child;
				String aval = childElement.getAttribute(attrKey);
				if(aval != null && aval.equals(attrVal)) {
					return childElement;
				}
			}
		}
		
		return null;
	}
	
	public static Element getFirstChildElementWithTagAndAttribute(Element node, String tag, String attrKey, String attrVal) {
		if(node == null || tag == null || attrKey == null) return null;
		NodeList nl = node.getElementsByTagName(tag);
		if(nl == null) return null;
		
		int childCount = nl.getLength();
		if(childCount == 0) return null;
		for(int i = 0; i < childCount; i++) {
			Node child = nl.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element)child;
				String aval = childElement.getAttribute(attrKey);
				if(aval != null && aval.equals(attrVal)) {
					return childElement;
				}
			}
		}
		
		return null;
	}
	
	public static List<Element> getChildElementsWithTag(Element node, String tag) {
		List<Element> list = new LinkedList<Element>();
		if(node == null || tag == null) return list;
		NodeList nl = node.getElementsByTagName(tag);
		if(nl == null) return list;
		
		int childCount = nl.getLength();
		if(childCount == 0) return list;
		for(int i = 0; i < childCount; i++) {
			Node child = nl.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE) {
				list.add((Element)child);
			}
		}
		
		return list;
	}
	
	public static List<Element> getChildElementsWithAttribute(Element node, String attrKey, String attrVal) {
		List<Element> list = new LinkedList<Element>();
		if(node == null || attrKey == null) return list;
		NodeList nl = node.getChildNodes();
		if(nl == null) return list;
		
		int childCount = nl.getLength();
		if(childCount == 0) return list;
		for(int i = 0; i < childCount; i++) {
			Node child = nl.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element)child;
				String aval = childElement.getAttribute(attrKey);
				if(aval != null && aval.equals(attrVal)) {
					list.add(childElement);
				}
			}
		}
		
		return list;
	}
	
	public static List<Element> getChildElementsWithTagAndAttribute(Element node, String tag, String attrKey, String attrVal) {
		List<Element> list = new LinkedList<Element>();
		if(node == null || tag == null || attrKey == null) return list;
		NodeList nl = node.getElementsByTagName(tag);
		if(nl == null) return list;
		
		int childCount = nl.getLength();
		if(childCount == 0) return list;
		for(int i = 0; i < childCount; i++) {
			Node child = nl.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element)child;
				String aval = childElement.getAttribute(attrKey);
				if(aval != null && aval.equals(attrVal)) {
					list.add(childElement);
				}
			}
		}
		
		return list;
	}
	
	public static List<Element> getChildElements(Element node){
		List<Element> list = new LinkedList<Element>();
		if(node == null) return list;
		NodeList nl = node.getChildNodes();
		if(nl == null) return list;
		
		int childCount = nl.getLength();
		if(childCount == 0) return list;
		for(int i = 0; i < childCount; i++) {
			Node child = nl.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE) {
				list.add((Element)child);
			}
		}
		
		return list;
	}
	
}
