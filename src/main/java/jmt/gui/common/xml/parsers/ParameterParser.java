package jmt.gui.common.xml.parsers;

import jmt.gui.common.xml.dto.ParameterDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ParameterParser implements XmlEntityParser<ParameterDescriptor> {

    @Override
    public ParameterDescriptor parse(Element element) {
        ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.setName(readAttribute(element, "name"));
        descriptor.setClassPath(readAttribute(element, "classPath"));
        descriptor.setArray(Boolean.parseBoolean(readAttribute(element, "array")));

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            String tagName = child.getTagName();
            if ("value".equals(tagName)) {
                descriptor.setValue(child.getTextContent());
            } else if ("refClass".equals(tagName)) {
                descriptor.setRefClass(child.getTextContent());
            } else if ("subParameter".equals(tagName)) {
                ParameterDescriptor subParameter = parse(child);
                descriptor.addSubParameter(subParameter);
            }
        }

        return descriptor;
    }

    private String readAttribute(Element element, String attributeName) {
        return element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
    }
}

