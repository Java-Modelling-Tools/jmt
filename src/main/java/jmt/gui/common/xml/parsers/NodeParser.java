/**
 * This class is responsible for parsing an XML element representing a node and converting it into a {@link NodeDescriptor} object.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.parsers;

import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class NodeParser implements XmlEntityParser<NodeDescriptor> {

    @Override
    public NodeDescriptor parse(Element element) {
        NodeDescriptor descriptor = new NodeDescriptor();
        descriptor.setName(readAttribute(element, "name"));

        SectionParser sectionParser = new SectionParser();
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element child = (Element) node;
            String tagName = child.getTagName();

            if (XMLConstantNames.XML_E_STATION_SECTION.equals(tagName)) {
                descriptor.addSection(sectionParser.parse(child));
            } else if (XMLConstantNames.XML_E_STATION_DUE_DATES.equals(tagName)) {
                descriptor.setClassSoftDeadlines(parseSoftDeadlines(child));
            } else if (XMLConstantNames.XML_E_STATION_QUANTUM_SIZE.equals(tagName)) {
                descriptor.setQuantumSize(parseListOfDoubles(child, XMLConstantNames.XML_E_QUANTA_SIZE));
            } else if (XMLConstantNames.XML_E_STATION_QUANTUM_SWITCHOVER_TIME.equals(tagName)) {
                descriptor.setQuantumSwitchoverTime(parseListOfDoubles(child, XMLConstantNames.XML_E_STATION_QUANTUM_SWITCHOVER_TIME));
            } else {
                descriptor.addParameter(parseGenericParameter(child));
            }
        }

        return descriptor;
    }

    private List<Double> parseSoftDeadlines(Element element) {
        return parseListOfDoubles(element, XMLConstantNames.XML_A_CLASS_SOFT_DEADLINE);
    }

    private List<Double> parseListOfDoubles(Element element, String childTagName) {
        List<Double> list = new ArrayList<>();
        NodeList nodes = element.getChildNodes();
        for (int j = 0; j < nodes.getLength(); j++) {
            Node node = nodes.item(j);
            if (node.getNodeType() == Node.ELEMENT_NODE && childTagName.equals(((Element) node).getTagName())) {
                Double val = parseContentAsDouble((Element) node);
                if (val != null) {
                    list.add(val);
                }
            }
        }
        return list;
    }

    private Double parseContentAsDouble(Element element) {
        String text = element.getTextContent();
        if (text != null && !text.trim().isEmpty()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private ParameterDescriptor parseGenericParameter(Element element) {
        ParameterDescriptor descriptor = new ParameterDescriptor();

        String nameAttr = readAttribute(element, XMLConstantNames.XML_A_PARAMETER_NAME);
        descriptor.setName((nameAttr != null && !nameAttr.isEmpty()) ? nameAttr : element.getTagName());

        descriptor.setClassPath(readAttribute(element, XMLConstantNames.XML_A_PARAMETER_CLASSPATH));
        descriptor.setArray(Boolean.parseBoolean(readAttribute(element, XMLConstantNames.XML_A_PARAMETER_ARRAY)));
        descriptor.setMap(Boolean.parseBoolean(readAttribute(element, XMLConstantNames.XML_A_PARAMETER_MAP)));

        NodeList children = element.getChildNodes();
        boolean hasElementChildren = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            hasElementChildren = true;
            descriptor.addSubParameter(parseGenericParameter((Element) node));
        }

        if (!hasElementChildren) {
            String text = element.getTextContent();
            if (text != null) {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    descriptor.setValue(trimmed);
                }
            }
        }

        return descriptor;
    }

    private String readAttribute(Element element, String attributeName) {
        return element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
    }
}