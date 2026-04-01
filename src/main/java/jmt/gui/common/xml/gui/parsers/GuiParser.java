/**
 * Parser for the GUI XML document ({@code JModelGUI.xsd}).
 * Converts DOM elements into a {@link GuiDescriptor} DTO, with no knowledge of the
 * {@link jmt.gui.common.definitions.CommonModel} or any Swing/AWT type.
 * The mapping from the raw DTO to the live model is handled separately by
 * {@link jmt.gui.common.xml.mapper.GuiDescriptorMapper}.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.gui.parsers;

import jmt.gui.common.xml.GuiXMLConstants;
import jmt.gui.common.xml.XmlEntityParser;
import jmt.gui.common.xml.gui.dto.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GuiParser implements XmlEntityParser<GuiDescriptor>, GuiXMLConstants {

    @Override
    public GuiDescriptor parse(Element root) {
        GuiDescriptor descriptor = new GuiDescriptor();
        parseClassColors(root, descriptor);
        parseStationPositions(root, descriptor);
        parseConnectionShapes(root, descriptor);
        parseParametricAnalysis(root, descriptor);
        return descriptor;
    }

    // -----------------------------------------------------------------------
    // Class colors
    // -----------------------------------------------------------------------

    private void parseClassColors(Element root, GuiDescriptor descriptor) {
        NodeList classes = root.getElementsByTagName(XML_E_CLASS);
        for (int i = 0; i < classes.getLength(); i++) {
            Element el = (Element) classes.item(i);
            ClassColorDescriptor desc = new ClassColorDescriptor();
            desc.setName(el.getAttribute(XML_A_CLASS_NAME));
            desc.setColor(el.getAttribute(XML_A_CLASS_COLOR));
            descriptor.addClassColor(desc);
        }
    }

    // -----------------------------------------------------------------------
    // Station positions
    // -----------------------------------------------------------------------

    private void parseStationPositions(Element root, GuiDescriptor descriptor) {
        NodeList stations = root.getElementsByTagName(XML_E_STATION);
        for (int i = 0; i < stations.getLength(); i++) {
            Element stationEl = (Element) stations.item(i);
            NodeList positionNodes = stationEl.getElementsByTagName(XML_E_POSITION);
            if (positionNodes.getLength() == 0) continue;

            Element posEl = (Element) positionNodes.item(0);
            StationPositionDescriptor desc = new StationPositionDescriptor();
            desc.setName(stationEl.getAttribute(XML_A_STATION_NAME));
            desc.setX(Double.parseDouble(posEl.getAttribute(XML_A_POSITION_X)));
            desc.setY(Double.parseDouble(posEl.getAttribute(XML_A_POSITION_Y)));

            if (posEl.hasAttribute(XML_A_POSITION_ROTATE)) {
                desc.setRotate(Boolean.parseBoolean(posEl.getAttribute(XML_A_POSITION_ROTATE)));
            }
            if (posEl.hasAttribute(XML_A_POSITION_ANGLE)) {
                desc.setAngle(Double.parseDouble(posEl.getAttribute(XML_A_POSITION_ANGLE)));
            }
            descriptor.addStationPosition(desc);
        }
    }

    // -----------------------------------------------------------------------
    // Connection shapes
    // -----------------------------------------------------------------------

    private void parseConnectionShapes(Element root, GuiDescriptor descriptor) {
        NodeList shapes = root.getElementsByTagName(XML_E_CONNECTION_SHAPE);
        for (int i = 0; i < shapes.getLength(); i++) {
            Element shapeEl = (Element) shapes.item(i);
            ConnectionShapeDescriptor shape = new ConnectionShapeDescriptor();
            shape.setSource(shapeEl.getAttribute(XML_A_CONNECTION_SOURCE));
            shape.setTarget(shapeEl.getAttribute(XML_A_CONNECTION_TARGET));

            NodeList arcNodes = shapeEl.getElementsByTagName(XML_E_ARC);
            for (int j = 0; j < arcNodes.getLength(); j++) {
                Element arcEl = (Element) arcNodes.item(j);
                shape.addArc(parseArc(arcEl));
            }
            descriptor.addConnectionShape(shape);
        }
    }

    private ArcDescriptor parseArc(Element arcEl) {
        ArcDescriptor arc = new ArcDescriptor();

        // Source endpoint
        NodeList sourceNodes = arcEl.getElementsByTagName(XML_E_SOURCE);
        if (sourceNodes.getLength() > 0) {
            Element src = (Element) sourceNodes.item(0);
            arc.setSourceX(Double.parseDouble(src.getAttribute(XML_A_SOURCE_X)));
            arc.setSourceY(Double.parseDouble(src.getAttribute(XML_A_SOURCE_Y)));
        }

        // Two Bezier control points
        NodeList points = arcEl.getElementsByTagName(XML_E_POINT);
        if (points.getLength() > 0) {
            Element p0 = (Element) points.item(0);
            arc.setControlPoint1X(Double.parseDouble(p0.getAttribute(XML_A_POINT_X)));
            arc.setControlPoint1Y(Double.parseDouble(p0.getAttribute(XML_A_POINT_Y)));
        }
        if (points.getLength() > 1) {
            Element p1 = (Element) points.item(1);
            arc.setControlPoint2X(Double.parseDouble(p1.getAttribute(XML_A_POINT_X)));
            arc.setControlPoint2Y(Double.parseDouble(p1.getAttribute(XML_A_POINT_Y)));
        }

        // Target endpoint
        NodeList targetNodes = arcEl.getElementsByTagName(XML_E_TARGET);
        if (targetNodes.getLength() > 0) {
            Element tgt = (Element) targetNodes.item(0);
            arc.setTargetX(Double.parseDouble(tgt.getAttribute(XML_A_TARGET_X)));
            arc.setTargetY(Double.parseDouble(tgt.getAttribute(XML_A_TARGET_Y)));
        }

        return arc;
    }

    // -----------------------------------------------------------------------
    // Parametric analysis
    // -----------------------------------------------------------------------

    private void parseParametricAnalysis(Element root, GuiDescriptor descriptor) {
        NodeList temp = root.getElementsByTagName(XML_E_PARAMETRIC);
        if (temp.getLength() == 0) return;

        Element el = (Element) temp.item(0);
        ParametricAnalysisDescriptor pad = new ParametricAnalysisDescriptor();
        pad.setEnabled(Boolean.parseBoolean(el.getAttribute(XML_A_PARAMETRIC_ENABLED)));
        pad.setClassPath(el.getAttribute(XML_A_PARAMETRIC_CLASSPATH));

        NodeList fields = el.getElementsByTagName(XML_E_FIELD);
        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            pad.addProperty(field.getAttribute(XML_A_FIELD_NAME), field.getAttribute(XML_A_FIELD_VALUE));
        }

        descriptor.setParametricAnalysis(pad);
    }
}