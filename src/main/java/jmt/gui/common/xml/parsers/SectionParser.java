/**
 * This class is responsible for parsing an XML element representing a section and converting it into a {@link SectionDescriptor} object.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.parsers;

import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.SectionDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SectionParser implements XmlEntityParser<SectionDescriptor>, XMLConstantNames {

    @Override
    public SectionDescriptor parse(Element element) {
        SectionDescriptor descriptor = new SectionDescriptor();
        descriptor.setClassName(readAttribute(element, XML_A_STATION_SECTION_CLASSNAME));

        ParameterParser parameterParser = new ParameterParser();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            if (XML_E_PARAMETER.equals(child.getTagName())) {
                descriptor.addParameter(parameterParser.parse(child));
            }
        }

        return descriptor;
    }

    private String readAttribute(Element element, String attributeName) {
        return element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
    }
}
