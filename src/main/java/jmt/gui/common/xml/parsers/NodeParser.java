package jmt.gui.common.xml.parsers;

import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeParser implements XmlEntityParser<NodeDescriptor> {

    @Override
    public NodeDescriptor parse(Element element) {
        NodeDescriptor descriptor = new NodeDescriptor();
        descriptor.setName(readAttribute(element, "name"));

        SectionParser sectionParser = new SectionParser();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            if ("section".equals(child.getTagName())) {
                descriptor.addSection(sectionParser.parse(child));
            } else {
                descriptor.addParameter(parseGenericParameter(child));
            }
        }

        return descriptor;
    }

    private ParameterDescriptor parseGenericParameter(Element element) {
        ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.setName(element.getTagName());
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
