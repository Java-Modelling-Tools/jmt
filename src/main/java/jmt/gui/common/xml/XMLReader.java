package jmt.gui.common.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import jmt.common.xml.XSDSchemaLoader;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.mapper.SimDescriptorMapper;
import jmt.gui.common.xml.parsers.SimParser;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLReader implements XMLConstantNames, CommonConstants {

    public static boolean loadModel(String fileName, CommonModel model) {
        Document doc = loadXML(fileName, XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_MODEL_DEFINITION));
        if (doc == null) return false;
        if (doc.getElementsByTagName(XML_DOCUMENT_ROOT).getLength() != 0) {
            parseXML(doc.getDocumentElement(), model);
            return true;
        } else if (doc.getElementsByTagName(GuiXMLConstants.XML_ARCHIVE_DOCUMENT_ROOT).getLength() != 0) {
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

        if ("node".equals(tagName)) {
            descriptor = new SimDescriptor();
            jmt.gui.common.xml.parsers.NodeParser nodeParser = new jmt.gui.common.xml.parsers.NodeParser();
            descriptor.addNode(nodeParser.parse(root));
        } else {
            SimParser parser = new SimParser();
            descriptor = parser.parse(root);
        }

        // In-Place mapping on model
        SimDescriptorMapper mapper = new SimDescriptorMapper(descriptor, model);
        mapper.map();
    }

    public static Document loadXML(String filename, String schemaSource) {
        DOMParser parser = new DOMParser();
        try {
            if (schemaSource != null) {
                parser.setFeature(NAMESPACES_FEATURE_ID, true);
                parser.setFeature(VALIDATION_FEATURE_ID, true);
                parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
                parser.setFeature(VALIDATION_DYNAMIC_FEATURE_ID, true);
                parser.setProperty(EXTERNAL_SCHEMA_LOCATION_PROPERTY_ID, schemaSource);
            }
            FileReader fr = new FileReader(filename);
            InputSource inSource = new InputSource(fr);
            parser.parse(inSource);
            return parser.getDocument();
        } catch (SAXException | IOException e) {
            return null;
        }
    }

    public static Document loadXML(String filename) {
        return loadXML(filename, null);
    }
}