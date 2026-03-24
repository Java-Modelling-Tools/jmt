package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.Map;

public class DelaySectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {

        ParameterDescriptor serviceStrategyParam = getParam(sec, PARAM_SERVICE_STRATEGY);
        if (serviceStrategyParam == null) {
            return;
        }

        Map<String, ParameterDescriptor> strategyMap = getRefClassMap(serviceStrategyParam);

        for (Map.Entry<String, ParameterDescriptor> entry : strategyMap.entrySet()) {
            String className = entry.getKey();
            ParameterDescriptor subParam = entry.getValue();

            Object classKey = ctx.classes.get(className);
            if (classKey != null) {
                Object parsedDistribution = parseServiceStrategy(subParam, ctx);
                model.setServiceTimeDistribution(key, classKey, parsedDistribution);
            }
        }
    }
}