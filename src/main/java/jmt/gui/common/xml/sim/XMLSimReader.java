/**
 * Refactored replacement for the legacy {@code XMLReader}, reading simulation model data from XML
 * using a two-phase Parse then Map pipeline (DTO intermediate representation).
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim;

import jmt.common.xml.XSDSchemaLoader;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLArchiver;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.XMLLoader;
import jmt.gui.common.xml.sim.dto.SimDescriptor;
import jmt.gui.common.xml.sim.mapper.SimDescriptorMapper;
import jmt.gui.common.xml.sim.parsers.NodeParser;
import jmt.gui.common.xml.sim.parsers.SimParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

public class XMLSimReader implements XMLConstantNames {
    private XMLSimReader() {
        /* This utility class should not be instantiated */
    }

    public static boolean loadModel(String fileName, CommonModel model) {
        Document doc = XMLLoader.loadXML(fileName, XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_MODEL_DEFINITION));
        if (doc == null) return false;
        if (doc.getElementsByTagName(XML_DOCUMENT_ROOT).getLength() != 0) {
            parseXML(doc.getDocumentElement(), model);
            return true;
        } else if (doc.getElementsByTagName(XML_ARCHIVE_DOCUMENT_ROOT).getLength() != 0) {
            Element simRoot = XMLArchiver.getSimFromArchiveDocument(doc);
            if (simRoot != null) {
                parseXML(simRoot, model);
                return true;
            }
        }
        return false;
    }

    public static boolean loadModel(File xmlFile, CommonModel model) {
        return loadModel(xmlFile.getAbsolutePath(), model);
    }

    public static void parseXML(Document document, CommonModel model) {
        if (document == null) return;
        parseXML(document.getDocumentElement(), model);
    }

    public static void parseXML(Element root, CommonModel model) {
        if (root == null || model == null) return;
        SimDescriptor descriptor;
        String tagName = root.getTagName();

        if (XML_NODE.equals(tagName)) {
            descriptor = new SimDescriptor();
            NodeParser nodeParser = new NodeParser();
            descriptor.addNode(nodeParser.parse(root));
        } else {
            SimParser parser = new SimParser();
            descriptor = parser.parse(root);
        }

        if (root.getParentNode() instanceof Element) {
            Element parent = (Element) root.getParentNode();
            if (XML_ARCHIVE_DOCUMENT_ROOT.equals(parent.getTagName())) {
                descriptor.setArchiveName(parent.getAttribute(XML_A_ROOT_NAME));
                descriptor.setArchiveTimestamp(parent.getAttribute(XML_A_ARCHIVE_TIMESTAMP));
            }
        }

        // In-Place mapping on model
        SimDescriptorMapper mapper = new SimDescriptorMapper(descriptor, model);
        mapper.mapToModel();
    }

}