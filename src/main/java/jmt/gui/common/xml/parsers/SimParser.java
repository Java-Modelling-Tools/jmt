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
            } else if (XMLConstantNames.XML_E_CLASS.equals(tagName)) {
                UserClassDescriptor uc = new UserClassDescriptor();
                uc.setName(readAttribute(child, XMLConstantNames.XML_A_CLASS_NAME));
                uc.setType(readAttribute(child, XMLConstantNames.XML_A_CLASS_TYPE));
                uc.setPriority(readAttribute(child, XMLConstantNames.XML_A_CLASS_PRIORITY));
                uc.setReferenceSource(firstNonEmpty(readAttribute(child, XMLConstantNames.XML_A_CLASS_REFSOURCE), readAttribute(child, XMLConstantNames.XML_A_CLASS_REFSOURCE)));
                uc.setSoftDeadline(readAttribute(child, XMLConstantNames.XML_A_CLASS_SOFT_DEADLINE));
                uc.setCustomers(readAttribute(child, XMLConstantNames.XML_A_CLASS_CUSTOMERS));
                descriptor.addUserClass(uc);
            } else if (XMLConstantNames.XML_E_CONNECTION.equals(tagName)) {
                ConnectionDescriptor cd = new ConnectionDescriptor();
                cd.setSource(readAttribute(child, XMLConstantNames.XML_A_CONNECTION_SOURCE));
                cd.setTarget(readAttribute(child, XMLConstantNames.XML_A_CONNECTION_TARGET));
                descriptor.addConnection(cd);
            } else if (XMLConstantNames.XML_E_MEASURE.equals(tagName)) {
                MeasureDescriptor md = new MeasureDescriptor();
                md.setName(readAttribute(child, XMLConstantNames.XML_A_MEASURE_NAME));
                md.setNodeType(readAttribute(child, XMLConstantNames.XML_A_MEASURE_NODETYPE));
                md.setReferenceNode(firstNonEmpty(readAttribute(child, XMLConstantNames.XML_A_MEASURE_STATION), readAttribute(child, XMLConstantNames.XML_A_MEASURE_STATION)));
                md.setReferenceUserClass(firstNonEmpty(readAttribute(child, XMLConstantNames.XML_A_MEASURE_CLASS), readAttribute(child, XMLConstantNames.XML_E_PARAMETER_REFCLASS)));
                md.setType(readAttribute(child, XMLConstantNames.XML_A_MEASURE_TYPE));
                md.setAlpha(readAttribute(child, XMLConstantNames.XML_A_MEASURE_ALPHA));
                md.setPrecision(readAttribute(child, XMLConstantNames.XML_A_MEASURE_PRECISION));
                md.setVerbose(readAttribute(child, XMLConstantNames.XML_A_MEASURE_VERBOSE));
                descriptor.addMeasure(md);
            } else if (XMLConstantNames.XML_E_REGION.equals(tagName)) {
                BlockingRegionDescriptor br = new BlockingRegionDescriptor();
                br.setName(readAttribute(child, XMLConstantNames.XML_A_REGION_NAME));
                br.setType(readAttribute(child, XMLConstantNames.XML_A_REGION_TYPE));

                NodeList regionChildren = child.getChildNodes();
                for (int j = 0; j < regionChildren.getLength(); j++) {
                    Node regionNode = regionChildren.item(j);
                    if (regionNode.getNodeType() != Node.ELEMENT_NODE) continue;

                    Element regionChild = (Element) regionNode;
                    String regionTag = regionChild.getTagName();
                    if (XMLConstantNames.XML_E_REGIONNODE.equals(regionTag)) {
                        br.addRegionNode(firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_REGIONNODE_NAME), readAttribute(regionChild, XMLConstantNames.XML_A_STATION_NAME)));
                    } else if (XMLConstantNames.XML_E_GLOBALCONSTRAINT.equals(regionTag)) {
                        br.addGlobalConstraint(XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS, readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS));
                    } else if (XMLConstantNames.XML_E_GLOBALMEMORYCONSTRAINT.equals(regionTag)) {
                        br.addGlobalConstraint(XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY, readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY));
                    } else if (XMLConstantNames.XML_E_CLASSCONSTRAINT.equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSCONSTRAINT_CLASS), readAttribute(child, XMLConstantNames.XML_E_CLASS));
                        br.addClassConstraint(className, XMLConstantNames.XML_A_CLASSCONSTRAINT_MAXJOBS, firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSCONSTRAINT_MAXJOBS), readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS)));
                    } else if (XMLConstantNames.XML_E_CLASSMEMORYCONSTRAINT.equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_CLASS), readAttribute(child, XMLConstantNames.XML_E_CLASS));
                        br.addClassConstraint(className, XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY, firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY), readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY)));
                    } else if (XMLConstantNames.XML_E_DROPRULES.equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_DROPRULE_CLASS), readAttribute(child, XMLConstantNames.XML_E_CLASS));
                        br.addDropRule(className, firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_DROPRULE_DROP), readAttribute(regionChild, XMLConstantNames.XML_DROP)));
                    } else if (XMLConstantNames.XML_E_CLASSWEIGHT.equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSWEIGHT_CLASS), readAttribute(child, XMLConstantNames.XML_E_CLASS));
                        br.addClassWeight(className, readAttribute(regionChild, XMLConstantNames.XML_A_CLASSWEIGHT_WEIGHT));
                    } else if (XMLConstantNames.XML_E_CLASSSIZE.equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSSIZE_CLASS), readAttribute(child, XMLConstantNames.XML_E_CLASS));
                        br.addClassSize(className, readAttribute(regionChild, XMLConstantNames.XML_A_CLASSSIZE_SIZE));
                    } else if (XMLConstantNames.XML_E_CLASSDUEDATE.equals(regionTag) || XMLConstantNames.XML_A_CLASS_DUE_DATE.equals(regionTag)) {
                        String className = firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSDUEDATE_CLASS), readAttribute(child, XMLConstantNames.XML_E_CLASS));
                        br.addClassConstraint(className, XMLConstantNames.XML_A_CLASS_SOFT_DEADLINE, firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_CLASSDUEDATE_DUEDATE), readAttribute(regionChild, XMLConstantNames.XML_A_CLASS_DUE_DATE)));
                    } else if (XMLConstantNames.XML_E_GROUPCONSTRAINT.equals(regionTag)) {
                        br.addGroupConstraint(firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_GROUPCONSTRAINT_GROUP), readAttribute(regionChild, XMLConstantNames.XML_GROUP)),
                                firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_GROUPCONSTRAINT_MAXJOBS), readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS)));
                    } else if (XMLConstantNames.XML_E_GROUPMEMORYCONSTRAINT.equals(regionTag)) {
                        br.addGroupMemoryConstraint(firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_GROUPMEMORYCONSTRAINT_GROUP), readAttribute(regionChild, XMLConstantNames.XML_GROUP)),
                                firstNonEmpty(readAttribute(regionChild, XMLConstantNames.XML_A_GROUPMEMORYCONSTRAINT_MAXMEMORY), readAttribute(regionChild, XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY)));
                    } else if (XMLConstantNames.XML_E_GROUPCLASSLIST.equals(regionTag)) {
                        NodeList groupClassNodes = regionChild.getChildNodes();
                        for (int k = 0; k < groupClassNodes.getLength(); k++) {
                            Node classNode = groupClassNodes.item(k);
                            if (classNode.getNodeType() != Node.ELEMENT_NODE) continue;
                            Element groupClass = (Element) classNode;
                            if (!XMLConstantNames.XML_E_GROUPCLASS.equals(groupClass.getTagName())) continue;
                            br.addGroupClass(firstNonEmpty(readAttribute(groupClass, XMLConstantNames.XML_A_GROUPCLASS_GROUP), readAttribute(groupClass, XMLConstantNames.XML_GROUP)),
                                    firstNonEmpty(readAttribute(groupClass, XMLConstantNames.XML_A_GROUPCLASS_CLASS), readAttribute(child, XMLConstantNames.XML_E_CLASS)));
                        }
                    }
                }
                descriptor.addBlockingRegion(br);
            } else if (XMLConstantNames.XML_E_PRELOAD.equals(tagName)) {
                NodeList stationPops = child.getElementsByTagName(XMLConstantNames.XML_E_STATIONPOPULATIONS);
                for (int j = 0; j < stationPops.getLength(); j++) {
                    Element statPop = (Element) stationPops.item(j);
                    NodeDescriptor pd = getNodeDescriptor(statPop);
                    descriptor.addPreload(pd);
                }
            }
        }
        return descriptor;
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