/**
 * This class is responsible for parsing an XML element representing a parameter and converting it into a {@link ParameterDescriptor} object.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
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

        String pendingRefClass = null;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element child = (Element) node;
            String tagName = child.getTagName();

            if ("value".equals(tagName)) {
                descriptor.setValue(child.getTextContent());
            } else if ("refClass".equals(tagName)) {
                pendingRefClass = child.getTextContent();
            } else if ("subParameter".equals(tagName)) {
                ParameterDescriptor subParameter = parse(child);
                if (pendingRefClass != null) {
                    subParameter.setRefClass(pendingRefClass);
                    pendingRefClass = null;
                }
                descriptor.addSubParameter(subParameter);
            }
        }
        if (pendingRefClass != null && descriptor.getRefClass() == null) {
            descriptor.setRefClass(pendingRefClass);
        }
        return descriptor;
    }

    private String readAttribute(Element element, String attributeName) {
        return element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
    }
}