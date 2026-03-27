/**
 * This class is responsible for mapping the preload information from the {@link SimDescriptor} to the {@link CommonModel}
 * and vice-versa. It sets the initial number of jobs in each station for each class, or extracts this information
 * from the model to build the DTO.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.Vector;

public class PreloadMapper implements ModelElementMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
        if (dto.getPreloads() == null || dto.getPreloads().isEmpty()) {
            return;
        }

        for (NodeDescriptor stationPopNode : dto.getPreloads()) {
            mapStationPreload(stationPopNode, context);
        }
    }

    private void mapStationPreload(NodeDescriptor stationPopNode, MappingContext context) {
        String stationName = stationPopNode.getName();
        Object stationKey = resolveStationOrRegion(stationName, context);

        if (stationKey != null && stationPopNode.getSections() != null) {
            for (SectionDescriptor classPopSection : stationPopNode.getSections()) {
                mapClassPreload(classPopSection, stationKey, context);
            }
        }
    }

    private void mapClassPreload(SectionDescriptor classPopSection, Object stationKey, MappingContext context) {
        CommonModel model = context.getModel();
        String className = classPopSection.getClassName();
        Object classKey = context.classes.get(className);

        if (classKey != null && classPopSection.getParameters() != null && !classPopSection.getParameters().isEmpty()) {
            ParameterDescriptor popParam = classPopSection.getParameters().get(0);
            String popStr = popParam.getValue();

            if (isPresent(popStr)) {
                Integer preloadCount = Integer.valueOf(popStr.trim());
                model.setPreloadedJobs(stationKey, classKey, preloadCount);
            }
        }
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model;
        Vector<Object> preloadableStations = model.getStationKeysPreloadable();

        if (preloadableStations == null || preloadableStations.isEmpty()) {
            return;
        }

        for (Object stationKey : preloadableStations) {
            mapModelPreloadsToDto(model, stationKey, dto);
        }
    }

    private void mapModelPreloadsToDto(CommonModel model, Object stationKey, SimDescriptor dto) {
        String stationName = model.getStationName(stationKey);
        if (stationName == null) return;

        NodeDescriptor stationPopNode = null;
        Vector<Object> classKeys = model.getClassKeys();

        if (classKeys != null) {
            for (Object classKey : classKeys) {
                Integer preloadCount = model.getPreloadedJobs(stationKey, classKey);

                // Only write the preload section if there are actually preloaded jobs
                if (preloadCount != null && preloadCount > 0) {

                    // Lazy initialization of the NodeDescriptor (created only if at least one class has preloads)
                    if (stationPopNode == null) {
                        stationPopNode = new NodeDescriptor();
                        stationPopNode.setName(stationName);
                    }

                    SectionDescriptor classPopSection = new SectionDescriptor();
                    classPopSection.setClassName(model.getClassName(classKey));

                    ParameterDescriptor popParam = new ParameterDescriptor();
                    popParam.setClassPath("java.lang.Integer");
                    popParam.setValue(preloadCount.toString());

                    classPopSection.addParameter(popParam);
                    stationPopNode.addSection(classPopSection);
                }
            }
        }

        // Add the station to the DTO preloads list only if it has at least one class preloaded
        if (stationPopNode != null) {
            dto.addPreload(stationPopNode);
        }
    }
}