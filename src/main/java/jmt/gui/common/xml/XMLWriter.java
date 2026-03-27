/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package jmt.gui.common.xml;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.mapper.SimDescriptorMapper;
import jmt.gui.common.xml.serializer.XmlSerializer;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;

/**
 * <p>Title: XML Writer</p>
 * <p>Description: Writes model information to an XML file. This class provides
 * methods for model save. Completely refactored to use the DTO/Mapper architecture.</p>
 *
 * @author Bertoli Marco (Original)
 * @author Borgna Daniele (Refactoring)
 * @since 25-mar-2026
 */
public class XMLWriter implements XMLConstantNames {

    private XMLWriter() {
        // Utility class
    }

    /**
     * Writes the given CommonModel to an XML file.
     */
    public static void writeXML(String fileName, CommonModel model) {
        writeToResult(new StreamResult(new File(fileName)), model, fileName);
    }

    /**
     * Writes the given CommonModel to an XML file.
     */
    public static void writeXML(File xmlFile, CommonModel model) {
        writeToResult(new StreamResult(xmlFile), model, xmlFile.getName());
    }

    /**
     * Writes the given CommonModel to an OutputStream.
     */
    public static void writeXML(OutputStream out, CommonModel model) {
        writeToResult(new StreamResult(out), model, "model");
    }

    /**
     * Dumps an already constructed DOM Document to an XML file.
     */
    public static void writeXML(String fileName, Document doc) {
        if (doc == null) {
            return;
        }
        try {
            Transformer transformer = createSecureTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(new File(fileName)));
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts a CommonModel to a DOM Document using the new DTO mapping architecture.
     *
     * @param model     The model to serialize
     * @param modelName The name to assign to the simulation archive
     * @return The DOM Document representing the simulation
     */
    public static Document getDocument(CommonModel model, String modelName) {
        // 1. Map CommonModel -> SimDescriptor (DTO)
        SimDescriptorMapper mapper = new SimDescriptorMapper(model);
        SimDescriptor dto = mapper.mapToDto();

        // 2. Set root attributes that are passed directly
        dto.setArchiveName(modelName);

        // 3. Serialize SimDescriptor -> DOM Document
        XmlSerializer serializer = new XmlSerializer();
        return serializer.serialize(dto);
    }

    /**
     * Helper method to execute the serialization and pipe it to a Result stream.
     */
    private static void writeToResult(Result result, CommonModel model, String modelName) {
        Document modelDoc = getDocument(model, modelName);
        if (modelDoc == null) {
            return;
        }
        try {
            Transformer transformer = createSecureTransformer();
            transformer.transform(new DOMSource(modelDoc), result);
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a secure XML Transformer configured to prevent XXE (XML External Entity) attacks
     * and sets default output properties (Encoding and Indentation).
     */
    private static Transformer createSecureTransformer() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();

        // Disable External Entities to prevent XXE vulnerabilities
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        return transformer;
    }
}