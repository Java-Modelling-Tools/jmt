/**
 * Legacy archive pipeline that uses the original {@link XMLReader}/{@link XMLWriter} pair
 * (and the corresponding legacy {@link GuiXMLReader}/{@link GuiXMLWriter} and
 * {@link XMLResultsReader}/{@link XMLResultsWriter}) to load and save JMT archive files.
 * Kept for backward traceability and for cross-verification tests.
 * Use {@link XMLArchiver} instead for all production code.
 *
 * @author Bertoli Marco
 *         Date: 22-lug-2005
 * @author Borgna Daniele (Refactoring)
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml;

import jmt.common.xml.XSDSchemaLoader;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.PAResultsModel;
import jmt.gui.common.definitions.StoredResultsModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Date;

/**
 * @deprecated Use {@link XMLArchiver} instead.
 */
@Deprecated
public class LegacyXMLArchiver implements GuiXMLConstants {

	/**
	 * Saves a JMT archive using the legacy {@link XMLWriter}/{@link GuiXMLWriter} pipeline.
	 * @param archiveName target file
	 * @param model data structure to save
	 */
	@SuppressWarnings("deprecation")
	public static void saveModel(File archiveName, CommonModel model) {
		Document modelDoc = getDocument(model, archiveName.getName());
		if (modelDoc == null) {
			return;
		}
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(modelDoc), new StreamResult(archiveName));
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads an archive into the specified {@link CommonModel} using the legacy
	 * {@link XMLReader}/{@link GuiXMLReader} pipeline.
	 * @param archiveName archive file to open
	 * @param model target model
	 */
	@SuppressWarnings("deprecation")
	public static void loadModel(File archiveName, CommonModel model) {
		Document doc = XMLReader.loadXML(archiveName.getAbsolutePath(), XSDSchemaLoader.loadSchema(XSDSchemaLoader.JMT_ARCHIVE));
		if (doc.getElementsByTagName(XML_ARCHIVE_DOCUMENT_ROOT).getLength() == 0) {
			XMLReader.parseXML(doc, model);
		} else {
			XMLReader.parseXML(getSimFromArchiveDocument(doc), model);
			if (containsGUI(doc)) {
				GuiXMLReader.parseXML(getGuiFromArchiveDocument(doc), model);
			}
			if (containsResults(doc)) {
				if (model.isParametricAnalysisEnabled()) {
					PAResultsModel results = new PAResultsModel(model, true);
					XMLResultsReader.parseXML(getResultsFromArchiveDocument(doc), results);
					model.setSimulationResults(results);
				} else {
					StoredResultsModel results = new StoredResultsModel();
					XMLResultsReader.parseXML(getResultsFromArchiveDocument(doc), results);
					model.setSimulationResults(results);
				}
			}
		}
	}

	/**
	 * Returns a new archive {@link Document} built with the legacy
	 * {@link XMLWriter}/{@link GuiXMLWriter} pipeline.
	 * @param model data structure
	 * @param documentName name attribute for the root element
	 * @return created Document, or {@code null} on error
	 */
	@SuppressWarnings("deprecation")
	public static Document getDocument(CommonModel model, String documentName) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		}
		Document archiveDoc = docBuilder.newDocument();

		Document sim = XMLWriter.getDocument(model, documentName);
		Document jmodel = GuiXMLWriter.getDocument(model);
		Document results = null;
		if (model.containsSimulationResults()) {
			if (model.isParametricAnalysisEnabled() && (model.getSimulationResults() instanceof PAResultsModel)) {
				results = XMLResultsWriter.getDocument(model.getSimulationResults());
			} else if (!model.isParametricAnalysisEnabled() && (!(model.getSimulationResults() instanceof PAResultsModel))) {
				results = XMLResultsWriter.getDocument(model.getSimulationResults());
			}
		}

		Element root = archiveDoc.createElement(XML_ARCHIVE_DOCUMENT_ROOT);
		archiveDoc.appendChild(root);

		root.appendChild(archiveDoc.importNode(sim.getElementsByTagName(XMLConstantNames.XML_DOCUMENT_ROOT).item(0), true));
		if (jmodel != null) {
			root.appendChild(archiveDoc.importNode(jmodel.getElementsByTagName(XML_DOCUMENT_ROOT).item(0), true));
		}
		if (results != null) {
			root.appendChild(archiveDoc.importNode(results.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_ROOT).item(0), true));
		}

		root.setAttribute("xsi:noNamespaceSchemaLocation", XML_ARCHIVE_DOCUMENT_XSD);
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		root.setAttribute(XML_ARCHIVE_A_NAME, documentName);
		root.setAttribute(XML_ARCHIVE_A_TIMESTAMP, new Date().toString());

		return archiveDoc;
	}

	// --- helpers (identical to XMLArchiver) ---

	/** @see XMLArchiver#getSimFromArchiveDocument(Document) */
	public static Element getSimFromArchiveDocument(Document archive) {
		return (Element) archive.getElementsByTagName(XMLConstantNames.XML_DOCUMENT_ROOT).item(0);
	}

	/** @see XMLArchiver#getGuiFromArchiveDocument(Document) */
	public static Element getGuiFromArchiveDocument(Document archive) {
		return (Element) archive.getElementsByTagName(GuiXMLConstants.XML_DOCUMENT_ROOT).item(0);
	}

	/** @see XMLArchiver#getResultsFromArchiveDocument(Document) */
	public static Element getResultsFromArchiveDocument(Document archive) {
		if (archive.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_ROOT).getLength() > 0) {
			return (Element) archive.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_ROOT).item(0);
		} else {
			return (Element) archive.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_O_ROOT).item(0);
		}
	}

	private static boolean containsGUI(Document archive) {
		return archive.getElementsByTagName(XML_DOCUMENT_ROOT).getLength() > 0;
	}

	private static boolean containsResults(Document archive) {
		return archive.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_ROOT).getLength() > 0
				|| archive.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_O_ROOT).getLength() > 0;
	}
}
