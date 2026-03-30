/**
 * This class is responsible for mapping the Delay section of a node.
 * It handles the parsing and writing of the service strategies and transition conditions.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

public class DelaySectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor serviceStrategyParam = getParam(sec, "ServiceStrategy");
        if (serviceStrategyParam != null) {
            getRefClassMap(serviceStrategyParam).forEach((className, subParam) -> {
                Object classKey = ctx.classes.get(className);
                if (classKey != null) {
                    Object parsedDistribution = parseServiceStrategy(subParam, ctx);
                    model.setServiceTimeDistribution(key, classKey, parsedDistribution);
                }
            });
        }

        extractTransitionConditions(key, getParam(sec, "enablingConditions"), getParam(sec, "inhibitingConditions"), getParam(sec, "resourceConditions"), ctx);
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        addServiceStrategyParameter(sec, model, key, context);
        mapTransitionConditions(key, sec, model);
    }
}