package jmt.gui.common.xml.parsers;

import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.*;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SimParser implements XmlEntityParser<SimDescriptor> {

    @Override
    public SimDescriptor parse(Element element) {
        SimDescriptor descriptor = new SimDescriptor();
        parseGlobalAttributes(element, descriptor);

        NodeParser nodeParser = new NodeParser();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element child = (Element) node;
            String tagName = child.getTagName();
            dispatchTopLevelElement(descriptor, nodeParser, child, tagName);
        }
        return descriptor;
    }

    private void parseGlobalAttributes(Element element, SimDescriptor descriptor) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            descriptor.addGlobalAttribute(attribute.getNodeName(), attribute.getNodeValue());
        }
    }

    private void dispatchTopLevelElement(SimDescriptor descriptor, NodeParser nodeParser, Element child, String tagName) {
        if (XMLConstantNames.XML_NODE.equals(tagName)) {
            descriptor.addNode(nodeParser.parse(child));
        } else if (XMLConstantNames.XML_E_CLASS.equals(tagName)) {
            descriptor.addUserClass(parseUserClass(child));
        } else if (XMLConstantNames.XML_E_CONNECTION.equals(tagName)) {
            descriptor.addConnection(parseConnection(child));
        } else if (XMLConstantNames.XML_E_MEASURE.equals(tagName)) {
            descriptor.addMeasure(parseMeasure(child));
        } else if (XMLConstantNames.XML_E_REGION.equals(tagName)) {
            descriptor.addBlockingRegion(parseBlockingRegion(child));
        } else if (XMLConstantNames.XML_E_PRELOAD.equals(tagName)) {
            parsePreload(descriptor, child);
        }
    }

    private UserClassDescriptor parseUserClass(Element child) {
        UserClassDescriptor uc = new UserClassDescriptor();
        uc.setName(readAttribute(child, XMLConstantNames.XML_A_CLASS_NAME));
        uc.setType(readAttribute(child, XMLConstantNames.XML_A_CLASS_TYPE));
        uc.setPriority(readAttribute(child, XMLConstantNames.XML_A_CLASS_PRIORITY));
        uc.setReferenceSource(firstNonEmpty(readAttribute(child, XMLConstantNames.XML_A_CLASS_REFSOURCE), readAttribute(child, XMLConstantNames.XML_A_CLASS_REFSOURCE)));
        uc.setSoftDeadline(readAttribute(child, XMLConstantNames.XML_A_CLASS_SOFT_DEADLINE));
        uc.setCustomers(readAttribute(child, XMLConstantNames.XML_A_CLASS_CUSTOMERS));
        return uc;
    }

    private ConnectionDescriptor parseConnection(Element child) {
        ConnectionDescriptor cd = new ConnectionDescriptor();
        cd.setSource(readAttribute(child, XMLConstantNames.XML_A_CONNECTION_SOURCE));
        cd.setTarget(readAttribute(child, XMLConstantNames.XML_A_CONNECTION_TARGET));
        return cd;
    }

    private MeasureDescriptor parseMeasure(Element child) {
        MeasureDescriptor md = new MeasureDescriptor();
        md.setName(readAttribute(child, XMLConstantNames.XML_A_MEASURE_NAME));
        md.setNodeType(readAttribute(child, XMLConstantNames.XML_A_MEASURE_NODETYPE));
        md.setReferenceNode(firstNonEmpty(readAttribute(child, XMLConstantNames.XML_A_MEASURE_STATION), readAttribute(child, XMLConstantNames.XML_A_MEASURE_STATION)));
        md.setReferenceUserClass(firstNonEmpty(readAttribute(child, XMLConstantNames.XML_A_MEASURE_CLASS), readAttribute(child, XMLConstantNames.XML_E_PARAMETER_REFCLASS)));
        md.setType(readAttribute(child, XMLConstantNames.XML_A_MEASURE_TYPE));
        md.setAlpha(readAttribute(child, XMLConstantNames.XML_A_MEASURE_ALPHA));
        md.setPrecision(readAttribute(child, XMLConstantNames.XML_A_MEASURE_PRECISION));
        md.setVerbose(readAttribute(child, XMLConstantNames.XML_A_MEASURE_VERBOSE));
        return md;
    }

    private BlockingRegionDescriptor parseBlockingRegion(Element child) {
        BlockingRegionDescriptor br = new BlockingRegionDescriptor();
        br.setName(readAttribute(child, XMLConstantNames.XML_A_REGION_NAME));
        br.setType(readAttribute(child, XMLConstantNames.XML_A_REGION_TYPE));

        NodeList regionChildren = child.getChildNodes();
        for (int j = 0; j < regionChildren.getLength(); j++) {
            Node regionNode = regionChildren.item(j);
            if (regionNode.getNodeType() != Node.ELEMENT_NODE) continue;

            Element regionChild = (Element) regionNode;
            dispatchRegionElement(br, child, regionChild, regionChild.getTagName());
        }
        return br;
    }

    private void dispatchRegionElement(BlockingRegionDescriptor br, Element parent, Element regionChild, String regionTag) {
        if (XMLConstantNames.XML_E_REGIONNODE.equals(regionTag)) {
            br.addRegionNode(firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_REGIONNODE_NAME), readAttribute(regionChild, XMLConstantNames.XML_A_STATION_NAME)));
        } else if (XMLConstantNames.XML_E_GLOBALCONSTRAINT.equals(regionTag)) {
            br.addGlobalConstraint(XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS, readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS));
        } else if (XMLConstantNames.XML_E_GLOBALMEMORYCONSTRAINT.equals(regionTag)) {
            br.addGlobalConstraint(XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY, readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY));
        } else if (XMLConstantNames.XML_E_CLASSCONSTRAINT.equals(regionTag)) {
            parseClassConstraint(br, parent, regionChild);
        } else if (XMLConstantNames.XML_E_CLASSMEMORYCONSTRAINT.equals(regionTag)) {
            parseClassMemoryConstraint(br, parent, regionChild);
        } else if (XMLConstantNames.XML_E_DROPRULES.equals(regionTag)) {
            parseDropRule(br, parent, regionChild);
        } else if (XMLConstantNames.XML_E_CLASSWEIGHT.equals(regionTag)) {
            parseClassWeight(br, parent, regionChild);
        } else if (XMLConstantNames.XML_E_CLASSSIZE.equals(regionTag)) {
            parseClassSize(br, parent, regionChild);
        } else if (XMLConstantNames.XML_E_CLASSDUEDATE.equals(regionTag) || XMLConstantNames.XML_A_CLASS_DUE_DATE.equals(regionTag)) {
            parseClassDueDate(br, parent, regionChild);
        } else if (XMLConstantNames.XML_E_GROUPCONSTRAINT.equals(regionTag)) {
            parseGroupConstraint(br, regionChild);
        } else if (XMLConstantNames.XML_E_GROUPMEMORYCONSTRAINT.equals(regionTag)) {
            parseGroupMemoryConstraint(br, regionChild);
        } else if (XMLConstantNames.XML_E_GROUPCLASSLIST.equals(regionTag)) {
            parseGroupClassList(br, parent, regionChild);
        }
    }

    private void parseClassConstraint(BlockingRegionDescriptor br, Element parent, Element regionChild) {
        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSCONSTRAINT_CLASS), readAttribute(parent, XMLConstantNames.XML_E_CLASS));
        br.addClassConstraint(className, XMLConstantNames.XML_A_CLASSCONSTRAINT_MAXJOBS, firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSCONSTRAINT_MAXJOBS), readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS)));
    }

    private void parseClassMemoryConstraint(BlockingRegionDescriptor br, Element parent, Element regionChild) {
        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_CLASS), readAttribute(parent, XMLConstantNames.XML_E_CLASS));
        br.addClassConstraint(className, XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY, firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY), readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY)));
    }

    private void parseDropRule(BlockingRegionDescriptor br, Element parent, Element regionChild) {
        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_DROPRULE_CLASS), readAttribute(parent, XMLConstantNames.XML_E_CLASS));
        br.addDropRule(className, firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_DROPRULE_DROP), readAttribute(regionChild, XMLConstantNames.XML_DROP)));
    }

    private void parseClassWeight(BlockingRegionDescriptor br, Element parent, Element regionChild) {
        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSWEIGHT_CLASS), readAttribute(parent, XMLConstantNames.XML_E_CLASS));
        br.addClassWeight(className, readAttribute(regionChild, XMLConstantNames.XML_A_CLASSWEIGHT_WEIGHT));
    }

    private void parseClassSize(BlockingRegionDescriptor br, Element parent, Element regionChild) {
        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSSIZE_CLASS), readAttribute(parent, XMLConstantNames.XML_E_CLASS));
        br.addClassSize(className, readAttribute(regionChild, XMLConstantNames.XML_A_CLASSSIZE_SIZE));
    }

    private void parseClassDueDate(BlockingRegionDescriptor br, Element parent, Element regionChild) {
        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSDUEDATE_CLASS), readAttribute(parent, XMLConstantNames.XML_E_CLASS));
        br.addClassConstraint(className, XMLConstantNames.XML_A_CLASS_SOFT_DEADLINE, firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSDUEDATE_DUEDATE), readAttribute(regionChild, XMLConstantNames.XML_A_CLASS_DUE_DATE)));
    }

    private void parseGroupConstraint(BlockingRegionDescriptor br, Element regionChild) {
        br.addGroupConstraint(firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_GROUPCONSTRAINT_GROUP), readAttribute(regionChild, XMLConstantNames.XML_GROUP)),
                firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_GROUPCONSTRAINT_MAXJOBS), readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS)));
    }

    private void parseGroupMemoryConstraint(BlockingRegionDescriptor br, Element regionChild) {
        br.addGroupMemoryConstraint(firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_GROUPMEMORYCONSTRAINT_GROUP), readAttribute(regionChild, XMLConstantNames.XML_GROUP)),
                firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_GROUPMEMORYCONSTRAINT_MAXMEMORY), readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY)));
    }

    private void parseGroupClassList(BlockingRegionDescriptor br, Element parent, Element regionChild) {
        NodeList groupClassNodes = regionChild.getChildNodes();
        for (int k = 0; k < groupClassNodes.getLength(); k++) {
            Node classNode = groupClassNodes.item(k);
            if (classNode.getNodeType() != Node.ELEMENT_NODE || !XMLConstantNames.XML_E_GROUPCLASS.equals(classNode.getNodeName())) continue;
            Element groupClass = (Element) classNode;
            br.addGroupClass(firstNonEmpty(readAttribute(groupClass, XMLConstantNames.XML_A_GROUPCLASS_GROUP), readAttribute(groupClass, XMLConstantNames.XML_GROUP)),
                    firstNonEmpty(readAttribute(groupClass, XMLConstantNames.XML_A_GROUPCLASS_CLASS), readAttribute(parent, XMLConstantNames.XML_E_CLASS)));
        }
    }

    private void parsePreload(SimDescriptor descriptor, Element child) {
        NodeList stationPops = child.getElementsByTagName(XMLConstantNames.XML_E_STATIONPOPULATIONS);
        for (int j = 0; j < stationPops.getLength(); j++) {
            Element statPop = (Element) stationPops.item(j);
            descriptor.addPreload(getNodeDescriptor(statPop));
        }
    }

    private NodeDescriptor getNodeDescriptor(Element statPop) {
        NodeDescriptor pd = new NodeDescriptor();
        pd.setName(statPop.getAttribute(XMLConstantNames.XML_A_PRELOADSTATION_NAME));
        NodeList classPops = statPop.getElementsByTagName(XMLConstantNames.XML_E_CLASSPOPULATION);
        for (int k = 0; k < classPops.getLength(); k++) {
            Element cPop = (Element) classPops.item(k);
            SectionDescriptor sd = new SectionDescriptor();
            sd.setClassName(firstNonEmpty(cPop.getAttribute("populationName"), cPop.getAttribute(XMLConstantNames.XML_A_CLASSPOPULATION_NAME)));
            ParameterDescriptor val = new ParameterDescriptor();
            val.setValue(cPop.getAttribute(XMLConstantNames.XML_A_CLASSPOPULATION_POPULATION));
            sd.addParameter(val);
            pd.addSection(sd);
        }
        return pd;
    }

    private String readAttribute(Element element, String attributeName) {
        return element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.isEmpty()) return first;
        return second;
    }
}