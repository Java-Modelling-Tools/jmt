package jmt.gui.common.xml.mapper.sections;

import jmt.engine.NetStrategies.ImpatienceStrategies.BalkingParameter;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.ImpatienceStrategies.RenegingParameter;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    private static final Map<String, String> DROP_RULES = new HashMap<>();
    static {
        DROP_RULES.put("drop", CommonConstants.FINITE_DROP);
        DROP_RULES.put("BAS blocking", CommonConstants.FINITE_BLOCK);
        DROP_RULES.put("waiting queue", CommonConstants.FINITE_WAITING);
        DROP_RULES.put("retrial", CommonConstants.FINITE_RETRIAL);
    }

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {

        ParameterDescriptor size = getParam(sec, PARAM_SIZE);
        if (size != null) {
            Integer sVal = valI(size);
            if (sVal != null) model.setStationQueueCapacity(key, sVal);
        }

        ParameterDescriptor putStrategy = getParam(sec, PARAM_QUEUE_PUT_STRATEGY);
        AtomicBoolean isPreemptive = new AtomicBoolean(false);
        AtomicBoolean isPriority = new AtomicBoolean(false);

        if (putStrategy != null) {
            getRefClassMap(putStrategy).forEach((className, subP) -> {
                String cp = subP.getClassPath();
                if (cp == null) return;
                String strategy = cp.substring(cp.lastIndexOf(".") + 1);
                Object classKey = ctx.classes.get(className);

                if (strategy.contains(STRAT_TAIL)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_FCFS);
                else if (strategy.contains(STRAT_HEAD)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_LCFS);
                else if (strategy.contains(STRAT_RAND)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_RAND);
                else if (strategy.contains(STRAT_SJF)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_SJF);
                else if (strategy.contains(STRAT_LJF)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_LJF);
                else if (strategy.contains(STRAT_SEPT)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_SEPT);
                else if (strategy.contains(STRAT_LEPT)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_LEPT);
                else if (strategy.contains(STRAT_FCFSPR)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_FCFS_PR);
                else if (strategy.contains(STRAT_LCFSPR)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_LCFS_PR);
                else if (strategy.contains(STRAT_SRPT)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_SRPT);
                else if (strategy.contains(STRAT_EDD)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_EDD);
                else if (strategy.contains(STRAT_EDF)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_EDF);
                else if (strategy.contains(STRAT_TBS)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_TBS);

                if (strategy.contains("PR") || strategy.contains("SRPT") || strategy.contains("EDF") || strategy.contains("TBS")) isPreemptive.set(true);
                if (strategy.contains("Priority")) isPriority.set(true);
            });
        }

        boolean isPolling = false;
        ParameterDescriptor fcfs = getParam(sec, PARAM_FCFS_STRATEGY);
        if (fcfs != null && fcfs.getClassPath() != null) {
            String cp = fcfs.getClassPath();
            if (cp.contains("Polling")) {
                isPolling = true;
                model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
                if (cp.contains("Limited")) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED);
                else if (cp.contains("Gated")) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
                else if (cp.contains("Exhaustive")) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);

                getSubParams(fcfs).stream()
                        .filter(p -> "pollingKValue".equals(p.getName()))
                        .findFirst()
                        .ifPresent(p -> {
                            Integer kVal = valI(p);
                            if (kVal != null) model.setStationPollingServerKValue(key, kVal);
                        });
            }
        }

        if (!isPolling) {
            if (isPreemptive.get()) model.setStationQueueStrategy(key, isPriority.get() ? CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY : CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE);
            else model.setStationQueueStrategy(key, isPriority.get() ? CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY : CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
        }

        ParameterDescriptor drops = getParam(sec, PARAM_DROP_STRATEGIES);
        ParameterDescriptor retrials = getParam(sec, PARAM_RETRIAL_DISTRIBUTIONS);
        if (drops != null) {
            Map<String, ParameterDescriptor> retrialMap = getRefClassMap(retrials);
            getRefClassMap(drops).forEach((className, subP) -> {
                String rule = val(subP);
                debug("Queue %s -> Drop rule per %s: %s", node.getName(), className, rule);
                if (rule != null) {
                    model.setDropRule(key, ctx.classes.get(className), DROP_RULES.get(rule));
                    if ("retrial".equals(rule) && retrialMap.containsKey(className)) {
                        model.setRetrialDistribution(key, ctx.classes.get(className), parseServiceStrategy(retrialMap.get(className), ctx));
                    }
                }
            });
        }

        ParameterDescriptor impatience = getParam(sec, PARAM_IMPATIENCE);
        if (impatience != null) {
            getRefClassMap(impatience).forEach((cName, p) -> {
                Object classKey = ctx.classes.get(cName);
                String impName = p.getName();
                ImpatienceType type = ImpatienceType.NONE;
                if (impName != null && impName.contains("Reneging")) type = ImpatienceType.RENEGING;
                else if (impName != null && impName.contains("Balking")) type = ImpatienceType.BALKING;

                if (type == ImpatienceType.RENEGING) {
                    model.setImpatienceParameter(key, classKey, new RenegingParameter((Distribution) parseServiceStrategy(p, ctx)));
                } else if (type == ImpatienceType.BALKING) {
                    List<ParameterDescriptor> subs = getSubParams(p);
                    BalkingParameter bp = new BalkingParameter((LDStrategy) parseServiceStrategy(getAt(subs, 1), ctx));
                    String bVal = val(getAt(subs, 3));
                    if (bVal != null) bp.setPriorityActivated(Boolean.parseBoolean(bVal));
                    model.setImpatienceParameter(key, classKey, bp);
                } else {
                    model.setImpatienceParameter(key, classKey, null);
                }
                model.setImpatienceType(key, classKey, type);
            });
        }
    }
}