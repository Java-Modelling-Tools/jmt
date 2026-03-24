package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.Map;

public class SourceSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {

        ParameterDescriptor serviceStrategyParam = getParam(sec, PARAM_SERVICE_STRATEGY);
        if (serviceStrategyParam == null) {
            return;
        }

        Map<String, ParameterDescriptor> distributions = getRefClassMap(serviceStrategyParam);
        String stationName = node.getName();

        for (Map.Entry<String, ParameterDescriptor> entry : distributions.entrySet()) {
            String className = entry.getKey();
            ParameterDescriptor strategyParam = entry.getValue();

            Object classKey = ctx.classes.get(className);

            if (classKey != null) {
                if (stationName.equals(ctx.refStations.get(classKey)) &&
                        model.getClassType(classKey) == CommonConstants.CLASS_TYPE_OPEN) {

                    Object distributionStrategy = parseServiceStrategy(strategyParam, ctx);

                    model.setClassDistribution(classKey, distributionStrategy);
                    model.setClassRefStation(classKey, key);

                    ctx.refStations.remove(classKey);
                }
            }
        }
    }
}