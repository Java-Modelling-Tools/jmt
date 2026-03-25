package jmt.gui.common.xml.mapper;

import jmt.gui.common.xml.dto.SimDescriptor;

public interface ModelElementMapper {
    /**
     * @param dto The root DTO object containing all data parsed from the XML
     * @param context The shared context containing the CommonModel and support maps
     */
    void map(SimDescriptor dto, MappingContext context);

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
}