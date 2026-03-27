package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.serializer.WritingContext;

/**
 * This class is responsible for mapping the Storage section of a node.
 * It configures capacity limits, class-specific constraints, and drop rules (e.g., for Places).
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
public class StorageSectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        mapTotalCapacity(sec, model, key);
        mapClassCapacities(sec, model, key, ctx);
        mapDropRules(sec, model, key, ctx);
        mapPutStrategies(sec, model, key, ctx);
    }

    private void mapTotalCapacity(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor tc = getParam(sec, XMLConstantNames.PARAM_TOTAL_CAPACITY);
        if (tc != null) {
            Integer tVal = valI(tc);
            if (tVal != null) model.setStationQueueCapacity(key, tVal);
        }
    }

    private void mapClassCapacities(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
        ParameterDescriptor caps = getParam(sec, XMLConstantNames.PARAM_CAPACITIES);
        if (caps != null) {
            getRefClassMap(caps).forEach((c, subP) -> {
                Integer cVal = valI(subP);
                if (cVal != null) model.setQueueCapacity(key, ctx.classes.get(c), cVal);
            });
        }
    }

    private void mapDropRules(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
        ParameterDescriptor drops = getParam(sec, XMLConstantNames.PARAM_DROP_RULES);
        if (drops != null) {
            getRefClassMap(drops).forEach((c, subP) -> {
                String dVal = val(subP);
                if (dVal != null) model.setDropRule(key, ctx.classes.get(c), Maps.DROP_RULES.get(dVal));
            });
        }
    }

    private void mapPutStrategies(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
        ParameterDescriptor puts = getParam(sec, XMLConstantNames.PARAM_PUT_STRATEGIES);
        if (puts != null) {
            getRefClassMap(puts).forEach((c, subP) -> {
                String cp = subP.getClassPath();
                if (cp != null) {
                    String ps = cp.substring(cp.lastIndexOf(".") + 1);
                    if (XMLConstantNames.STRAT_TAIL_STRATEGY.equals(ps)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_FCFS);
                    else if (XMLConstantNames.STRAT_HEAD_STRATEGY.equals(ps)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_LCFS);
                    else if (XMLConstantNames.STRAT_RAND_STRATEGY.equals(ps)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_RAND);
                }
            });
        }
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext ctx) {
        writeTotalCapacity(sec, model, key);
        writeClassCapacities(sec, model, key);
        writeDropRules(sec, model, key);
        writePutStrategies(sec, model, key);
    }

    private void writeTotalCapacity(SectionDescriptor sec, CommonModel model, Object key) {
        Integer totalCapacity = model.getStationQueueCapacity(key);
        if (totalCapacity != null) {
            sec.addParameter(makeParam(XMLConstantNames.PARAM_TOTAL_CAPACITY, Integer.class.getName(), totalCapacity.toString()));
        }
    }

    private void writeClassCapacities(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor caps = makeContainer(XMLConstantNames.PARAM_CAPACITIES, Integer.class.getName());
        caps.setArray(true);

        for (Object classKey : model.getClassKeys()) {
            Integer classCap = model.getQueueCapacity(key, classKey);
            if (classCap != null && classCap != -1) { // -1 indicates infinite/not set
                caps.addSubParameter(makeRefParam("capacity", Integer.class.getName(), model.getClassName(classKey), classCap.toString()));
            }
        }

        if (!caps.getSubParameters().isEmpty()) {
            sec.addParameter(caps);
        }
    }

    private void writeDropRules(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor drops = makeContainer(XMLConstantNames.PARAM_DROP_RULES, String.class.getName());
        drops.setArray(true);

        for (Object classKey : model.getClassKeys()) {
            String dropRule = model.getDropRule(key, classKey);
            if (dropRule != null) {
                String xmlDropRule = Maps.DROP_RULES_REVERSE.get(dropRule);
                if (xmlDropRule != null) {
                    drops.addSubParameter(makeRefParam("dropRule", String.class.getName(), model.getClassName(classKey), xmlDropRule));
                }
            }
        }

        if (!drops.getSubParameters().isEmpty()) {
            sec.addParameter(drops);
        }
    }

    private void writePutStrategies(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor puts = makeContainer(XMLConstantNames.PARAM_PUT_STRATEGIES, "jmt.engine.NetStrategies.QueuePutStrategy");
        puts.setArray(true);

        for (Object classKey : model.getClassKeys()) {
            String queueStrategy = model.getQueueStrategy(key, classKey);
            String strategyName = getPutStrategyName(queueStrategy);

            if (strategyName != null) {
                puts.addSubParameter(makeRefParam(strategyName, "jmt.engine.NetStrategies.QueuePutStrategies." + strategyName, model.getClassName(classKey), null));
            }
        }

        if (!puts.getSubParameters().isEmpty()) {
            sec.addParameter(puts);
        }
    }

    private String getPutStrategyName(String queueStrategy) {
        if (CommonConstants.QUEUE_STRATEGY_FCFS.equals(queueStrategy)) return XMLConstantNames.STRAT_TAIL_STRATEGY;
        if (CommonConstants.QUEUE_STRATEGY_LCFS.equals(queueStrategy)) return XMLConstantNames.STRAT_HEAD_STRATEGY;
        if (CommonConstants.QUEUE_STRATEGY_RAND.equals(queueStrategy)) return XMLConstantNames.STRAT_RAND_STRATEGY;
        return null;
    }
}