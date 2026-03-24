package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

public class PreloadMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        // Se non c'è la sezione preload, usciamo subito
        if (dto.getPreloads() == null || dto.getPreloads().isEmpty()) {
            return;
        }

        CommonModel model = context.getModel();

        // Itera su ogni blocco "stationPopulations" mappato come NodeDescriptor dal Parser
        for (NodeDescriptor stationPopNode : dto.getPreloads()) {

            String stationName = stationPopNode.getName();
            Object stationKey = context.stations.get(stationName);

            // Cerca anche tra le regioni se non trova la stazione, come faceva il vecchio XMLReader
            if (stationKey == null) {
                stationKey = context.regions.get(stationName);
            }

            if (stationKey != null && stationPopNode.getSections() != null) {

                // Il SimParser mappa le "classPopulation" come SectionDescriptor
                for (SectionDescriptor classPopSection : stationPopNode.getSections()) {

                    // Il SimParser ha salvato il nome della classe nell'attributo className della sezione
                    String className = classPopSection.getClassName();
                    Object classKey = context.classes.get(className);

                    if (classKey != null && classPopSection.getParameters() != null && !classPopSection.getParameters().isEmpty()) {

                        // La popolazione è salvata come "value" nel primo (e unico) ParameterDescriptor della sezione
                        ParameterDescriptor popParam = classPopSection.getParameters().get(0);
                        String popStr = popParam.getValue();

                        if (popStr != null && !popStr.trim().isEmpty()) {
                            Integer preloadCount = Integer.valueOf(popStr.trim());
                            model.setPreloadedJobs(stationKey, classKey, preloadCount);
                        }
                    }
                }
            }
        }
    }
}