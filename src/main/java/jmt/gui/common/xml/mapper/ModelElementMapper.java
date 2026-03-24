package jmt.gui.common.xml.mapper;

import jmt.gui.common.xml.dto.SimDescriptor;

public interface ModelElementMapper {
    /**
     * @param dto The root DTO object containing all data parsed from the XML
     * @param context The shared context containing the CommonModel and support maps
     */
    void map(SimDescriptor dto, MappingContext context);
}