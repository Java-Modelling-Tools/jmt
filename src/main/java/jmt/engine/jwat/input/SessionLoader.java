package jmt.engine.jwat.input;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jmt.engine.jwat.JwatSession;
import jmt.engine.jwat.MatrixObservations;
import jmt.engine.jwat.Observation;
import jmt.engine.jwat.TimeConsumingWorker;
import jmt.engine.jwat.workloadAnalysis.WorkloadAnalysisSession;
import jmt.engine.jwat.workloadAnalysis.utils.ModelWorkloadAnalysis;
import jmt.engine.jwat.workloadAnalysis.utils.WorkloadResultLoader;
import jmt.gui.jwat.JWATConstants;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SessionLoader extends TimeConsumingWorker {

	private InputStream zis;
	private ZipFile zf;
	private String file;
	private String msg;
	private ResultLoader res;

	public SessionLoader(String fileName, ProgressShow prg) throws IOException {
		super(prg);
		zf = new ZipFile(fileName);
		file = fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.lastIndexOf("."));
		zis = zf.getInputStream(new ZipEntry(file + JwatSession.XMLext));
	}

	@Override
	public void finished() {
		if (this.get() != null) {
			fireEventStatus(new EventSessionLoaded((JwatSession) this.get()));
		} else {
			fireEventStatus(new EventFinishAbort(msg));
		}
	}

	private NodeList loadDataModel(JwatSession session) throws Exception {
		Observation[] valori;
		String[] selName;
		int[] selType;
		double[] valLst;
		int numObs, numVar, i, j, id;
		MatrixObservations m = null;
		VariableMapping[] map;
		DOMParser domP;
		NodeList tmpNodeLst, resNodeList = null;
		Node tmpNode;
		String tmpVal;
		String curType;
		DataInputStream dis;

		//Extracting zipped files in temp dir
		updateInfos(1, "Reading XML file", false);

		//load xml file
		domP = new DOMParser();
		domP.parse(new InputSource(zis));
		Document doc = domP.getDocument();
		//parse variables
		tmpNodeLst = doc.getElementsByTagName(JwatSession.ROOT);
		curType = tmpNodeLst.item(0).getAttributes().getNamedItem("type").getNodeValue();
		if (JwatSession.WORKLOAD_SAVE.equals(curType)) {
			res = new WorkloadResultLoader();
		} else {
			if (JwatSession.TRAFFIC_SAVE.equals(curType)) {
				res = new WorkloadResultLoader();
			}
		}
		tmpNodeLst = doc.getElementsByTagName("Variables");
		tmpNode = tmpNodeLst.item(0);
		tmpVal = tmpNode.getAttributes().getNamedItem("num").getNodeValue();
		numVar = Integer.parseInt(tmpVal);
		selName = new String[numVar];
		selType = new int[numVar];

		tmpNodeLst = tmpNode.getChildNodes();
		j = 0;
		updateInfos(2, "Reading Variables", false);
		for (i = 0; i < tmpNodeLst.getLength(); i++) {
			tmpNode = tmpNodeLst.item(i);
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
				tmpVal = tmpNode.getAttributes().getNamedItem("name").getNodeValue();
				selName[j] = tmpVal;
				tmpVal = tmpNode.getAttributes().getNamedItem("type").getNodeValue();
				selType[j] = Integer.parseInt(tmpVal);
				j++;
			}
		}

		//parse data
		updateInfos(3, "Reading Data", false);
		tmpNodeLst = doc.getElementsByTagName("Data");
		tmpNode = tmpNodeLst.item(0);
		tmpVal = tmpNode.getAttributes().getNamedItem("size").getNodeValue();
		numObs = Integer.parseInt(tmpVal);
		file = tmpNode.getAttributes().getNamedItem("filename").getNodeValue();

		resNodeList = doc.getElementsByTagName("Results");

		valori = new Observation[numObs];
		map = new VariableMapping[numVar];
		valLst = new double[numVar];

		updateInfos(3, "Reading Mapping", false);
		for (j = 0; j < numVar; j++) {
			if (selType[j] == JWATConstants.DATE) {
				map[j] = new DataMapping();
			}
			if (selType[j] == JWATConstants.NUMERIC) {
				map[j] = null;
			}
			if (selType[j] == JWATConstants.STRING) {
				map[j] = loadVarMapping(zf, selName[j]);
			}
		}

		//load data file
		zis = zf.getInputStream(new ZipEntry(file));
		dis = new DataInputStream(zis);

		for (i = 0; i < numObs; i++) {
			id = dis.readInt();
			for (j = 0; j < numVar; j++) {
				valLst[j] = dis.readDouble();
			}
			valori[i] = new Observation(valLst, id);
		}
		updateInfos(5, "Create matrix", false);
		m = new MatrixObservations(valori, selName, selType, map);
		session.getDataModel().setMatrix(m);

		return resNodeList;
	}

	protected int loadDataResult(NodeList resultNodeList, JwatSession session) throws IOException {
		updateInfos(6, "Load Results", false);
		return res.loadResult(zf, resultNodeList, session);
	}

	private StringMapping loadVarMapping(ZipFile zf, String varName) throws IOException {
		InputStream zis = zf.getInputStream(new ZipEntry(varName + "_Map" + JwatSession.BINext));
		DataInputStream dis = new DataInputStream(zis);

		StringMapping map = new StringMapping();
		double val;
		String str;
		int numEl = dis.read();

		for (int i = 0; i < numEl; i++) {
			val = dis.readDouble();
			str = dis.readUTF();
			map.addNewMapping(val, str);
		}

		return map;
	}

	@Override
	public Object construct() {
		JwatSession session = null;
		String fullName = zf.getName();
		String path = fullName.substring(0, fullName.lastIndexOf(File.separator));
		String fname = fullName.substring(fullName.lastIndexOf(File.separator) + 1);

		try {
			initShow(7);
			session = new WorkloadAnalysisSession(new ModelWorkloadAnalysis(), path, fname);
			NodeList resultNodeList = loadDataModel(session);

			if (resultNodeList != null) {
				loadDataResult(resultNodeList, session);
			} else {
				System.out.println("no results");
			}
			updateInfos(7, "Done", true);
		} catch (Exception e) {
			closeView();
			msg = "Failed to load session";
			session = null;
			e.printStackTrace();
		} catch (OutOfMemoryError err) {
			msg = "Out of memory. Try with more memory";
			session = null;
			err.printStackTrace();
		}
		return session;
	}
}
