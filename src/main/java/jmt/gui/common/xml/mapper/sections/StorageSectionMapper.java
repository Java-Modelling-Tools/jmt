/**
 * This class is responsible for mapping the Storage section of a node.
 * It configures capacity limits, class-specific constraints, and drop rules (e.g., for Places).
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.HashMap;
import java.util.Map;

public class StorageSectionMapper extends AbstractSectionMapper {

    private static final Map<String, String> DROP_RULES = new HashMap<>();
    private static final Map<String, String> DROP_RULES_REVERSE = new HashMap<>();

    static {
        DROP_RULES.put("drop", CommonConstants.FINITE_DROP);
        DROP_RULES.put("BAS blocking", CommonConstants.FINITE_BLOCK);
        DROP_RULES.put("waiting queue", CommonConstants.FINITE_WAITING);
        DROP_RULES.put("retrial", CommonConstants.FINITE_RETRIAL);

        DROP_RULES.forEach((xmlVal, modelVal) -> DROP_RULES_REVERSE.put(modelVal, xmlVal));
    }

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
                if (dVal != null) model.setDropRule(key, ctx.classes.get(c), DROP_RULES.get(dVal));
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
        // 1. Total Capacity
        Integer totalCapacity = model.getStationQueueCapacity(key);
        if (totalCapacity != null) {
            ParameterDescriptor tc = new ParameterDescriptor();
            tc.setName(XMLConstantNames.PARAM_TOTAL_CAPACITY);
            tc.setClassPath(Integer.class.getName());
            tc.setValue(totalCapacity.toString());
            sec.addParameter(tc);
        }

        // 2. Class Capacities
        ParameterDescriptor caps = new ParameterDescriptor();
        caps.setName(XMLConstantNames.PARAM_CAPACITIES);
        caps.setClassPath(Integer.class.getName());
        boolean hasCaps = false;

        for (Object classKey : model.getClassKeys()) {
            Integer classCap = model.getQueueCapacity(key, classKey);
            if (classCap != null && classCap != -1) { // -1 usually means infinite/not set
                ParameterDescriptor cParam = new ParameterDescriptor();
                cParam.setName("capacity");
                cParam.setClassPath(Integer.class.getName());
                cParam.setRefClass(model.getClassName(classKey));
                cParam.setValue(classCap.toString());
                caps.addSubParameter(cParam);
                hasCaps = true;
            }
        }
        if (hasCaps) sec.addParameter(caps);

        // 3. Drop Rules
        ParameterDescriptor drops = new ParameterDescriptor();
        drops.setName(XMLConstantNames.PARAM_DROP_RULES);
        drops.setClassPath(String.class.getName());
        boolean hasDrops = false;

        for (Object classKey : model.getClassKeys()) {
            String dropRule = model.getDropRule(key, classKey);
            if (dropRule != null) {
                String xmlDropRule = DROP_RULES_REVERSE.get(dropRule);
                if (xmlDropRule != null) {
                    ParameterDescriptor dParam = new ParameterDescriptor();
                    dParam.setName("dropRule");
                    dParam.setClassPath(String.class.getName());
                    dParam.setRefClass(model.getClassName(classKey));
                    dParam.setValue(xmlDropRule);
                    drops.addSubParameter(dParam);
                    hasDrops = true;
                }
            }
        }
        if (hasDrops) sec.addParameter(drops);

        // 4. Put Strategies
        ParameterDescriptor puts = new ParameterDescriptor();
        puts.setName(XMLConstantNames.PARAM_PUT_STRATEGIES);
        puts.setClassPath("jmt.engine.NetStrategies.QueuePutStrategy");
        boolean hasPuts = false;

        for (Object classKey : model.getClassKeys()) {
            String queueStrategy = model.getQueueStrategy(key, classKey);
            if (queueStrategy != null) {
                String strategyName = null;
                if (CommonConstants.QUEUE_STRATEGY_FCFS.equals(queueStrategy)) strategyName = XMLConstantNames.STRAT_TAIL_STRATEGY;
                else if (CommonConstants.QUEUE_STRATEGY_LCFS.equals(queueStrategy)) strategyName = XMLConstantNames.STRAT_HEAD_STRATEGY;
                else if (CommonConstants.QUEUE_STRATEGY_RAND.equals(queueStrategy)) strategyName = XMLConstantNames.STRAT_RAND_STRATEGY;

                if (strategyName != null) {
                    ParameterDescriptor pParam = new ParameterDescriptor();
                    pParam.setName(strategyName);
                    pParam.setClassPath("jmt.engine.NetStrategies.QueuePutStrategies." + strategyName);
                    pParam.setRefClass(model.getClassName(classKey));
                    puts.addSubParameter(pParam);
                    hasPuts = true;
                }
            }
        }
        if (hasPuts) sec.addParameter(puts);
    }
}