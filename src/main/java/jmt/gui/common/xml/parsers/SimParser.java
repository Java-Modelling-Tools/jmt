package jmt.gui.common.xml.parsers;

import jmt.gui.common.xml.dto.BlockingRegionDescriptor;
import jmt.gui.common.xml.dto.ConnectionDescriptor;
import jmt.gui.common.xml.dto.MeasureDescriptor;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.dto.UserClassDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SimParser implements XmlEntityParser<SimDescriptor> {

    @Override
    public SimDescriptor parse(Element element) {
        SimDescriptor descriptor = new SimDescriptor();
        readRootAttributes(element, descriptor);

        NodeParser nodeParser = new NodeParser();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            String tagName = child.getTagName();
            if ("node".equals(tagName)) {
                NodeDescriptor nodeDescriptor = nodeParser.parse(child);
                descriptor.addNode(nodeDescriptor);
            } else if ("userClass".equals(tagName)) {
                descriptor.addUserClass(parseUserClass(child));
            } else if ("connection".equals(tagName)) {
                descriptor.addConnection(parseConnection(child));
            } else if ("measure".equals(tagName)) {
                descriptor.addMeasure(parseMeasure(child));
            } else if ("blockingRegion".equals(tagName)) {
                descriptor.addBlockingRegion(parseBlockingRegion(child));
            }
        }

        return descriptor;
    }

    private void readRootAttributes(Element element, SimDescriptor descriptor) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            descriptor.addGlobalAttribute(attribute.getNodeName(), attribute.getNodeValue());
        }
    }

    private UserClassDescriptor parseUserClass(Element element) {
        UserClassDescriptor descriptor = new UserClassDescriptor();
        descriptor.setName(readAttribute(element, "name"));
        descriptor.setType(readAttribute(element, "type"));
        descriptor.setPriority(readAttribute(element, "priority"));
        descriptor.setReferenceSource(firstNonEmpty(
                readAttribute(element, "referenceSource"),
                readAttribute(element, "refSource")));
        descriptor.setSoftDeadline(readAttribute(element, "softDeadline"));
        descriptor.setCustomers(readAttribute(element, "customers"));
        return descriptor;
    }

    private ConnectionDescriptor parseConnection(Element element) {
        ConnectionDescriptor descriptor = new ConnectionDescriptor();
        descriptor.setSource(readAttribute(element, "source"));
        descriptor.setTarget(readAttribute(element, "target"));
        return descriptor;
    }

    private MeasureDescriptor parseMeasure(Element element) {
        MeasureDescriptor descriptor = new MeasureDescriptor();
        descriptor.setName(readAttribute(element, "name"));
        descriptor.setNodeType(readAttribute(element, "nodeType"));
        descriptor.setReferenceNode(firstNonEmpty(
                readAttribute(element, "referenceNode"),
                readAttribute(element, "refNode")));
        descriptor.setReferenceUserClass(firstNonEmpty(
                readAttribute(element, "referenceUserClass"),
                readAttribute(element, "refClass")));
        descriptor.setType(readAttribute(element, "type"));
        descriptor.setAlpha(readAttribute(element, "alpha"));
        descriptor.setPrecision(readAttribute(element, "precision"));
        descriptor.setVerbose(readAttribute(element, "verbose"));
        return descriptor;
    }

    private BlockingRegionDescriptor parseBlockingRegion(Element element) {
        BlockingRegionDescriptor descriptor = new BlockingRegionDescriptor();
        descriptor.setName(readAttribute(element, "name"));
        descriptor.setType(readAttribute(element, "type"));
        return descriptor;
    }

    private String readAttribute(Element element, String attributeName) {
        return element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && first.length() > 0) {
            return first;
        }
        return second;
    }
}

