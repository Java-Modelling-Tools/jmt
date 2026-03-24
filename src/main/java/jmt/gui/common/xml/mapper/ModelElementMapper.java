package jmt.gui.common.xml.mapper;

import jmt.gui.common.xml.dto.SimDescriptor;

public interface ModelElementMapper {
    /**
     * @param dto L'oggetto DTO radice contenente tutti i dati parsati dall'XML
     * @param context Il contesto condiviso contenente il CommonModel e le mappe di supporto
     */
    void map(SimDescriptor dto, MappingContext context);
}