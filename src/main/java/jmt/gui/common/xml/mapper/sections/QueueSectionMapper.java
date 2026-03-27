/**
 * This class is responsible for mapping the Queue section of a node.
 * It configures queue strategies (FCFS, LCFS, etc.), capacity limits, drop rules, and impatience strategies.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueSectionMapper extends AbstractSectionMapper {

    private static final String STRATEGIES_BASE = "jmt.engine.NetStrategies.";
    private static final String QUEUE_GET_SUFFIX = "QueueGetStrategies.";
    private static final String QUEUE_PUT_SUFFIX = "QueuePutStrategies.";

    private static final Map<String, String> DROP_RULES = new HashMap<>();
    static {
        DROP_RULES.put("drop", CommonConstants.FINITE_DROP);
        DROP_RULES.put("BAS blocking", CommonConstants.FINITE_BLOCK);
        DROP_RULES.put("waiting queue", CommonConstants.FINITE_WAITING);
        DROP_RULES.put("retrial", CommonConstants.FINITE_RETRIAL);
    }

    private static final Map<String, String> DROP_RULES_REVERSE = new HashMap<>();
    static {
        DROP_RULES.forEach((xmlVal, modelVal) -> DROP_RULES_REVERSE.put(modelVal, xmlVal));
    }

    private static final Map<String, String> QUEUE_PUT_MAPPING = new HashMap<>();
    static {
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_FCFS, "TailStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_LCFS, "HeadStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_RAND, "RandStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_SJF, "SJFStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_LJF, "LJFStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_SEPT, "SEPTStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_LEPT, "LEPTStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_FCFS_PR, "FCFSPRStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_LCFS_PR, "LCFSPRStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_SRPT, "SRPTStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_EDD, "EDDStrategy");
        QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_EDF, "EDFStrategy");
    }

    private static final Map<String, String> PRIORITY_QUEUE_PUT_MAPPING = new HashMap<>();
    static {
        QUEUE_PUT_MAPPING.forEach((k, v) -> PRIORITY_QUEUE_PUT_MAPPING.put(k, v + "Priority"));
        PRIORITY_QUEUE_PUT_MAPPING.put(CommonConstants.QUEUE_STRATEGY_TBS, "TBSStrategyPriority");
    }

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        mapQueueSize(sec, model, key);

        AtomicBoolean isPreemptive = new AtomicBoolean(false);
        AtomicBoolean isPriority = new AtomicBoolean(false);
        ParameterDescriptor putStrategy = getParam(sec, XMLConstantNames.PARAM_QUEUE_PUT_STRATEGY);
        if (putStrategy != null) {
            mapPutStrategies(putStrategy, key, model, ctx, isPreemptive, isPriority);
        }

        boolean isPolling = mapPollingStrategy(sec, model, key);

        if (!isPolling && putStrategy != null) {
            setStationQueueStrategy(key, model, isPreemptive.get(), isPriority.get());
        }

        mapDropStrategies(sec, key, model, ctx);
        mapImpatienceStrategies(sec, key, model, ctx);
        mapPetriNetConditions(sec, key, ctx);
    }

    private void mapQueueSize(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor size = getParam(sec, XMLConstantNames.PARAM_SIZE);
        if (size != null) {
            Integer sVal = valI(size);
            if (sVal != null) model.setStationQueueCapacity(key, sVal);
        }
    }

    private void mapPutStrategies(ParameterDescriptor putStrategy, Object key, CommonModel model, MappingContext ctx, AtomicBoolean isPreemptive, AtomicBoolean isPriority) {
        getRefClassMap(putStrategy).forEach((className, subP) -> {
            String cp = subP.getClassPath();
            if (cp == null) return;
            String strategy = cp.substring(cp.lastIndexOf(".") + 1);
            Object classKey = ctx.classes.get(className);

            setQueueStrategy(key, classKey, strategy, model);

            if (strategy.contains("PR") || strategy.contains("SRPT") || strategy.contains("EDF") || strategy.contains("TBS")) isPreemptive.set(true);
            if (strategy.contains("Priority")) isPriority.set(true);
        });
    }

    private void setQueueStrategy(Object key, Object classKey, String strategy, CommonModel model) {
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
    }

    private boolean mapPollingStrategy(SectionDescriptor sec, CommonModel model, Object key) {
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

            getSubParams(fcfs).stream()
                    .filter(p -> "pollingKValue".equals(p.getName()))
                    .findFirst()
                    .ifPresent(p -> {
                        Integer kVal = valI(p);
                        if (kVal != null) model.setStationPollingServerKValue(key, kVal);
                    });
        }
        return isPolling;
    }

    private void setStationQueueStrategy(Object key, CommonModel model, boolean preemptive, boolean priority) {
        if (preemptive) {
            model.setStationQueueStrategy(key, priority ? CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY : CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE);
        } else {
            model.setStationQueueStrategy(key, priority ? CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY : CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
        }
    }

    private void mapDropStrategies(SectionDescriptor sec, Object key, CommonModel model, MappingContext ctx) {
        ParameterDescriptor drops = getParam(sec, XMLConstantNames.PARAM_DROP_STRATEGIES);
        ParameterDescriptor retrials = getParam(sec, XMLConstantNames.PARAM_RETRIAL_DISTRIBUTIONS);
        if (drops != null) {
            Map<String, ParameterDescriptor> retrialMap = getRefClassMap(retrials);
            getRefClassMap(drops).forEach((className, subP) -> {
                String rule = val(subP);
                if (rule != null) {
                    Object classKey = ctx.classes.get(className);
                    model.setDropRule(key, classKey, DROP_RULES.get(rule));
                    if ("retrial".equals(rule) && retrialMap.containsKey(className)) {
                        model.setRetrialDistribution(key, classKey, parseServiceStrategy(retrialMap.get(className), ctx));
                    }
                }
            });
        }
    }

    private void mapImpatienceStrategies(SectionDescriptor sec, Object key, CommonModel model, MappingContext ctx) {
        ParameterDescriptor impatience = getParam(sec, XMLConstantNames.PARAM_IMPATIENCE);
        if (impatience != null) {
            getRefClassMap(impatience).forEach((cName, p) -> mapSingleImpatienceStrategy(cName, p, key, model, ctx));
        }
    }

    private void mapSingleImpatienceStrategy(String cName, ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
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
    }

    private void mapPetriNetConditions(SectionDescriptor sec, Object key, MappingContext ctx) {
        ParameterDescriptor en = getParam(sec, "enablingConditions");
        ParameterDescriptor in = getParam(sec, "inhibitingConditions");
        ParameterDescriptor res = getParam(sec, "resourceConditions");

        if (en != null || in != null || res != null) {
            extractTransitionConditions(key, en, in, res, ctx);
        }
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        addQueueSizeParam(sec, model, key);
        addDropStrategiesParam(sec, model, key, context);
        addQueueGetStrategyParam(sec, model, key);
        addQueuePutStrategiesParam(sec, model, key);
        addImpatienceParam(sec, model, key, context);

        if (CommonConstants.STATION_TYPE_SERVER.equals(stationType) || CommonConstants.STATION_TYPE_DELAY.equals(stationType)) {
            mapTransitionConditions(key, sec, model, context);
        }
    }

    private void addQueueSizeParam(SectionDescriptor sec, CommonModel model, Object key) {
        Integer capacity = model.getStationQueueCapacity(key);
        ParameterDescriptor size = new ParameterDescriptor();
        size.setName(XMLConstantNames.PARAM_SIZE);
        size.setClassPath("java.lang.Integer");
        size.setValue(capacity == null ? "-1" : capacity.toString());
        sec.addParameter(size);
    }

    private void addDropStrategiesParam(SectionDescriptor sec, CommonModel model, Object key, WritingContext context) {
        Vector<Object> classes = model.getClassKeys();
        ParameterDescriptor dropStrategies = new ParameterDescriptor();
        dropStrategies.setName(XMLConstantNames.PARAM_DROP_STRATEGIES);
        dropStrategies.setClassPath(String.class.getName());

        ParameterDescriptor retrialDistributions = new ParameterDescriptor();
        retrialDistributions.setName(XMLConstantNames.PARAM_RETRIAL_DISTRIBUTIONS);
        retrialDistributions.setClassPath(STRATEGIES_BASE + "ServiceStrategy");

        boolean isRetrialEnabled = false;
        for (Object classKey : classes) {
            String strategy = model.getDropRule(key, classKey);
            ParameterDescriptor drop = new ParameterDescriptor();
            drop.setName("dropStrategy");
            drop.setClassPath(String.class.getName());
            drop.setRefClass(model.getClassName(classKey));
            drop.setValue(DROP_RULES_REVERSE.get(strategy));
            dropStrategies.addSubParameter(drop);

            if (CommonConstants.FINITE_RETRIAL.equals(strategy)) {
                Object dist = model.getRetrialDistribution(key, classKey);
                retrialDistributions.addSubParameter(getDistributionParameter(dist, model, classKey, "retrialDistribution", STRATEGIES_BASE + "ServiceStrategies.ServiceTimeStrategy", context));
                isRetrialEnabled = true;
            }
        }

        sec.addParameter(dropStrategies);
        if (isRetrialEnabled) {
            sec.addParameter(retrialDistributions);
        }
    }

    private void addQueueGetStrategyParam(SectionDescriptor sec, CommonModel model, Object key) {
        String stationQueueStrategy = model.getStationQueueStrategy(key);
        String getStrategy = resolveGetStrategyName(stationQueueStrategy, model, key);

        ParameterDescriptor fcfs = new ParameterDescriptor();
        fcfs.setName(XMLConstantNames.PARAM_FCFS_STRATEGY);
        fcfs.setClassPath(STRATEGIES_BASE + QUEUE_GET_SUFFIX + getStrategy);

        if (isLimitedPolling(stationQueueStrategy, model, key)) {
            ParameterDescriptor kVal = new ParameterDescriptor();
            kVal.setName("pollingKValue");
            kVal.setClassPath("java.lang.Integer");
            kVal.setValue(model.getStationPollingServerKValue(key).toString());
            fcfs.addSubParameter(kVal);
        }

        sec.addParameter(fcfs);
    }

    private String resolveGetStrategyName(String stationQueueStrategy, CommonModel model, Object key) {
        if (!CommonConstants.STATION_QUEUE_STRATEGY_POLLING.equals(stationQueueStrategy)) {
            return "FCFSstrategy";
        }
        String pollingType = model.getStationPollingServerType(key);
        if (CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED.equals(pollingType)) return "LimitedPollingGetStrategy";
        if (CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED.equals(pollingType)) return "GatedPollingGetStrategy";
        if (CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE.equals(pollingType)) return "ExhaustivePollingGetStrategy";
        return "FCFSstrategy";
    }

    private boolean isLimitedPolling(String stationQueueStrategy, CommonModel model, Object key) {
        return CommonConstants.STATION_QUEUE_STRATEGY_POLLING.equals(stationQueueStrategy)
                && CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED.equals(model.getStationPollingServerType(key));
    }

    private void addQueuePutStrategiesParam(SectionDescriptor sec, CommonModel model, Object key) {
        String queueStrategy = model.getStationQueueStrategy(key);
        Map<String, String> strategyMapping = isPriorityStrategy(queueStrategy) ? PRIORITY_QUEUE_PUT_MAPPING : QUEUE_PUT_MAPPING;

        ParameterDescriptor putStrategies = new ParameterDescriptor();
        putStrategies.setName(XMLConstantNames.PARAM_QUEUE_PUT_STRATEGY);
        putStrategies.setClassPath(STRATEGIES_BASE + "QueuePutStrategy");

        for (Object classKey : model.getClassKeys()) {
            String strategy = isPSStrategy(queueStrategy) ? CommonConstants.QUEUE_STRATEGY_FCFS : model.getQueueStrategy(key, classKey);
            String strategyName = strategyMapping.get(strategy);

            ParameterDescriptor put = new ParameterDescriptor();
            put.setName(strategyName);
            put.setClassPath(STRATEGIES_BASE + QUEUE_PUT_SUFFIX + strategyName);
            put.setRefClass(model.getClassName(classKey));
            putStrategies.addSubParameter(put);
        }

        sec.addParameter(putStrategies);
    }

    private boolean isPSStrategy(String queueStrategy) {
        return CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER.equals(queueStrategy)
                || CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY.equals(queueStrategy);
    }

    private boolean isPriorityStrategy(String queueStrategy) {
        return CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY.equals(queueStrategy)
                || CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY.equals(queueStrategy);
    }

    private void addImpatienceParam(SectionDescriptor sec, CommonModel model, Object key, WritingContext context) {
        Vector<Object> classes = model.getClassKeys();
        ParameterDescriptor impatience = new ParameterDescriptor();
        impatience.setName(XMLConstantNames.PARAM_IMPATIENCE);
        impatience.setClassPath(ImpatienceType.class.getPackage().getName() + ".Impatience");

        boolean isEnabled = false;
        for (Object classKey : classes) {
            ImpatienceType type = model.getImpatienceType(key, classKey);
            if (type == ImpatienceType.RENEGING) {
                impatience.addSubParameter(createRenegingParam(model, key, classKey, context));
                isEnabled = true;
            } else if (type == ImpatienceType.BALKING) {
                impatience.addSubParameter(createBalkingParam(model, key, classKey, context));
                isEnabled = true;
            }
        }

        if (isEnabled) {
            sec.addParameter(impatience);
        }
    }

    private ParameterDescriptor createRenegingParam(CommonModel model, Object key, Object classKey, WritingContext context) {
        RenegingParameter rp = (RenegingParameter) model.getImpatienceParameter(key, classKey);
        Distribution dist = rp.getDistribution();
        String renegingPath = ImpatienceType.class.getPackage().getName() + ".Reneging";
        return getDistributionParameter(dist, model, classKey, "Reneging", renegingPath, context);
    }

    private ParameterDescriptor createBalkingParam(CommonModel model, Object key, Object classKey, WritingContext context) {
        BalkingParameter bp = (BalkingParameter) model.getImpatienceParameter(key, classKey);
        LDStrategy strategy = bp.getLdStrategy();
        String balkingPath = ImpatienceType.class.getPackage().getName() + ".Balking";

        ParameterDescriptor ldParam = getDistributionParameter(strategy, model, classKey, "LoadDependentStrategy", "jmt.engine.NetStrategies.ServiceStrategies.LoadDependentStrategy", context);

        ParameterDescriptor priorityParam = new ParameterDescriptor();
        priorityParam.setName("priorityActivated");
        priorityParam.setClassPath(Boolean.class.getName());
        priorityParam.setValue(String.valueOf(bp.getPriorityActivated()));

        ParameterDescriptor balking = new ParameterDescriptor();
        balking.setName("Balking");
        balking.setClassPath(balkingPath);
        balking.setRefClass(model.getClassName(classKey));
        balking.addSubParameter(ldParam);
        balking.addSubParameter(priorityParam);
        return balking;
    }
}