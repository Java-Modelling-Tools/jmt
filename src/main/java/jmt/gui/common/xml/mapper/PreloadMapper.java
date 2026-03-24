package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

public class PreloadMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        // Se non c'è la sezione preload, usciamo subito
        if (dto.getPreloads() == null || dto.getPreloads().isEmpty()) {
            return;
        }

        CommonModel model = context.getModel();

        // Itera su ogni blocco "stationPopulations" (mappato come NodeDescriptor)
        for (NodeDescriptor stationPopNode : dto.getPreloads()) {

            String stationName = stationPopNode.getName();
            Object stationKey = context.stations.get(stationName);

            // Se la stazione esiste nel nostro contesto, procediamo
            if (stationKey != null && stationPopNode.getParameters() != null) {

                // Itera su ogni "classPopulation" all'interno della stazione
                for (ParameterDescriptor classPopParam : stationPopNode.getParameters()) {

                    String className = classPopParam.getRefClass();
                    Object classKey = context.classes.get(className);

                    if (classKey != null) {
                        // La popolazione nel vecchio XML era l'attributo "population",
                        // che il DTO generico normalmente riversa nel campo "value"
                        String popStr = classPopParam.getValue();

                        if (popStr != null && !popStr.isEmpty()) {
                            Integer preloadCount = Integer.valueOf(popStr);
                            model.setPreloadedJobs(stationKey, classKey, preloadCount);
                        }
                    }
                }
            }
        }
    }
}