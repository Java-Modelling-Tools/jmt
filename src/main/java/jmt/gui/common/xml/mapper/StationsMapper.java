package jmt.gui.common.xml.mapper;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.mapper.sections.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StationsMapper implements ModelElementMapper {

    private static final Map<List<String>, String> STATION_SIGNATURES = new HashMap<>();

    // Il nostro Registry delle classi che estendono AbstractSectionMapper
    private final Map<String, AbstractSectionMapper> SECTION_MAPPERS = new HashMap<>();

    public StationsMapper() {
        initSignatures();
        initSectionMappers();
    }

    private void initSignatures() {
        //
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
                XMLConstantNames.CLASSNAME_SERVER,
                XMLConstantNames.CLASSNAME_PREEMPTIVESERVER,
                XMLConstantNames.CLASSNAME_PSSERVER,
                XMLConstantNames.CLASSNAME_PSSERVER_PRIORITY,
                XMLConstantNames.CLASSNAME_EXHAUSTIVEPSERVER,
                XMLConstantNames.CLASSNAME_LKPSERVER,
                XMLConstantNames.CLASSNAME_GATEDPSERVER
        );
        for (String middle : serverMiddleSections) {
            STATION_SIGNATURES.put(Arrays.asList(XMLConstantNames.CLASSNAME_QUEUE, middle, XMLConstantNames.CLASSNAME_ROUTER), CommonConstants.STATION_TYPE_SERVER);
        }
    }

    private void initSectionMappers() {
        //
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

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        if (dto.getNodes() == null || dto.getNodes().isEmpty()) return;

        CommonModel model = context.getModel();

        for (NodeDescriptor node : dto.getNodes()) {
            String type = getStationType(node);
            String name = node.getName();

            Object stationKey = model.addStation(name, type, 1, new ArrayList<ServerType>());
            context.stations.put(name, stationKey);

            List<SectionDescriptor> sections = node.getSections();
            if (sections != null && !sections.isEmpty()) {
                for (SectionDescriptor section : sections) {
                    AbstractSectionMapper mapper = SECTION_MAPPERS.get(section.getClassName());
                    if (mapper != null) {
                        mapper.map(section, model, stationKey, node, type, context);
                    }
                }
            }

            // --- ATTRIBUTI A LIVELLO DI STAZIONE ---
            if (CommonConstants.STATION_TYPE_DELAY.equals(type) || CommonConstants.STATION_TYPE_SERVER.equals(type)) {
                parseClassStationSoftDeadlines(node, model, stationKey);
            }
            if (CommonConstants.STATION_TYPE_SERVER.equals(type)) {
                parseQuantaSize(node, model, stationKey);
                parseQuantumSwitchoverTime(node, model, stationKey);
            }
        }
    }

    private String getStationType(NodeDescriptor node) {
        List<SectionDescriptor> sections = node.getSections();
        if (sections == null || sections.isEmpty()) {
            return "UNKNOWN";
        }

        //
        if (XMLConstantNames.CLASSNAME_SINK.equals(sections.get(0).getClassName())) {
            return CommonConstants.STATION_TYPE_SINK;
        }

        List<String> signature = sections.stream()
                .map(SectionDescriptor::getClassName)
                .limit(3)
                .collect(Collectors.toList());

        return STATION_SIGNATURES.getOrDefault(signature, "UNKNOWN");
    }

    // --- UTILITY PER RECUPERARE PARAMETRI DAL NODO ---
    private ParameterDescriptor getNodeParameter(NodeDescriptor node, String name) {
        if (node.getParameters() == null) return null;
        return node.getParameters().stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst().orElse(null);
    }

    // --- METODI SPECIFICI PER LA STAZIONE AGGIORNATI ---

    private void parseClassStationSoftDeadlines(NodeDescriptor node, CommonModel model, Object key) {
        //
        ParameterDescriptor deadlinesParam = getNodeParameter(node, XMLConstantNames.XML_E_STATION_DUE_DATES);
        if (deadlinesParam != null && deadlinesParam.getSubParameters() != null) {
            List<Object> classKeys = model.getClassKeys();
            List<ParameterDescriptor> subParams = deadlinesParam.getSubParameters();

            for (int i = 0; i < subParams.size() && i < classKeys.size(); i++) {
                String val = subParams.get(i).getValue();
                if (val != null) {
                    model.setClassStationSoftDeadline(key, classKeys.get(i), Double.parseDouble(val));
                }
            }
        }
    }

    private void parseQuantaSize(NodeDescriptor node, CommonModel model, Object key) {
        //
        ParameterDescriptor param = getNodeParameter(node, XMLConstantNames.XML_E_STATION_QUANTUM_SIZE);
        if (param != null && param.getValue() != null) {
            model.setQuantumSize(key, Double.parseDouble(param.getValue()));
        }
    }

    private void parseQuantumSwitchoverTime(NodeDescriptor node, CommonModel model, Object key) {
        //
        ParameterDescriptor param = getNodeParameter(node, XMLConstantNames.XML_E_STATION_QUANTUM_SWITCHOVER_TIME);
        if (param != null && param.getValue() != null) {
            model.setQuantumSwitchoverTime(key, Double.parseDouble(param.getValue()));
        }
    }
}