package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

public class PreloadMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        // If there is no preload section, we exit immediately
        if (dto.getPreloads() == null || dto.getPreloads().isEmpty()) {
            return;
        }

        CommonModel model = context.getModel();

        // Iterate over each "stationPopulations" block mapped as NodeDescriptor by the Parser
        for (NodeDescriptor stationPopNode : dto.getPreloads()) {

            String stationName = stationPopNode.getName();
            Object stationKey = context.stations.get(stationName);

            // Also search among the regions if the station is not found, as the old XMLReader did
            if (stationKey == null) {
                stationKey = context.regions.get(stationName);
            }

            if (stationKey != null && stationPopNode.getSections() != null) {

                // The SimParser maps the "classPopulation" as SectionDescriptor
                for (SectionDescriptor classPopSection : stationPopNode.getSections()) {

                    // The SimParser saved the class name in the className attribute of the section
                    String className = classPopSection.getClassName();
                    Object classKey = context.classes.get(className);

                    if (classKey != null && classPopSection.getParameters() != null && !classPopSection.getParameters().isEmpty()) {

                        // The population is saved as "value" in the first (and only) ParameterDescriptor of the section
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