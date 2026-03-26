/**
 * This interface defines the contract for a mapper that transforms a part of the {@link SimDescriptor} DTO into
 * the {@link jmt.gui.common.definitions.CommonModel}. Each implementation is responsible for a specific part of the model,
 * such as stations, classes, or connections.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.gui.common.xml.dto.SimDescriptor;

public interface ModelElementMapper {
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
     * @param context The shared context containing the CommonModel and support maps
     */
    void mapCommonModelToDto(SimDescriptor dto, MappingContext context);

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