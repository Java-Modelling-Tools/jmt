package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.serializer.WritingContext;

public class SourceSectionMapper extends AbstractSectionMapper {

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor serviceStrategyParam = getParam(sec, XMLConstantNames.PARAM_SERVICE_STRATEGY);
        if (serviceStrategyParam == null) return;

        getRefClassMap(serviceStrategyParam).forEach((className, p) -> {
            Object classKey = ctx.classes.get(className);

            if (classKey != null && node.getName().equals(ctx.refStations.get(classKey)) && model.getClassType(classKey) == CommonConstants.CLASS_TYPE_OPEN) {
                Object distributionStrategy = parseServiceStrategy(p, ctx);
                model.setClassDistribution(classKey, distributionStrategy);
                model.setClassRefStation(classKey, key);
                ctx.refStations.remove(classKey);
            }
        });
    }

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext ctx) {
        ParameterDescriptor serviceStrategy = new ParameterDescriptor();
        serviceStrategy.setName(XMLConstantNames.PARAM_SERVICE_STRATEGY);
        serviceStrategy.setClassPath("jmt.engine.NetStrategies.ServiceStrategy");
        serviceStrategy.setArray(true);

        for (Object classKey : model.getClassKeys()) {
            if (model.getClassType(classKey) == CommonConstants.CLASS_TYPE_OPEN
                    && key.equals(model.getClassRefStation(classKey))) {

                Object distribution = model.getClassDistribution(classKey);
                if (distribution != null) {
                    ParameterDescriptor stStrategy = getDistributionParameter(
                            distribution, model, classKey, "ServiceTimeStrategy",
                            "jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy", ctx);
                    serviceStrategy.addSubParameter(stStrategy);
                }
            }
        }

        if (!serviceStrategy.getSubParameters().isEmpty()) {
            sec.addParameter(serviceStrategy);
        }
    }
}