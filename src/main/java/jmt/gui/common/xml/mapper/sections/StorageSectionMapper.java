package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

import java.util.HashMap;
import java.util.Map;

public class StorageSectionMapper extends AbstractSectionMapper {

    private static final Map<String, String> DROP_RULES = new HashMap<>();
    static {
        DROP_RULES.put("drop", CommonConstants.FINITE_DROP);
        DROP_RULES.put("BAS blocking", CommonConstants.FINITE_BLOCK);
        DROP_RULES.put("waiting queue", CommonConstants.FINITE_WAITING);
        DROP_RULES.put("retrial", CommonConstants.FINITE_RETRIAL);
    }

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
}