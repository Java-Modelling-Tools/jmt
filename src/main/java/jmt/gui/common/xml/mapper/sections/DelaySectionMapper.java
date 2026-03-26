package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

public class DelaySectionMapper extends AbstractSectionMapper {

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
}