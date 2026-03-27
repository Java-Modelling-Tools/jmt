package jmt.gui.common.xml.mapper.sections;

import jmt.engine.NetStrategies.ImpatienceStrategies.BalkingParameter;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.ImpatienceStrategies.RenegingParameter;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


public class QueueSectionMapper extends AbstractSectionMapper {



    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor size = getParam(sec, XMLConstantNames.PARAM_SIZE);
        if (size != null) {
            Integer sVal = valI(size);
            if (sVal != null) model.setStationQueueCapacity(key, sVal);
        }

        AtomicBoolean isPreemptive = new AtomicBoolean(false);
        AtomicBoolean isPriority = new AtomicBoolean(false);
        // Fallback name for queue strategy
        ParameterDescriptor putStrategy = getParam(sec, XMLConstantNames.PARAM_QUEUE_PUT_STRATEGY);
        if (putStrategy == null) putStrategy = getParam(sec, "NetStrategy");

        if (putStrategy != null) {
            getRefClassMap(putStrategy).forEach((className, subP) -> {
                String cp = subP.getClassPath();
                if (cp != null) {
                    String strategy = cp.substring(cp.lastIndexOf(".") + 1);
                    Object classKey = ctx.classes.get(className);
                    if (strategy.contains(XMLConstantNames.STRAT_TAIL)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_FCFS);
                    else if (strategy.contains(XMLConstantNames.STRAT_HEAD)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_LCFS);
                    else if (strategy.contains(XMLConstantNames.STRAT_RAND)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_RAND);
                    else if (strategy.contains(XMLConstantNames.STRAT_SJF)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_SJF);
                    else if (strategy.contains(XMLConstantNames.STRAT_LJF)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_LJF);
                    else if (strategy.contains(XMLConstantNames.STRAT_SEPT)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_SEPT);
                    else if (strategy.contains(XMLConstantNames.STRAT_LEPT)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_LEPT);
                    else if (strategy.contains(XMLConstantNames.STRAT_FCFSPR)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_FCFS_PR);
                    else if (strategy.contains(XMLConstantNames.STRAT_LCFSPR)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_LCFS_PR);
                    else if (strategy.contains(XMLConstantNames.STRAT_SRPT)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_SRPT);
                    else if (strategy.contains(XMLConstantNames.STRAT_EDD)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_EDD);
                    else if (strategy.contains(XMLConstantNames.STRAT_EDF)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_EDF);
                    else if (strategy.contains(XMLConstantNames.STRAT_TBS)) model.setQueueStrategy(key, classKey, CommonConstants.QUEUE_STRATEGY_TBS);

                    if (strategy.contains("PR") || strategy.contains("SRPT") || strategy.contains("EDF") || strategy.contains("TBS")) isPreemptive.set(true);
                    if (strategy.contains("Priority")) isPriority.set(true);
                }
            });
        }

        boolean isPolling = false;
        ParameterDescriptor fcfs = getParam(sec, XMLConstantNames.PARAM_FCFS_STRATEGY);
        if (fcfs != null) {
            String cp = fcfs.getClassPath();
            if (cp != null && cp.contains("Polling")) {
                isPolling = true;
                model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
                if (cp.contains("Limited")) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED);
                else if (cp.contains("Gated")) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
                else if (cp.contains("Exhaustive")) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);
            }
            getSubParams(fcfs).stream().filter(p -> "pollingKValue".equals(p.getName())).findFirst().ifPresent(p -> {
                Integer kVal = valI(p);
                if (kVal != null) model.setStationPollingServerKValue(key, kVal);
            });
        }

        if (!isPolling && putStrategy != null) {
            model.setStationQueueStrategy(key, isPreemptive.get() ? (isPriority.get() ? CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY : CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE) : (isPriority.get() ? CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY : CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE));
        }

        ParameterDescriptor drops = getParam(sec, XMLConstantNames.PARAM_DROP_STRATEGIES);
        ParameterDescriptor retrials = getParam(sec, XMLConstantNames.PARAM_RETRIAL_DISTRIBUTIONS);
        if (drops != null) {
            Map<String, ParameterDescriptor> retrialMap = getRefClassMap(retrials);
            getRefClassMap(drops).forEach((className, subP) -> {
                String rule = val(subP);
                if (rule != null) {
                    Object classKey = ctx.classes.get(className);
                    model.setDropRule(key, classKey, Maps.DROP_RULES.get(rule));
                    if ("retrial".equals(rule) && retrialMap.containsKey(className)) {
                        model.setRetrialDistribution(key, classKey, parseServiceStrategy(retrialMap.get(className), ctx));
                    }
                }
            });
        }

        ParameterDescriptor impatience = getParam(sec, XMLConstantNames.PARAM_IMPATIENCE);
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
                    String bVal = val(getAt(subs, 3)); // adjusted index if needed
                    if (bVal != null) bp.setPriorityActivated(Boolean.parseBoolean(bVal));
                    model.setImpatienceParameter(key, classKey, bp);
                } else {
                    model.setImpatienceParameter(key, classKey, null);
                }
                model.setImpatienceType(key, classKey, type);
            });
        }

        extractTransitionConditions(key, getParam(sec, "enablingConditions"), getParam(sec, "inhibitingConditions"), getParam(sec, "resourceConditions"), ctx);
    }

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        Integer capacity = model.getStationQueueCapacity(key);
        sec.addParameter(makeParam(XMLConstantNames.PARAM_SIZE, "java.lang.Integer", capacity == null ? "-1" : capacity.toString()));

        List<Object> classes = model.getClassKeys();

        ParameterDescriptor dropStrategies = makeContainer(XMLConstantNames.PARAM_DROP_STRATEGIES, String.class.getName());
        dropStrategies.setArray(true); // FIX
        ParameterDescriptor retrialDistributions = makeContainer(XMLConstantNames.PARAM_RETRIAL_DISTRIBUTIONS, STRATEGIES_BASE + "ServiceStrategy");
        retrialDistributions.setArray(true);

        boolean isRetrialEnabled = false;
        for (Object classKey : classes) {
            String strategy = model.getDropRule(key, classKey);
            dropStrategies.addSubParameter(makeRefParam("dropStrategy", String.class.getName(), model.getClassName(classKey), Maps.DROP_RULES_REVERSE.get(strategy)));
            if (CommonConstants.FINITE_RETRIAL.equals(strategy)) {
                Object dist = model.getRetrialDistribution(key, classKey);
                retrialDistributions.addSubParameter(getDistributionParameter(dist, model, classKey, "retrialDistribution", STRATEGIES_BASE + "ServiceStrategies.ServiceTimeStrategy", context));
                isRetrialEnabled = true;
            }
        }
        sec.addParameter(dropStrategies);
        if (isRetrialEnabled) sec.addParameter(retrialDistributions);

        String stationQueueStrategy = model.getStationQueueStrategy(key);
        String getStrategy = "FCFSstrategy";
        if (CommonConstants.STATION_QUEUE_STRATEGY_POLLING.equals(stationQueueStrategy)) {
            String pollingType = model.getStationPollingServerType(key);
            if (CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED.equals(pollingType)) getStrategy = "LimitedPollingGetStrategy";
            else if (CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED.equals(pollingType)) getStrategy = "GatedPollingGetStrategy";
            else if (CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE.equals(pollingType)) getStrategy = "ExhaustivePollingGetStrategy";
        }

        ParameterDescriptor fcfs = makeContainer(XMLConstantNames.PARAM_FCFS_STRATEGY, STRATEGIES_BASE + QUEUE_GET_SUFFIX + getStrategy);
        if ("LimitedPollingGetStrategy".equals(getStrategy)) {
            fcfs.addSubParameter(makeParam("pollingKValue", "java.lang.Integer", model.getStationPollingServerKValue(key).toString()));
        }
        sec.addParameter(fcfs);

        ParameterDescriptor putStrategies = makeContainer("NetStrategy", STRATEGIES_BASE + "QueuePutStrategy"); // FIX NAME LEGACY
        putStrategies.setArray(true); // FIX

        boolean isPriority = CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY.equals(stationQueueStrategy) || CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY.equals(stationQueueStrategy);
        boolean isPS = CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER.equals(stationQueueStrategy) || CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY.equals(stationQueueStrategy);
        Map<String, String> strategyMapping = isPriority ? Maps.PRIORITY_QUEUE_PUT_MAPPING : Maps.QUEUE_PUT_MAPPING;

        for (Object classKey : classes) {
            String strategy = isPS ? CommonConstants.QUEUE_STRATEGY_FCFS : model.getQueueStrategy(key, classKey);
            String strategyName = strategyMapping.get(strategy);
            putStrategies.addSubParameter(makeRefParam(strategyName, STRATEGIES_BASE + QUEUE_PUT_SUFFIX + strategyName, model.getClassName(classKey), null));
        }
        sec.addParameter(putStrategies);

        ParameterDescriptor impatience = makeContainer(XMLConstantNames.PARAM_IMPATIENCE, ImpatienceType.class.getPackage().getName() + ".Impatience");
        impatience.setArray(true); // FIX

        boolean hasImpatience = false;
        for (Object classKey : classes) {
            ImpatienceType type = model.getImpatienceType(key, classKey);
            if (type == ImpatienceType.RENEGING) {
                RenegingParameter rp = (RenegingParameter) model.getImpatienceParameter(key, classKey);
                impatience.addSubParameter(getDistributionParameter(rp.getDistribution(), model, classKey, "Reneging", ImpatienceType.class.getPackage().getName() + ".Reneging", context));
                hasImpatience = true;
            } else if (type == ImpatienceType.BALKING) {
                BalkingParameter bp = (BalkingParameter) model.getImpatienceParameter(key, classKey);
                ParameterDescriptor balking = makeContainer("Balking", ImpatienceType.class.getPackage().getName() + ".Balking");
                balking.setRefClass(model.getClassName(classKey));
                balking.addSubParameter(getDistributionParameter(bp.getLdStrategy(), model, null, "LoadDependentStrategy", "jmt.engine.NetStrategies.ServiceStrategies.LoadDependentStrategy", context));
                balking.addSubParameter(makeParam("priorityActivated", Boolean.class.getName(), String.valueOf(bp.getPriorityActivated())));
                impatience.addSubParameter(balking);
                hasImpatience = true;
            }
        }
        if (hasImpatience) sec.addParameter(impatience);

        if (CommonConstants.STATION_TYPE_SERVER.equals(stationType) || CommonConstants.STATION_TYPE_DELAY.equals(stationType)) {
            mapTransitionConditions(key, sec, model, context);
        }
    }
}