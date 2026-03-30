/**
 * Serializes a {@link GuiDescriptor} DTO into a DOM {@link Document} conforming to
 * {@code JModelGUI.xsd}. This class is the symmetric counterpart of
 * {@link jmt.gui.common.xml.parsers.GuiParser} and has no knowledge of any
 * Swing/AWT type or live model class.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.gui.serializer;

import jmt.gui.common.xml.GuiXMLConstants;
import jmt.gui.common.xml.gui.dto.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class GuiSerializer implements GuiXMLConstants {

    /**
     * Serializes the given {@link GuiDescriptor} into a fresh DOM {@link Document}.
     *
     * @param dto the fully-populated GUI descriptor
     * @return a DOM document ready to be piped through a {@link javax.xml.transform.Transformer}
     */
    public Document serialize(GuiDescriptor dto) {
        Document doc = createDocument();
        Element root = doc.createElement(XML_DOCUMENT_ROOT);
        doc.appendChild(root);
        root.setAttribute("xsi:noNamespaceSchemaLocation", XML_DOCUMENT_XSD);
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

        writeClassColors(doc, root, dto);
        writeStationPositions(doc, root, dto);
        writeConnectionShapes(doc, root, dto);
        writeParametricAnalysis(doc, root, dto);

        return doc;
    }

    // -----------------------------------------------------------------------
    // Class colors
    // -----------------------------------------------------------------------

    private void writeClassColors(Document doc, Element root, GuiDescriptor dto) {
        for (ClassColorDescriptor desc : dto.getClassColors()) {
            Element el = doc.createElement(XML_E_CLASS);
            el.setAttribute(XML_A_CLASS_NAME,  desc.getName());
            el.setAttribute(XML_A_CLASS_COLOR, desc.getColor());
            root.appendChild(el);
        }
    }

    // -----------------------------------------------------------------------
    // Station positions
    // -----------------------------------------------------------------------

    private void writeStationPositions(Document doc, Element root, GuiDescriptor dto) {
        for (StationPositionDescriptor desc : dto.getStationPositions()) {
            Element station = doc.createElement(XML_E_STATION);
            station.setAttribute(XML_A_STATION_NAME, desc.getName());

            Element position = doc.createElement(XML_E_POSITION);
            position.setAttribute(XML_A_POSITION_X,      Double.toString(desc.getX()));
            position.setAttribute(XML_A_POSITION_Y,      Double.toString(desc.getY()));
            position.setAttribute(XML_A_POSITION_ROTATE, Boolean.toString(desc.isRotate()));
            position.setAttribute(XML_A_POSITION_ANGLE,  Double.toString(desc.getAngle()));

            station.appendChild(position);
            root.appendChild(station);
        }
    }

    // -----------------------------------------------------------------------
    // Connection shapes
    // -----------------------------------------------------------------------

    private void writeConnectionShapes(Document doc, Element root, GuiDescriptor dto) {
        for (ConnectionShapeDescriptor shape : dto.getConnectionShapes()) {
            Element shapeEl = doc.createElement(XML_E_CONNECTION_SHAPE);
            shapeEl.setAttribute(XML_A_CONNECTION_SOURCE, shape.getSource());
            shapeEl.setAttribute(XML_A_CONNECTION_TARGET, shape.getTarget());

            for (ArcDescriptor arc : shape.getArcs()) {
                shapeEl.appendChild(serializeArc(doc, arc));
            }
            root.appendChild(shapeEl);
        }
    }

    private Element serializeArc(Document doc, ArcDescriptor arc) {
        Element arcEl = doc.createElement(XML_E_ARC);

        Element source = doc.createElement(XML_E_SOURCE);
        source.setAttribute(XML_A_SOURCE_X, Double.toString(arc.getSourceX()));
        source.setAttribute(XML_A_SOURCE_Y, Double.toString(arc.getSourceY()));

        Element cp1 = doc.createElement(XML_E_POINT);
        cp1.setAttribute(XML_A_POINT_X, Double.toString(arc.getControlPoint1X()));
        cp1.setAttribute(XML_A_POINT_Y, Double.toString(arc.getControlPoint1Y()));

        Element cp2 = doc.createElement(XML_E_POINT);
        cp2.setAttribute(XML_A_POINT_X, Double.toString(arc.getControlPoint2X()));
        cp2.setAttribute(XML_A_POINT_Y, Double.toString(arc.getControlPoint2Y()));

        Element target = doc.createElement(XML_E_TARGET);
        target.setAttribute(XML_A_TARGET_X, Double.toString(arc.getTargetX()));
        target.setAttribute(XML_A_TARGET_Y, Double.toString(arc.getTargetY()));

        arcEl.appendChild(source);
        arcEl.appendChild(cp1);
        arcEl.appendChild(cp2);
        arcEl.appendChild(target);
        return arcEl;
    }

    // -----------------------------------------------------------------------
    // Parametric analysis
    // -----------------------------------------------------------------------

    private void writeParametricAnalysis(Document doc, Element root, GuiDescriptor dto) {
        ParametricAnalysisDescriptor pad = dto.getParametricAnalysis();
        if (pad == null) return;

        Element el = doc.createElement(XML_E_PARAMETRIC);
        el.setAttribute(XML_A_PARAMETRIC_ENABLED, Boolean.toString(pad.isEnabled()));

        if (pad.isEnabled() && pad.getClassPath() != null) {
            el.setAttribute(XML_A_PARAMETRIC_CLASSPATH, pad.getClassPath());
            for (java.util.Map.Entry<String, String> entry : pad.getProperties().entrySet()) {
                Element field = doc.createElement(XML_E_FIELD);
                field.setAttribute(XML_A_FIELD_NAME,  entry.getKey());
                field.setAttribute(XML_A_FIELD_VALUE, entry.getValue());
                el.appendChild(field);
            }
        }
        root.appendChild(el);
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private Document createDocument() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to create XML Document for GUI", e);
        }
    }
}