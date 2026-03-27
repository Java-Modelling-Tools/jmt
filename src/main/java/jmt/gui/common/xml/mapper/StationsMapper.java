/**
 * This class is responsible for mapping {@link NodeDescriptor} objects representing stations from the DTO to the {@link CommonModel}
 * and vice-versa. It determines the station type based on its section signature and then uses specialized section mappers
 * to configure the station or extract its data.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.mapper.sections.*;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.*;
import java.util.stream.Collectors;

public class StationsMapper implements ModelElementMapper {

    private static final Map<List<String>, String> STATION_SIGNATURES = new HashMap<>();
    private static final Map<String, List<String>> TYPE_TO_SIGNATURE = new HashMap<>(); // Inverse map for writing
    private static final Map<String, AbstractSectionMapper> SECTION_MAPPERS = new HashMap<>();

    public StationsMapper() {
        initSignatures();
        initSectionMappers();
    }

    private void initSignatures() {
        // --- READING SIGNATURES ---
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_SOURCE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_SOURCE);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_TERMINAL, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_TERMINAL);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_ROUTER);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_DELAY, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_DELAY);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_FORK), CommonConstants.STATION_TYPE_FORK);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_JOIN, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_JOIN);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_LOGGER, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_LOGGER);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_CLASSSWITCH, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_CLASSSWITCH);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_SEMAPHORE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_SEMAPHORE);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_JOIN, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_FORK), CommonConstants.STATION_TYPE_SCALER);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_STORAGE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_LINKAGE), CommonConstants.STATION_TYPE_PLACE);
        STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_ENABLING, XMLConstantNames.CLASSNAME_TIMING, XMLConstantNames.CLASSNAME_FIRING), CommonConstants.STATION_TYPE_TRANSITION);

        List<String> serverMiddleSections = Arrays.asList(
                XMLConstantNames.CLASSNAME_SERVER, XMLConstantNames.CLASSNAME_PREEMPTIVESERVER, XMLConstantNames.CLASSNAME_PSSERVER,
                XMLConstantNames.CLASSNAME_PSSERVER_PRIORITY, XMLConstantNames.CLASSNAME_EXHAUSTIVEPSERVER, XMLConstantNames.CLASSNAME_LKPSERVER, XMLConstantNames.CLASSNAME_GATEDPSERVER
        );
        for (String middle : serverMiddleSections) {
            STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, middle, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_SERVER);
        }

        // --- WRITING SIGNATURES (Inverse Map) ---
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_SOURCE, Arrays.asList(XMLConstantNames.CLASSNAME_SOURCE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_TERMINAL, Arrays.asList(XMLConstantNames.CLASSNAME_TERMINAL, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_ROUTER, Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_DELAY, Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_DELAY, XMLConstantNames.CLASSNAME_ROUTER));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_FORK, Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_FORK));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_JOIN, Arrays.asList(XMLConstantNames.CLASSNAME_JOIN, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_LOGGER, Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_LOGGER, XMLConstantNames.CLASSNAME_ROUTER));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_CLASSSWITCH, Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_CLASSSWITCH, XMLConstantNames.CLASSNAME_ROUTER));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_SEMAPHORE, Arrays.asList(XMLConstantNames.CLASSNAME_SEMAPHORE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_ROUTER));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_SCALER, Arrays.asList(XMLConstantNames.CLASSNAME_JOIN, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_FORK));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_PLACE, Arrays.asList(XMLConstantNames.CLASSNAME_STORAGE, XMLConstantNames.CLASSNAME_TUNNEL, XMLConstantNames.CLASSNAME_LINKAGE));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_TRANSITION, Arrays.asList(XMLConstantNames.CLASSNAME_ENABLING, XMLConstantNames.CLASSNAME_TIMING, XMLConstantNames.CLASSNAME_FIRING));
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_SINK, Collections.singletonList(XMLConstantNames.CLASSNAME_SINK));

        // For servers, we map the default base signature. The ServerSectionMapper will override
        // the middle section className (e.g. to PreemptiveServer) based on the model strategy during writing.
        TYPE_TO_SIGNATURE.put(CommonConstants.STATION_TYPE_SERVER, Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, XMLConstantNames.CLASSNAME_SERVER, XMLConstantNames.CLASSNAME_ROUTER));
    }

    private void initSectionMappers() {
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_SOURCE, new SourceSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_ROUTER, new RouterSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_QUEUE, new QueueSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_DELAY, new DelaySectionMapper());

        AbstractSectionMapper serverMapper = new ServerSectionMapper();
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_SERVER, serverMapper);
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_PREEMPTIVESERVER, serverMapper);
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_PSSERVER, serverMapper);
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_PSSERVER_PRIORITY, serverMapper);
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_EXHAUSTIVEPSERVER, serverMapper);
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_LKPSERVER, serverMapper);
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_GATEDPSERVER, serverMapper);

        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_FORK, new ForkSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_JOIN, new JoinSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_LOGGER, new LoggerSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_CLASSSWITCH, new ClassSwitchSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_SEMAPHORE, new SemaphoreSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_STORAGE, new StorageSectionMapper());

        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_ENABLING, new EnablingSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_TIMING, new TimingSectionMapper());
        SECTION_MAPPERS.put(XMLConstantNames.CLASSNAME_FIRING, new FiringSectionMapper());
    }

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
        if (dto.getNodes() == null || dto.getNodes().isEmpty()) return;

        for (NodeDescriptor node : dto.getNodes()) {
            mapStation(node, context);
        }
    }

    private void mapStation(NodeDescriptor node, MappingContext context) {
        CommonModel model = context.getModel();
        String type = getStationType(node);
        String name = node.getName();

        Object stationKey = model.addStation(name, type, 1, new ArrayList<>());
        context.stations.put(name, stationKey);

        mapSections(node, model, stationKey, type, context);
        mapSoftDeadlines(node, model, stationKey, type);
        mapQuantumParameters(node, model, stationKey, type);
    }

    private void mapSections(NodeDescriptor node, CommonModel model, Object stationKey, String type, MappingContext context) {
        List<SectionDescriptor> sections = node.getSections();
        if (sections != null) {
            for (SectionDescriptor section : sections) {
                AbstractSectionMapper mapper = SECTION_MAPPERS.get(section.getClassName());
                if (mapper != null) {
                    mapper.mapDtoToCommonModel(section, model, stationKey, node, type, context);
                }
            }
        }
    }

    private void mapSoftDeadlines(NodeDescriptor node, CommonModel model, Object stationKey, String type) {
        if (CommonConstants.STATION_TYPE_DELAY.equals(type) || CommonConstants.STATION_TYPE_SERVER.equals(type)) {
            List<Double> deadlines = node.getClassSoftDeadlines();
            if (deadlines != null && !deadlines.isEmpty()) {
                List<Object> classKeys = model.getClassKeys();
                for (int i = 0; i < deadlines.size() && i < classKeys.size(); i++) {
                    model.setClassStationSoftDeadline(stationKey, classKeys.get(i), deadlines.get(i));
                }
            }
        }
    }

    private void mapQuantumParameters(NodeDescriptor node, CommonModel model, Object stationKey, String type) {
        if (CommonConstants.STATION_TYPE_SERVER.equals(type)) {
            if (node.getQuantumSize() != null && !node.getQuantumSize().isEmpty()) {
                model.setQuantumSize(stationKey, node.getQuantumSize().get(0));
            }
            if (node.getQuantumSwitchoverTime() != null && !node.getQuantumSwitchoverTime().isEmpty()) {
                model.setQuantumSwitchoverTime(stationKey, node.getQuantumSwitchoverTime().get(0));
            }
        }
    }

    private String getStationType(NodeDescriptor node) {
        List<SectionDescriptor> sections = node.getSections();
        if (sections == null || sections.isEmpty()) return "UNKNOWN";
        if (XMLConstantNames.CLASSNAME_SINK.equals(sections.get(0).getClassName())) return CommonConstants.STATION_TYPE_SINK;

        List<String> signature = sections.stream()
                .map(SectionDescriptor::getClassName)
                .limit(3)
                .collect(Collectors.toList());

        return STATION_SIGNATURES.getOrDefault(signature, "UNKNOWN");
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model;
        Vector<Object> stationKeys = model.getStationKeys();

        if (stationKeys == null || stationKeys.isEmpty()) {
            return;
        }

        for (Object stationKey : stationKeys) {
            NodeDescriptor node = new NodeDescriptor();
            String name = model.getStationName(stationKey);
            String type = model.getStationType(stationKey);

            node.setName(name);

            // 1. Generate SectionDescriptors based on the station type signature
            generateSectionsForTypeToDto(node, model, stationKey, type, context);

            // 2. Map Node-level specific properties
            mapModelSoftDeadlinesToDto(node, model, stationKey, type);
            mapModelQuantumParametersToDto(node, model, stationKey, type);

            dto.addNode(node);
        }
    }

    private void generateSectionsForTypeToDto(NodeDescriptor node, CommonModel model, Object stationKey, String type, WritingContext context) {
        List<String> signature = TYPE_TO_SIGNATURE.get(type);

        if (signature != null) {
            for (String sectionClassName : signature) {
                SectionDescriptor section = new SectionDescriptor();
                section.setClassName(sectionClassName);
                node.addSection(section);

                // Delegate to the specific Section Mapper to write its parameters
                AbstractSectionMapper mapper = SECTION_MAPPERS.get(sectionClassName);
                if (mapper != null) {
                    // Note: Ensure AbstractSectionMapper defines mapCommonModelToDto(...)
                    mapper.mapCommonModelToDto(section, model, stationKey, node, type, context);
                }
            }
        }
    }

    private void mapModelSoftDeadlinesToDto(NodeDescriptor node, CommonModel model, Object stationKey, String type) {
        if (CommonConstants.STATION_TYPE_DELAY.equals(type) || CommonConstants.STATION_TYPE_SERVER.equals(type)) {
            Vector<Object> classKeys = model.getClassKeys();
            if (classKeys != null) {
                List<Double> deadlines = new ArrayList<>();
                boolean hasNonZeroDeadline = false;

                for (Object classKey : classKeys) {
                    Double deadline = model.getClassStationSoftDeadline(stationKey, classKey);
                    if (deadline == null) {
                        deadline = 0.0;
                    }
                    if (deadline > 0.0) {
                        hasNonZeroDeadline = true;
                    }
                    deadlines.add(deadline);
                }

                // Append the list to the node only if at least one deadline is actually set
                if (hasNonZeroDeadline) {
                    node.setClassSoftDeadlines(deadlines);
                }
            }
        }
    }

    private void mapModelQuantumParametersToDto(NodeDescriptor node, CommonModel model, Object stationKey, String type) {
        if (CommonConstants.STATION_TYPE_SERVER.equals(type)) {
            Double quantumSize = model.getQuantumSize(stationKey);
            if (quantumSize != null) {
                node.addQuantumSize(quantumSize);
            }

            Double quantumSwitchoverTime = model.getQuantumSwitchoverTime(stationKey);
            if (quantumSwitchoverTime != null) {
                node.addQuantumSwitchoverTime(quantumSwitchoverTime);
            }
        }
    }
}