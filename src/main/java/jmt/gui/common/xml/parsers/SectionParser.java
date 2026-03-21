package jmt.gui.common.xml.parsers;

import jmt.gui.common.xml.dto.SectionDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SectionParser implements XmlEntityParser<SectionDescriptor> {

    @Override
    public SectionDescriptor parse(Element element) {
        SectionDescriptor descriptor = new SectionDescriptor();
        descriptor.setClassName(readAttribute(element, "className"));

        ParameterParser parameterParser = new ParameterParser();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            if ("parameter".equals(child.getTagName())) {
                descriptor.addParameter(parameterParser.parse(child));
            }
        }

        return descriptor;
    }

    private String readAttribute(Element element, String attributeName) {
        return element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
    }
}

