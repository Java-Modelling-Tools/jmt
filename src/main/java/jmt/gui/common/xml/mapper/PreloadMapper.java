/**
 * This class is responsible for mapping the preload information from the {@link SimDescriptor} to the {@link CommonModel}.
 * It sets the initial number of jobs in each station for each class.
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

public class PreloadMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
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
}