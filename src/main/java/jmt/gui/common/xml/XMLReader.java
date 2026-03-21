/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.gui.common.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import jmt.common.xml.XSDSchemaLoader;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.parsers.SimParser;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XMLReader now acts as a thin facade:
 * XML load/validation -> SimParser (DTO tree) -> ModelPopulator (CommonModel build).
 */
public class XMLReader implements XMLConstantNames, CommonConstants {

    /**
     * Loads a model saved in an XML file, given the name of the file.
     * @param fileName name of the file to be opened
     * @param model data structure where model should be created
     * @return true iff model was recognized and loaded, false otherwise
     */
    public static boolean loadModel(String fileName, CommonModel model) {
        Document doc = loadXML(fileName, XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_MODEL_DEFINITION));
        if (doc == null) {
            return false;
        }

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

    /**
     * Loads a model saved in an XML file, given the handler to the file.
     * @param xmlFile handler to the file to be opened
     * @param model data structure where model should be created
     * @return true iff model was recognized and loaded, false otherwise
     */
    public static boolean loadModel(File xmlFile, CommonModel model) {
        return loadModel(xmlFile.getAbsolutePath(), model);
    }

    /**
     * Backward-compatible overload used by archive loading code.
     * @param document XML document to parse
     * @param model target data model
     */
    public static void parseXML(Document document, CommonModel model) {
        if (document == null) {
            return;
        }
        parseXML(document.getDocumentElement(), model);
    }

    /**
     * Parses the given XML root element through the new IR architecture.
     * @param root root of document to be parsed
     * @param model target data model
     */
    public static void parseXML(Element root, CommonModel model) {
        if (root == null || model == null) {
            return;
        }

        SimParser parser = new SimParser();
        SimDescriptor descriptor = parser.parse(root);
        ModelPopulator.populate(descriptor, model);
    }

    /**
     * Loads an XML file returning the Document representation and validating when a schema is provided.
     * @param filename file to load
     * @param schemaSource schema location, null to disable validation
     * @return Document representation, or null on error
     */
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
        } catch (SAXException e) {
            System.err.println("XMLLoader Error - An error occurs while attempting to parse the document \"" + e.getMessage() + "\".");
            return null;
        } catch (IOException e) {
            System.err.println("XMLLoader Error - An error occurs while attempting to parse the document.");
            return null;
        }
    }

    /**
     * Loads an XML file returning the Document representation without validation.
     * @param filename file to load
     * @return Document representation, or null on error
     */
    public static Document loadXML(String filename) {
        return loadXML(filename, null);
    }
}
