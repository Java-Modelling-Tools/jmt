package jmt.gui.common.xml.parsers;

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

            if ("section".equals(tagName)) {
                descriptor.addSection(sectionParser.parse(child));
            } else if ("classSoftDeadlines".equals(tagName)) {
                descriptor.setClassSoftDeadlines(parseSoftDeadlines(child));
            } else if ("quantumSize".equals(tagName)) {
                descriptor.setQuantumSize(parseContentAsDouble(child));
            } else if ("quantumSwitchoverTime".equals(tagName)) {
                descriptor.setQuantumSwitchoverTime(parseContentAsDouble(child));
            } else {
                descriptor.addParameter(parseGenericParameter(child));
            }
        }

        return descriptor;
    }

    private List<Double> parseSoftDeadlines(Element element) {
        List<Double> deadlines = new ArrayList<>();
        NodeList nodes = element.getChildNodes();
        for (int j = 0; j < nodes.getLength(); j++) {
            Node node = nodes.item(j);
            if (node.getNodeType() == Node.ELEMENT_NODE && "softDeadline".equals(((Element) node).getTagName())) {
                Double val = parseContentAsDouble((Element) node);
                if (val != null) deadlines.add(val);
            }
        }
        return deadlines;
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

        String nameAttr = readAttribute(element, "name");
        descriptor.setName((nameAttr != null && !nameAttr.isEmpty()) ? nameAttr : element.getTagName());

        descriptor.setClassPath(readAttribute(element, "classPath"));
        descriptor.setArray(Boolean.parseBoolean(readAttribute(element, "array")));

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