package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

public class SourceSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor serviceStrategyParam = getParam(sec, PARAM_SERVICE_STRATEGY);
        if (serviceStrategyParam == null) return;

        getRefClassMap(serviceStrategyParam).forEach((className, p) -> {
            Object classKey = ctx.classes.get(className);
            debug("Source %s -> Class: %s, ServiceStrategy: %s", node.getName(), className, p.getClassPath());

            if (classKey != null && node.getName().equals(ctx.refStations.get(classKey)) && model.getClassType(classKey) == CommonConstants.CLASS_TYPE_OPEN) {
                Object distributionStrategy = parseServiceStrategy(p, ctx);
                model.setClassDistribution(classKey, distributionStrategy);
                model.setClassRefStation(classKey, key);
                ctx.refStations.remove(classKey);
            }
        });
    }
}