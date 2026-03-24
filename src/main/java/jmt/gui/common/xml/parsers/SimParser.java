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
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            descriptor.addGlobalAttribute(attribute.getNodeName(), attribute.getNodeValue());
        }

        NodeParser nodeParser = new NodeParser();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element child = (Element) node;
            String tagName = child.getTagName();
            if (XMLConstantNames.XML_NODE.equals(tagName)) {
                descriptor.addNode(nodeParser.parse(child));
            } else if ("userClass".equals(tagName)) {
                UserClassDescriptor uc = new UserClassDescriptor();
                uc.setName(readAttribute(child, "name"));
                uc.setType(readAttribute(child, "type"));
                uc.setPriority(readAttribute(child, "priority"));
                uc.setReferenceSource(firstNonEmpty(readAttribute(child, "referenceSource"), readAttribute(child, "refSource")));
                uc.setSoftDeadline(readAttribute(child, "softDeadline"));
                uc.setCustomers(readAttribute(child, "customers"));
                descriptor.addUserClass(uc);
            } else if ("connection".equals(tagName)) {
                ConnectionDescriptor cd = new ConnectionDescriptor();
                cd.setSource(readAttribute(child, "source"));
                cd.setTarget(readAttribute(child, "target"));
                descriptor.addConnection(cd);
            } else if ("measure".equals(tagName)) {
                MeasureDescriptor md = new MeasureDescriptor();
                md.setName(readAttribute(child, "name"));
                md.setNodeType(readAttribute(child, "nodeType"));
                md.setReferenceNode(firstNonEmpty(readAttribute(child, "referenceNode"), readAttribute(child, "refNode")));
                md.setReferenceUserClass(firstNonEmpty(readAttribute(child, "referenceUserClass"), readAttribute(child, "refClass")));
                md.setType(readAttribute(child, "type"));
                md.setAlpha(readAttribute(child, "alpha"));
                md.setPrecision(readAttribute(child, "precision"));
                md.setVerbose(readAttribute(child, "verbose"));
                descriptor.addMeasure(md);
            } else if ("blockingRegion".equals(tagName)) {
                BlockingRegionDescriptor br = new BlockingRegionDescriptor();
                br.setName(readAttribute(child, "name"));
                br.setType(readAttribute(child, "type"));

                NodeList regionChildren = child.getChildNodes();
                for (int j = 0; j < regionChildren.getLength(); j++) {
                    Node regionNode = regionChildren.item(j);
                    if (regionNode.getNodeType() != Node.ELEMENT_NODE) continue;

                    Element regionChild = (Element) regionNode;
                    String regionTag = regionChild.getTagName();
                    if ("regionNode".equals(regionTag)) {
                        br.addRegionNode(firstNonEmpty(readAttribute(regionChild, "nodeName"), readAttribute(regionChild, "name")));
                    } else if ("globalConstraint".equals(regionTag)) {
                        br.addGlobalConstraint("maxJobs", readAttribute(regionChild, "maxJobs"));
                    } else if ("globalMemoryConstraint".equals(regionTag)) {
                        br.addGlobalConstraint("maxMemory", readAttribute(regionChild, "maxMemory"));
                    } else if ("classConstraint".equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, "jobClass"), readAttribute(regionChild, "class"));
                        br.addClassConstraint(className, "maxJobsPerClass", firstNonEmpty(readAttribute(regionChild, "maxJobsPerClass"), readAttribute(regionChild, "maxJobs")));
                    } else if ("classMemoryConstraint".equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, "jobClass"), readAttribute(regionChild, "class"));
                        br.addClassConstraint(className, "maxMemoryPerClass", firstNonEmpty(readAttribute(regionChild, "maxMemoryPerClass"), readAttribute(regionChild, "maxMemory")));
                    } else if ("dropRules".equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, "jobClass"), readAttribute(regionChild, "class"));
                        br.addDropRule(className, firstNonEmpty(readAttribute(regionChild, "dropThisClass"), readAttribute(regionChild, "drop")));
                    } else if ("classWeight".equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, "jobClass"), readAttribute(regionChild, "class"));
                        br.addClassWeight(className, readAttribute(regionChild, "weight"));
                    } else if ("classSize".equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, "jobClass"), readAttribute(regionChild, "class"));
                        br.addClassSize(className, readAttribute(regionChild, "size"));
                    } else if ("classSoftDeadline".equals(regionTag) || "classDueDate".equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, "jobClass"), readAttribute(regionChild, "class"));
                        br.addClassConstraint(className, "softDeadline", firstNonEmpty(readAttribute(regionChild, "softDeadline"), readAttribute(regionChild, "dueDate")));
                    } else if ("groupConstraint".equals(regionTag)) {
                        br.addGroupConstraint(firstNonEmpty(readAttribute(regionChild, "jobGroup"), readAttribute(regionChild, "group")),
                                firstNonEmpty(readAttribute(regionChild, "maxJobsPerGroup"), readAttribute(regionChild, "maxJobs")));
                    } else if ("groupMemoryConstraint".equals(regionTag)) {
                        br.addGroupMemoryConstraint(firstNonEmpty(readAttribute(regionChild, "jobGroup"), readAttribute(regionChild, "group")),
                                firstNonEmpty(readAttribute(regionChild, "maxMemoryPerGroup"), readAttribute(regionChild, "maxMemory")));
                    } else if ("groupClassList".equals(regionTag)) {
                        NodeList groupClassNodes = regionChild.getChildNodes();
                        for (int k = 0; k < groupClassNodes.getLength(); k++) {
                            Node classNode = groupClassNodes.item(k);
                            if (classNode.getNodeType() != Node.ELEMENT_NODE) continue;
                            Element groupClass = (Element) classNode;
                            if (!"groupClass".equals(groupClass.getTagName())) continue;
                            br.addGroupClass(firstNonEmpty(readAttribute(groupClass, "jobGroup"), readAttribute(groupClass, "group")),
                                    firstNonEmpty(readAttribute(groupClass, "jobClass"), readAttribute(groupClass, "class")));
                        }
                    }
                }
                descriptor.addBlockingRegion(br);
            } else if ("preload".equals(tagName)) {
                NodeList stationPops = child.getElementsByTagName("stationPopulations");
                for (int j = 0; j < stationPops.getLength(); j++) {
                    Element statPop = (Element) stationPops.item(j);
                    NodeDescriptor pd = new NodeDescriptor();
                    pd.setName(statPop.getAttribute("stationName"));
                    NodeList classPops = statPop.getElementsByTagName("classPopulation");
                    for (int k = 0; k < classPops.getLength(); k++) {
                        Element cPop = (Element) classPops.item(k);
                        SectionDescriptor sd = new SectionDescriptor();
                        sd.setClassName(firstNonEmpty(cPop.getAttribute("populationName"), cPop.getAttribute("refClass")));
                        ParameterDescriptor val = new ParameterDescriptor();
                        val.setValue(cPop.getAttribute("population"));
                        sd.addParameter(val);
                        pd.addSection(sd);
                    }
                    descriptor.addPreload(pd);
                }
            }
        }
        return descriptor;
    }

    private String readAttribute(Element element, String attributeName) {
        return element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.isEmpty()) return first;
        return second;
    }
}