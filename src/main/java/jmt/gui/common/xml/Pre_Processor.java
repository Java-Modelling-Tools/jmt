package jmt.gui.common.xml;




import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.xml.XMLWriter;
import jmt.gui.common.xml.XMLArchiver;
import jmt.gui.common.xml.XMLReader;

public class Pre_Processor {

	private static int[] mapper;
	private static int[] reducer;
	private static int[][] forkdegree;
	private static int[][] threshold;
	private static Document doc;
	private static ArrayList<String> input = new ArrayList<String>();
	private static ArrayList<String> output = new ArrayList<String>();
	
  public static void Reader(String argv[]) {

    try {

	File fXmlFile = new File("/homes/vsoaresl/Dev/JMT_Code/jmt/src/main/java/jmt/gui/common/xml/TemplateTest.xml");
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	doc = dBuilder.parse(fXmlFile);
			
	//optional, but recommended
	//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
	doc.getDocumentElement().normalize();
	
	NodeList node_mr = doc.getElementsByTagName("template_mapreduce");
	String name;
	Element mapreduce;
	mapper = new int[node_mr.getLength()];
	reducer = new int[node_mr.getLength()];
	forkdegree = new int[node_mr.getLength()][2];
	//input = new String[node_mr.getLength()];
	//output = new String[node_mr.getLength()];
	
	for (int i = 0; i < node_mr.getLength(); i++) {
		mapreduce = (Element) node_mr.item(i); //Each MapReduce
		name = mapreduce.getAttribute("name");
		
		Element in = (Element) mapreduce.getElementsByTagName("input").item(0);
		if (in.hasAttribute("name")){
			input.add(i, in.getAttribute("name"));
		}		
		
		Element fork = (Element) mapreduce.getElementsByTagName("fork").item(0);	
		forkdegree[i][0] = Integer.parseInt(fork.getElementsByTagName("map").item(0).getTextContent());
		forkdegree[i][1] = Integer.parseInt(fork.getElementsByTagName("red").item(0).getTextContent());
		
		mapper[i] = Integer.parseInt(mapreduce.getElementsByTagName("mapper").item(0).getTextContent());
		reducer[i] = Integer.parseInt(mapreduce.getElementsByTagName("reducer").item(0).getTextContent());
		
		NodeList node_semaphore = (NodeList) mapreduce.getElementsByTagName("semaphore");		
		threshold = new int[node_mr.getLength()][node_semaphore.getLength()];		
		
		for (int j = 0; j < node_semaphore.getLength(); j++){
			Node nNode = node_semaphore.item(j);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element semaphore = (Element) nNode;
				threshold[i][j] = Integer.parseInt(semaphore.getElementsByTagName("class1").item(0).getTextContent());
				if (threshold[i][j] < 0){
					threshold[i][j] = 0;
				} if (threshold[i][j] > 100){
					threshold[i][j] = 100;
				}
			}
		}
		Element out = (Element) mapreduce.getElementsByTagName("output").item(0);
		if (out.hasAttribute("name")){	
			output.add(i, out.getAttribute("name"));
		}
		
	}
	/*
	NodeList nList = doc.getElementsByTagName("mapper");
	for (int temp = 0; temp < nList.getLength(); temp++) {
		Node nNode = nList.item(temp);				
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) nNode;
			mapper = Integer.parseInt(eElement.getElementsByTagName("value").item(0).getTextContent());
		}
	}
	
	nList = doc.getElementsByTagName("reducer");
	for (int temp = 0; temp < nList.getLength(); temp++) {
		Node nNode = nList.item(temp);				
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) nNode;
			reducer = Integer.parseInt(eElement.getElementsByTagName("value").item(0).getTextContent());
		}
	}
	
	nList = doc.getElementsByTagName("fork");
	
	for (int temp = 0; temp < nList.getLength(); temp++) {
		Node nNode = nList.item(temp);						
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) nNode;
			forkdegree[0] = Integer.parseInt(eElement.getElementsByTagName("map").item(0).getTextContent());
			forkdegree[1] = Integer.parseInt(eElement.getElementsByTagName("red").item(0).getTextContent());
		}
	}
	
	nList = doc.getElementsByTagName("semaphore");
	threshold = new int[nList.getLength()];
	for (int temp = 0; temp < nList.getLength(); temp++) {
		Node nNode = nList.item(temp);						
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) nNode;
			threshold[temp] = Integer.parseInt(eElement.getElementsByTagName("class1").item(0).getTextContent());
			if (threshold[temp] < 0){
				threshold[temp] = 0;
			} if (threshold[temp] > 100){
				threshold[temp] = 100;
			}
		}
	}*/
    } catch (Exception e) {
	e.printStackTrace();
    }
  }
  
  public static void Write(String argv[]) {
	  
	  CommonModel model = new CommonModel();
	  Object distrib_class;
	  Object source;
	  Object[][] fork = new Object[mapper.length][2];
	  Object[][] queue = new Object[mapper.length][10];
	  Object[] semaphore = new Object[mapper.length];
	  Object[][] join = new Object[mapper.length][2];
	  Object sink;
	  Object in;
	  Object out;
	  
	  Exponential ex = new Exponential();	  
	  
	  XMLReader.parseXML(doc, model);
	  
	  distrib_class = model.addClass("Class 1", CommonConstants.CLASS_TYPE_OPEN, 0, null, ex);
	  
	  for (int j = 0; j < mapper.length; j++){
		  
		  in = model.getStationByName(input.get(j));
		  out = model.getStationByName(output.get(j));
		  		  
		  source = model.addStation("Source 1_"+j, CommonConstants.STATION_TYPE_SOURCE);
		  fork[j][0] = model.addStation("Fork 1_"+j, CommonConstants.STATION_TYPE_FORK);
		  fork[j][1] = model.addStation("Fork 2_"+j, CommonConstants.STATION_TYPE_FORK);
		  
		  for (int i = 0; i < mapper[j]+reducer[j]; i++){		  
			  queue[j][i] = model.addStation("Queue" + (i+1)+ "_"+j, CommonConstants.STATION_TYPE_SERVER );	  		  
		  }
		  
		  semaphore[j] = model.addStation("Task Semaphore 1_"+j , CommonConstants.STATION_TYPE_TASKSEMAPHORE);
		  join[j][0] = model.addStation("Join 1_"+j, CommonConstants.STATION_TYPE_JOIN);
		  join[j][1] = model.addStation("Join 2_"+j, CommonConstants.STATION_TYPE_JOIN);
		  sink = model.addStation("Sink 1_"+j, CommonConstants.STATION_TYPE_SINK);
		  
		  model.setClassRefStation(distrib_class, source);	  
		  
		  model.setConnected(in, fork[j][0], true);
		  for (int i = 0; i < mapper[j]+reducer[j]; i++){
			  if (i < mapper[j]){
				  model.setConnected(fork[j][0], queue[j][i], true);
				  model.setConnected(queue[j][i], semaphore[j], true);
			  } else{
				  model.setConnected(fork[j][1], queue[j][i], true);
				  model.setConnected(queue[j][i], join[j][1], true);
			  }		  		  
		  }
		  model.setConnected(semaphore[j], join[j][0], true);
		  model.setConnected(join[j][1], out, true);
		  //model.setConnected(source, fork[j][0], true);
		  model.setConnected(join[j][0], fork[j][1], true);
		 
		  
		  model.setStationNumberOfServers(forkdegree[j][0], fork[j][0]);
		  model.setStationNumberOfServers(forkdegree[j][1], fork[j][1]);
		  
		  model.setSemaphoreThreshold(distrib_class, threshold[j][0], semaphore[j]);
	  }
	  XMLWriter.writeXML("/homes/vsoaresl/Code_Study/XML_Test_Files/initial_test.jsimg", model);
	  //XMLArchiver.saveModel("/homes/vsoaresl/Code_Study/XML_Test_Files/initial_test.xml", model);
	  
	  /*
	  try {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("arquive");
		doc.appendChild(rootElement);
		Attr name = doc.createAttribute("name");
		Attr xsd = doc.createAttribute("xsi:noNamespaceSchemaLocation");
		name.setValue("Teste");
		xsd.setValue("Archive.xsd");
		rootElement.setAttributeNode(name);
		rootElement.setAttributeNode(xsd);
		

		// staff elements
		Element staff = doc.createElement("Staff");
		rootElement.appendChild(staff);

		// set attribute to staff element
		Attr attr = doc.createAttribute("id");
		attr.setValue("1");
		staff.setAttributeNode(attr);

		// shorten way
		// staff.setAttribute("id", "1");

		// firstname elements
		Element firstname = doc.createElement("firstname");
		firstname.appendChild(doc.createTextNode("yong"));
		staff.appendChild(firstname);

		// lastname elements
		Element lastname = doc.createElement("lastname");
		lastname.appendChild(doc.createTextNode("mook kim"));
		staff.appendChild(lastname);

		// nickname elements
		Element nickname = doc.createElement("nickname");
		nickname.appendChild(doc.createTextNode("mkyong"));
		staff.appendChild(nickname);

		// salary elements
		Element salary = doc.createElement("salary");
		salary.appendChild(doc.createTextNode("100000"));
		staff.appendChild(salary);

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File("/homes/vsoaresl/Code_Study/XML_Test_Files/initial_test.xml"));

		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);

		transformer.transform(source, result);

		System.out.println("File saved!");

	  } catch (ParserConfigurationException pce) {
		pce.printStackTrace();
	  } catch (TransformerException tfe) {
		tfe.printStackTrace();
	  }*/
	}
  
  
  public static void main(String argv[]){
	  	Reader(argv);
		Write(argv);		
  }

}

