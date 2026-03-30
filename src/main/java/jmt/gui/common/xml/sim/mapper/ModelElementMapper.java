/**
 * This interface defines the contract for a mapper that transforms a part of the {@link SimDescriptor} DTO into
 * the {@link jmt.gui.common.definitions.CommonModel}. Each implementation is responsible for a specific part of the model,
 * such as stations, classes, or connections.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.dto.SimDescriptor;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ModelElementMapper extends XMLConstantNames {
    /**
     * Maps a part of the data from the {@link SimDescriptor} to the {@link jmt.gui.common.definitions.CommonModel}.
     *
     * @param dto     The root DTO object containing all data parsed from the XML
     * @param context The shared context containing the CommonModel and support maps
     */
    void mapDtoToCommonModel(SimDescriptor dto, MappingContext context);

    /**
     * Maps the internal {@link jmt.gui.common.definitions.CommonModel} back to the {@link SimDescriptor} DTO.
     * This is the inverse operation of {@link #mapDtoToCommonModel(SimDescriptor, MappingContext)}.
     *
     * @param dto     The root DTO object that will be populated with data from the model
     * @param context The shared context used during the WRITING phase
     */
    void mapCommonModelToDto(SimDescriptor dto, WritingContext context); // Usiamo WritingContext qui

    default boolean isPresent(String value) {
        return value != null && !value.isEmpty();
    }

    default Integer getInt(String value, Integer defaultValue) {
        return isPresent(value) ? Integer.valueOf(value) : defaultValue;
    }

    default Double getDouble(String value, Double defaultValue) {
        return isPresent(value) ? Double.valueOf(value) : defaultValue;
    }

    default Long getLong(String value, Long defaultValue) {
        return isPresent(value) ? Long.valueOf(value) : defaultValue;
    }

    default Boolean getBoolean(String value, Boolean defaultValue) {
        return isPresent(value) ? Boolean.valueOf(value) : defaultValue;
    }

    default Object resolveStationOrRegion(String name, MappingContext context) {
        Object station = context.stations.get(name);
        return (station != null) ? station : context.regions.get(name);
    }

    default ParameterDescriptor getParam(SectionDescriptor sec, String name) {
        if (sec == null || sec.getParameters() == null) return null;
        return sec.getParameters().stream().filter(p -> name.equals(p.getName())).findFirst().orElse(null);
    }

    default List<ParameterDescriptor> getSubParams(ParameterDescriptor p) {
        return (p != null && p.getSubParameters() != null) ? p.getSubParameters() : Collections.emptyList();
    }

    default Map<String, ParameterDescriptor> getRefClassMap(ParameterDescriptor p) {
        Map<String, ParameterDescriptor> map = new LinkedHashMap<>();
        if (p == null || p.getSubParameters() == null) return map;
        for (ParameterDescriptor sub : p.getSubParameters()) {
            String refClass = sub.getRefClass();
            if (refClass != null && !refClass.isEmpty()) {
                map.put(refClass, sub);
            }
        }
        return map;
    }

    default ParameterDescriptor getFirst(List<ParameterDescriptor> list) {
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    default ParameterDescriptor getAt(List<ParameterDescriptor> list, int index) {
        return (list != null && index >= 0 && index < list.size()) ? list.get(index) : null;
    }

    default String val(ParameterDescriptor p) {
        if (p == null) return null;
        if (p.getValue() != null && !p.getValue().trim().isEmpty()) return p.getValue().trim();
        for (ParameterDescriptor sub : getSubParams(p)) {
            if ("value".equals(sub.getName()) && sub.getValue() != null) return sub.getValue().trim();
        }
        return null;
    }

    default Double valD(ParameterDescriptor p) {
        String v = val(p);
        return v != null ? Double.valueOf(v) : null;
    }

    default Integer valI(ParameterDescriptor p) {
        String v = val(p);
        return v != null ? Double.valueOf(v).intValue() : null;
    }

    default ParameterDescriptor makeParam(String name, String classPath, String value) {
        ParameterDescriptor p = new ParameterDescriptor();
        p.setName(name);
        p.setClassPath(classPath);
        p.setValue(value);
        return p;
    }

    default ParameterDescriptor makeContainer(String name, String classPath) {
        ParameterDescriptor p = new ParameterDescriptor();
        p.setName(name);
        p.setClassPath(classPath);
        return p;
    }

    default ParameterDescriptor makeRefParam(String name, String classPath, String refClass, String value) {
        ParameterDescriptor p = makeParam(name, classPath, value);
        p.setRefClass(refClass);
        return p;
    }

    /**
     * Helper method to retrieve the string name of a node, checking both stations and blocking regions.
     */
    default String getNodeName(CommonModel model, Object nodeKey) {
        String name = model.getStationName(nodeKey);
        if (name == null) {
            name = model.getRegionName(nodeKey);
        }
        return name;
    }
}