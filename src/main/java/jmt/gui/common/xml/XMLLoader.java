/**
 * Utility class for loading and parsing XML documents using the Xerces DOM parser,
 * with optional XSD schema validation support.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.FileReader;
import java.io.IOException;


public class XMLLoader implements XMLConstantNames {
    private XMLLoader() {
        /* This utility class should not be instantiated */
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
