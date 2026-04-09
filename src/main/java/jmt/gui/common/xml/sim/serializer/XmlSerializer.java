/**
 * Serializes a {@link jmt.gui.common.xml.sim.dto.SimDescriptor} DTO into a DOM {@link org.w3c.dom.Document},
 * which can then be written to file by the new XMLWriter.
 *
 * <p>This class is the symmetric counterpart of the XML→DTO parsing phase.
 * It knows about the DOM structure but knows nothing about {@link jmt.gui.common.definitions.CommonModel}:
 * all model data must already be encoded in the DTO before calling
 * {@link jmt.gui.common.xml.sim.serializer.XmlSerializer#serialize(jmt.gui.common.xml.sim.dto.SimDescriptor)}.
 *
 * <p><b>Key invariant (replicated from the legacy {@code XMLParameter.appendParameterElement}):</b>
 * when a {@link jmt.gui.common.xml.sim.dto.ParameterDescriptor} has a {@code refClass}, the
 * {@code <refClass>} element is appended to the <em>parent</em> node <em>before</em> the parameter
 * element itself. This matches the original writer's behaviour exactly.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.serializer;

import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlSerializer implements XMLConstantNames {

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Serializes the given {@link SimDescriptor} into a fresh DOM {@link Document}.
     *
     * @param sim the fully-populated simulation descriptor
     * @return a DOM document ready to be transformed to a file/stream
     */
    public Document serialize(SimDescriptor sim) {
        Document doc = createDocument();
        Element root = doc.createElement(XML_DOCUMENT_ROOT);
        doc.appendChild(root);

        // --- Root attributes (archive name, timestamp, global simulation params) ---
        if (sim.getArchiveName() != null) {
            root.setAttribute(XML_A_ROOT_NAME, sim.getArchiveName());
        }
        root.setAttribute(XML_XSI_NO_NAMESPACE_SCHEMA_LOCATION, XML_DOCUMENT_XSD);
        root.setAttribute(XML_XMLNS_XSI, XML_XSI_SCHEMA_LOCATION_URI);

        // All other global attributes (seed, maxTime, maxSamples, polling, log params, …)
        // are stored as a plain map in SimDescriptor and emitted here.
        sim.getGlobalAttributes().forEach(root::setAttribute);

        // --- Child sections, in the same order as the legacy XMLWriter.writeModel() ---
        writeUserClasses(doc, root, sim);
        writeNodes(doc, root, sim);
        writeMeasures(doc, root, sim);
        writeConnections(doc, root, sim);
        writeBlockingRegions(doc, root, sim);
        writePreload(doc, root, sim);

        return doc;
    }

    // -----------------------------------------------------------------------
    // User classes
    // -----------------------------------------------------------------------

    private void writeUserClasses(Document doc, Element root, SimDescriptor sim) {
        for (UserClassDescriptor uc : sim.getUserClasses()) {
            Element el = doc.createElement(XML_E_CLASS);
            setAttrIfNotNull(el, XML_A_CLASS_NAME,           uc.getName());
            setAttrIfNotNull(el, XML_A_CLASS_TYPE,           uc.getType());
            setAttrIfNotNull(el, XML_A_CLASS_PRIORITY,       uc.getPriority());
            setAttrIfNotNull(el, XML_A_CLASS_SOFT_DEADLINE,  uc.getSoftDeadline());
            setAttrIfNotNull(el, XML_A_CLASS_CUSTOMERS,      uc.getCustomers());
            setAttrIfNotNull(el, XML_A_CLASS_REFSOURCE,      uc.getReferenceSource());
            setAttrIfNotNull(el, XML_A_CLASS_CACHEMISSCLASS, uc.getCacheMissClass());
            setAttrIfNotNull(el, XML_A_CLASS_CACHEHITCLASS,  uc.getCacheHitClass());
            root.appendChild(el);
        }
    }

    // -----------------------------------------------------------------------
    // Nodes (stations)
    // -----------------------------------------------------------------------

    private void writeNodes(Document doc, Element root, SimDescriptor sim) {
        for (NodeDescriptor node : sim.getNodes()) {
            Element nodeEl = doc.createElement(XML_NODE);
            nodeEl.setAttribute(XML_A_STATION_NAME, node.getName());

            writeClassSoftDeadlines(doc, nodeEl, node);
            writeQuantumSize(doc, nodeEl, node);
            writeQuantumSwitchoverTime(doc, nodeEl, node);

            for (SectionDescriptor sec : node.getSections()) {
                Element secEl = doc.createElement(XML_E_STATION_SECTION);
                secEl.setAttribute(XML_A_STATION_SECTION_CLASSNAME, sec.getClassName());
                nodeEl.appendChild(secEl);

                for (ParameterDescriptor param : sec.getParameters()) {
                    writeParameter(param, secEl, doc, false);
                }
            }

            root.appendChild(nodeEl);
        }
    }

    private void writeClassSoftDeadlines(Document doc, Element nodeEl, NodeDescriptor node) {
        if (node.getClassSoftDeadlines() == null || node.getClassSoftDeadlines().isEmpty()) return;
        Element container = doc.createElement(XML_E_STATION_DUE_DATES);
        for (Double d : node.getClassSoftDeadlines()) {
            Element entry = doc.createElement(XML_E_SOFT_DEADLINE_ELEMENT);
            entry.setTextContent(d.toString());
            container.appendChild(entry);
        }
        nodeEl.appendChild(container);
    }

    private void writeQuantumSize(Document doc, Element nodeEl, NodeDescriptor node) {
        if (node.getQuantumSize() == null || node.getQuantumSize().isEmpty()) return;
        Element container = doc.createElement(XML_E_STATION_QUANTUM_SIZE);
        for (Double d : node.getQuantumSize()) {
            Element entry = doc.createElement(XML_E_QUANTA_SIZE);
            entry.setTextContent(d.toString());
            container.appendChild(entry);
        }
        nodeEl.appendChild(container);
    }

    private void writeQuantumSwitchoverTime(Document doc, Element nodeEl, NodeDescriptor node) {
        if (node.getQuantumSwitchoverTime() == null || node.getQuantumSwitchoverTime().isEmpty()) return;
        Element container = doc.createElement(XML_E_STATION_QUANTUM_SWITCHOVER_TIME);
        for (Double d : node.getQuantumSwitchoverTime()) {
            Element entry = doc.createElement(XML_E_QUANTUM_SWITCHOVER_TIME_ELEMENT);
            entry.setTextContent(d.toString());
            container.appendChild(entry);
        }
        nodeEl.appendChild(container);
    }

    // -----------------------------------------------------------------------
    // Measures
    // -----------------------------------------------------------------------

    private void writeMeasures(Document doc, Element root, SimDescriptor sim) {
        for (MeasureDescriptor m : sim.getMeasures()) {
            Element el = doc.createElement(XML_E_MEASURE);
            setAttrIfNotNull(el, XML_A_MEASURE_TYPE,          m.getType());
            setAttrIfNotNull(el, XML_A_MEASURE_STATION,       m.getReferenceNode());
            setAttrIfNotNull(el, XML_A_MEASURE_NODETYPE,      m.getNodeType());
            setAttrIfNotNull(el, XML_A_MEASURE_CLASS,         m.getReferenceUserClass());
            setAttrIfNotNull(el, XML_A_MEASURE_ALPHA,         m.getAlpha());
            setAttrIfNotNull(el, XML_A_MEASURE_PRECISION,     m.getPrecision());
            setAttrIfNotNull(el, XML_A_MEASURE_NAME,          m.getName());
            setAttrIfNotNull(el, XML_A_MEASURE_VERBOSE,       m.getVerbose());
            setAttrIfNotNull(el, XML_A_MEASURE_SERVERTYPE,    m.getServerType());
            root.appendChild(el);
        }
    }

    // -----------------------------------------------------------------------
    // Connections
    // -----------------------------------------------------------------------

    private void writeConnections(Document doc, Element root, SimDescriptor sim) {
        for (ConnectionDescriptor c : sim.getConnections()) {
            Element el = doc.createElement(XML_E_CONNECTION);
            el.setAttribute(XML_A_CONNECTION_SOURCE, c.getSource());
            el.setAttribute(XML_A_CONNECTION_TARGET, c.getTarget());
            root.appendChild(el);
        }
    }

    // -----------------------------------------------------------------------
    // Blocking regions
    // -----------------------------------------------------------------------

    private void writeBlockingRegions(Document doc, Element root, SimDescriptor sim) {
        for (BlockingRegionDescriptor region : sim.getBlockingRegions()) {
            Element el = doc.createElement(XML_E_REGION);
            el.setAttribute(XML_A_REGION_NAME, region.getName());
            el.setAttribute(XML_A_REGION_TYPE, region.getType());

            for (String stationName : region.getRegionNodes()) {
                Element nodeEl = doc.createElement(XML_E_REGIONNODE);
                nodeEl.setAttribute(XML_A_REGIONNODE_NAME, stationName);
                el.appendChild(nodeEl);
            }

            Element gc = doc.createElement(XML_E_GLOBALCONSTRAINT);
            gc.setAttribute(XML_A_GLOBALCONSTRAINT_MAXJOBS, region.getGlobalConstraints().get(XML_A_GLOBALCONSTRAINT_MAXJOBS));
            el.appendChild(gc);

            Element gmc = doc.createElement(XML_E_GLOBALMEMORYCONSTRAINT);
            gmc.setAttribute(XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY, region.getGlobalConstraints().get(XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY));
            el.appendChild(gmc);

            // FIX: Ordine rigoroso XSD per i child element della region
            // 1. classConstraint
            region.getClassConstraints().forEach((className, props) -> {
                Element ccEl = doc.createElement(XML_E_CLASSCONSTRAINT);
                ccEl.setAttribute(XML_A_CLASSCONSTRAINT_CLASS,   className);
                ccEl.setAttribute(XML_A_CLASSCONSTRAINT_MAXJOBS, props.get(XML_A_CLASSCONSTRAINT_MAXJOBS));
                el.appendChild(ccEl);
            });
            // 2. classMemoryConstraint
            region.getClassConstraints().forEach((className, props) -> {
                Element cmcEl = doc.createElement(XML_E_CLASSMEMORYCONSTRAINT);
                cmcEl.setAttribute(XML_A_CLASSMEMORYCONSTRAINT_CLASS,     className);
                cmcEl.setAttribute(XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY, props.get(XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY));
                el.appendChild(cmcEl);
            });
            // 3. dropRules
            region.getDropRules().forEach((className, rule) -> {
                Element drEl = doc.createElement(XML_E_DROPRULES);
                drEl.setAttribute(XML_A_DROPRULE_CLASS, className);
                drEl.setAttribute(XML_A_DROPRULE_DROP,  rule);
                el.appendChild(drEl);
            });
            // 4. classWeight
            region.getClassWeights().forEach((className, weight) -> {
                Element cwEl = doc.createElement(XML_E_CLASSWEIGHT);
                cwEl.setAttribute(XML_A_CLASSWEIGHT_CLASS,  className);
                cwEl.setAttribute(XML_A_CLASSWEIGHT_WEIGHT, weight);
                el.appendChild(cwEl);
            });
            // 5. classSize
            if (region.getClassSizes() != null) {
                region.getClassSizes().forEach((className, size) -> {
                    Element csEl = doc.createElement(XML_E_CLASSSIZE);
                    csEl.setAttribute(XML_A_CLASSSIZE_CLASS, className);
                    csEl.setAttribute(XML_A_CLASSSIZE_SIZE, size);
                    el.appendChild(csEl);
                });
            }
            // 6. classSoftDeadline
            region.getClassConstraints().forEach((className, props) -> {
                String deadline = props.get(XML_A_CLASS_SOFT_DEADLINE);
                if (deadline != null) {
                    Element csdEl = doc.createElement(XML_E_CLASSDUEDATE);
                    csdEl.setAttribute(XML_A_CLASSDUEDATE_CLASS,   className);
                    csdEl.setAttribute(XML_A_CLASSDUEDATE_DUEDATE, deadline);
                    el.appendChild(csdEl);
                }
            });

            // Group constraints
            region.getGroupConstraints().forEach((groupName, maxJobs) -> {
                Element gcEl = doc.createElement(XML_E_GROUPCONSTRAINT);
                gcEl.setAttribute(XML_A_GROUPCONSTRAINT_GROUP,   groupName);
                gcEl.setAttribute(XML_A_GROUPCONSTRAINT_MAXJOBS, maxJobs);
                el.appendChild(gcEl);
            });
            region.getGroupMemoryConstraints().forEach((groupName, maxMemory) -> {
                Element gmcEl = doc.createElement(XML_E_GROUPMEMORYCONSTRAINT);
                gmcEl.setAttribute(XML_A_GROUPMEMORYCONSTRAINT_GROUP,     groupName);
                gmcEl.setAttribute(XML_A_GROUPMEMORYCONSTRAINT_MAXMEMORY, maxMemory);
                el.appendChild(gmcEl);
            });
            region.getGroupClassLists().forEach((groupName, classNames) -> {
                Element gclEl = doc.createElement(XML_E_GROUPCLASSLIST);
                for (String className : classNames) {
                    Element gcmEl = doc.createElement(XML_E_GROUPCLASS);
                    gcmEl.setAttribute(XML_A_GROUPCLASS_GROUP, groupName);
                    gcmEl.setAttribute(XML_A_GROUPCLASS_CLASS, className);
                    gclEl.appendChild(gcmEl);
                }
                el.appendChild(gclEl);
            });

            root.appendChild(el);
        }
    }

    // -----------------------------------------------------------------------
    // Preload
    // -----------------------------------------------------------------------

    private void writePreload(Document doc, Element root, SimDescriptor sim) {
        if (sim.getPreloads() == null || sim.getPreloads().isEmpty()) return;

        Element preloadEl = doc.createElement(XML_E_PRELOAD);
        boolean hasAny = false;

        for (NodeDescriptor preload : sim.getPreloads()) {
            if (preload.getSections() == null || preload.getSections().isEmpty()) {
                continue;
            }

            Element stationEl = doc.createElement(XML_E_STATIONPOPULATIONS);
            stationEl.setAttribute(XML_A_PRELOADSTATION_NAME, preload.getName());

            for (SectionDescriptor section : preload.getSections()) {
                Element classPopEl = doc.createElement(XML_E_CLASSPOPULATION);
                classPopEl.setAttribute(XML_A_CLASSPOPULATION_NAME, section.getClassName());

                if (section.getParameters() != null && !section.getParameters().isEmpty()) {
                    classPopEl.setAttribute(XML_A_CLASSPOPULATION_POPULATION, section.getParameters().get(0).getValue());
                }
                stationEl.appendChild(classPopEl);
            }
            preloadEl.appendChild(stationEl);
            hasAny = true;
        }

        if (hasAny) {
            root.appendChild(preloadEl);
        }
    }

    // -----------------------------------------------------------------------
    // Core parameter serialization
    // -----------------------------------------------------------------------

    private void writeParameter(ParameterDescriptor p, Element parent, Document doc, boolean isSubParam) {
        if (p.getRefClass() != null) {
            Element refClassEl = doc.createElement(XML_E_PARAMETER_REFCLASS);
            refClassEl.appendChild(doc.createTextNode(p.getRefClass()));
            parent.appendChild(refClassEl);
        }

        Element el = doc.createElement(isSubParam ? XML_E_SUBPARAMETER : XML_E_PARAMETER);

        if (p.getClassPath() != null) {
            el.setAttribute(XML_A_PARAMETER_CLASSPATH, p.getClassPath());
        }
        if (p.getName() != null) {
            el.setAttribute(XML_A_PARAMETER_NAME, p.getName());
        }

        boolean hasSubParameters = p.getSubParameters() != null && !p.getSubParameters().isEmpty();
        boolean isLeafWithValue = p.getValue() != null && !hasSubParameters;
        boolean hasRefClassGroupedChildren = hasSubParameters
                && p.getSubParameters().stream().anyMatch(sp -> sp.getRefClass() != null && !sp.getRefClass().isEmpty());

        // Legacy-compat:
        // - leaf nodes with <value> are scalar (no array attr)
        // - group-by-refClass parameters must be array for SimLoader
        if (!isLeafWithValue && (p.isArray() || hasRefClassGroupedChildren)) {
            el.setAttribute(XML_A_PARAMETER_ARRAY, VAL_TRUE_STRING);
        }

        // Legacy-compat: map parameters (e.g., advancedFeatures) must carry map="true"
        // so SimLoader handles them as map containers instead of constructor arguments.
        if (p.isMap()) {
            el.setAttribute(XML_A_PARAMETER_MAP, VAL_TRUE_STRING);
        }

        if (p.getValue() != null) {
            Element valueEl = doc.createElement(XML_E_PARAMETER_VALUE);
            valueEl.appendChild(doc.createTextNode(p.getValue()));
            el.appendChild(valueEl);
        }

        for (ParameterDescriptor sub : p.getSubParameters()) {
            writeParameter(sub, el, doc, true);
        }

        parent.appendChild(el);
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private Document createDocument() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to create XML Document", e);
        }
    }

    private void setAttrIfNotNull(Element el, String attr, String value) {
        if (value != null && !VAL_NULL_STRING.equals(value)) {
            el.setAttribute(attr, value);
        }
    }
}