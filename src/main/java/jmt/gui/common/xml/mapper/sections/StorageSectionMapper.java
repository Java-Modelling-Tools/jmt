package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.HashMap;
import java.util.Map;

public class StorageSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    private static final Map<String, String> DROP_RULES = new HashMap<>();
    static {
        DROP_RULES.put(DROP_XML_DROP, CommonConstants.FINITE_DROP);
        DROP_RULES.put(DROP_XML_BAS_BLOCKING, CommonConstants.FINITE_BLOCK);
        DROP_RULES.put(DROP_XML_WAITING_QUEUE, CommonConstants.FINITE_WAITING);
        DROP_RULES.put(DROP_XML_RETRIAL, CommonConstants.FINITE_RETRIAL);
    }

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor tc = getParam(sec, PARAM_TOTAL_CAPACITY);
        if (tc != null) {
            Integer tVal = valI(tc);
            if (tVal != null) model.setStationQueueCapacity(key, tVal);
        }

        ParameterDescriptor caps = getParam(sec, PARAM_CAPACITIES);
        if (caps != null) {
            getRefClassMap(caps).forEach((c, subP) -> {
                Integer cVal = valI(subP);
                if (cVal != null) model.setQueueCapacity(key, ctx.classes.get(c), cVal);
            });
        }

        ParameterDescriptor drops = getParam(sec, PARAM_DROP_RULES);
        if (drops != null) {
            getRefClassMap(drops).forEach((c, subP) -> {
                String dVal = val(subP);
                if (dVal != null) model.setDropRule(key, ctx.classes.get(c), DROP_RULES.get(dVal));
            });
        }

        ParameterDescriptor puts = getParam(sec, PARAM_PUT_STRATEGIES);
        if (puts != null) {
            getRefClassMap(puts).forEach((c, subP) -> {
                String ps = subP.getClassPath().substring(subP.getClassPath().lastIndexOf(".") + 1);
                if (STRAT_TAIL_STRATEGY.equals(ps)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_FCFS);
                else if (STRAT_HEAD_STRATEGY.equals(ps)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_LCFS);
                else if (STRAT_RAND_STRATEGY.equals(ps)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_RAND);
            });
        }
    }
}