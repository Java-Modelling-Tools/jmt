/**
 * Maps queue and storage section descriptors between the DTO and the {@link jmt.gui.common.definitions.CommonModel},
 * including queue policies, capacity, impatience strategies, and drop rules.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.engine.NetStrategies.ImpatienceStrategies.BalkingParameter;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.ImpatienceStrategies.RenegingParameter;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.List;
import java.util.Map;

public class QueueSectionMapper extends AbstractSectionMapper {

    private static final String STRATEGIES_BASE = "jmt.engine.NetStrategies.";
    private static final String QUEUE_GET_SUFFIX = "QueueGetStrategies.";
    private static final String QUEUE_PUT_SUFFIX = "QueuePutStrategies.";

    // =========================================================================================
    // --- READING PHASE (XML -> DTO -> Model) ---
    // =========================================================================================

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        mapQueueSize(sec, model, key);

        mapPutStrategies(sec, model, key, ctx);
        mapGetStrategies(sec, model, key);

        mapDropRulesAndRetrials(sec, model, key, ctx);
        mapImpatience(sec, model, key, ctx);

        extractTransitionConditions(key, getParam(sec, "enablingConditions"), getParam(sec, "inhibitingConditions"), getParam(sec, "resourceConditions"), ctx);
    }

    private void mapQueueSize(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor size = getParam(sec, XMLConstantNames.PARAM_SIZE);
        if (size != null) {
            Integer sVal = valI(size);
            if (sVal != null) model.setStationQueueCapacity(key, sVal);
        }
    }

    private boolean mapPutStrategies(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
        ParameterDescriptor putStrategy = getParam(sec, XMLConstantNames.PARAM_QUEUE_PUT_STRATEGY);
        if (putStrategy == null) putStrategy = getParam(sec, "NetStrategy");
        if (putStrategy == null) return false;

        getRefClassMap(putStrategy).forEach((className, subP) -> {
            String cp = subP.getClassPath();
            if (cp != null) {
                String strategy = cp.substring(cp.lastIndexOf(".") + 1);
                Object classKey = ctx.classes.get(className);

                model.setQueueStrategy(key, classKey, resolveQueueStrategyConstant(strategy));

                String stationStrategy = resolveStationQueueStrategyFromPutStrategy(strategy);
                if (stationStrategy != null) {
                    String merged = mergeStationQueueStrategies(model.getStationQueueStrategy(key), stationStrategy);
                    model.setStationQueueStrategy(key, merged);
                }
            }
        });

        return true;
    }

    private String resolveStationQueueStrategyFromPutStrategy(String strategy) {
        if (strategy.contains(XMLConstantNames.STRAT_FCFSPR)
                || strategy.contains(XMLConstantNames.STRAT_LCFSPR)
                || strategy.contains(XMLConstantNames.STRAT_SRPT)
                || strategy.contains(XMLConstantNames.STRAT_EDF)
                || strategy.contains(XMLConstantNames.STRAT_TBS)) {
            if (strategy.contains("Priority")) {
                return CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY;
            } else {
                return CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE;
            }
        }

        if (strategy.contains("Priority")) {
            return CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY;
        } else {
            return CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
        }
    }

    private String mergeStationQueueStrategies(String current, String candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return rankStationQueueStrategy(candidate) > rankStationQueueStrategy(current) ? candidate : current;
    }

    private int rankStationQueueStrategy(String strategy) {
        if (CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY.equals(strategy)) return 4;
        if (CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE.equals(strategy)) return 3;
        if (CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY.equals(strategy)) return 2;
        if (CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE.equals(strategy)) return 1;
        return 0;
    }

    private String resolveQueueStrategyConstant(String strategy) {
        if (strategy.contains(XMLConstantNames.STRAT_TAIL)) return CommonConstants.QUEUE_STRATEGY_FCFS;
        if (strategy.contains(XMLConstantNames.STRAT_HEAD)) return CommonConstants.QUEUE_STRATEGY_LCFS;
        if (strategy.contains(XMLConstantNames.STRAT_RAND)) return CommonConstants.QUEUE_STRATEGY_RAND;
        if (strategy.contains(XMLConstantNames.STRAT_SJF)) return CommonConstants.QUEUE_STRATEGY_SJF;
        if (strategy.contains(XMLConstantNames.STRAT_LJF)) return CommonConstants.QUEUE_STRATEGY_LJF;
        if (strategy.contains(XMLConstantNames.STRAT_SEPT)) return CommonConstants.QUEUE_STRATEGY_SEPT;
        if (strategy.contains(XMLConstantNames.STRAT_LEPT)) return CommonConstants.QUEUE_STRATEGY_LEPT;
        if (strategy.contains(XMLConstantNames.STRAT_FCFSPR)) return CommonConstants.QUEUE_STRATEGY_FCFS_PR;
        if (strategy.contains(XMLConstantNames.STRAT_LCFSPR)) return CommonConstants.QUEUE_STRATEGY_LCFS_PR;
        if (strategy.contains(XMLConstantNames.STRAT_SRPT)) return CommonConstants.QUEUE_STRATEGY_SRPT;
        if (strategy.contains(XMLConstantNames.STRAT_EDD)) return CommonConstants.QUEUE_STRATEGY_EDD;
        if (strategy.contains(XMLConstantNames.STRAT_EDF)) return CommonConstants.QUEUE_STRATEGY_EDF;
        if (strategy.contains(XMLConstantNames.STRAT_TBS)) return CommonConstants.QUEUE_STRATEGY_TBS;
        return CommonConstants.QUEUE_STRATEGY_FCFS;
    }

    private boolean mapGetStrategies(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor fcfs = getParam(sec, XMLConstantNames.PARAM_FCFS_STRATEGY);
        if (fcfs == null) return false;

        getSubParams(fcfs).stream()
                .filter(p -> "pollingKValue".equals(p.getName()))
                .findFirst()
                .ifPresent(p -> {
                    Integer kVal = valI(p);
                    if (kVal != null) model.setStationPollingServerKValue(key, kVal);
                });

        // Legacy reader uses the parameter NAME ("FCFSstrategy") instead of classpath,
        // so polling detection usually does not trigger here.
        String getStrategy = fcfs.getName();
        if ("LimitedPollingGetStrategy".equals(getStrategy)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED);
            return true;
        } else if ("GatedPollingGetStrategy".equals(getStrategy)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
            return true;
        } else if ("ExhaustivePollingGetStrategy".equals(getStrategy)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);
            return true;
        }

        return false;
    }


    private void mapDropRulesAndRetrials(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
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
    }

    private void mapImpatience(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
        ParameterDescriptor impatience = getParam(sec, XMLConstantNames.PARAM_IMPATIENCE);
        if (impatience == null) return;

        getRefClassMap(impatience).forEach((cName, p) -> {
            Object classKey = ctx.classes.get(cName);
            String impName = p.getName();
            ImpatienceType type;

            if (impName != null && impName.contains("Reneging")) {
                type = ImpatienceType.RENEGING;
            } else if (impName != null && impName.contains("Balking")) {
                type = ImpatienceType.BALKING;
            } else {
                type = ImpatienceType.NONE;
            }

            if (type == ImpatienceType.RENEGING) {
                model.setImpatienceParameter(key, classKey, new RenegingParameter((Distribution) parseServiceStrategy(p, ctx)));
            } else if (type == ImpatienceType.BALKING) {
                List<ParameterDescriptor> subs = getSubParams(p);
                BalkingParameter bp = new BalkingParameter((LDStrategy) parseServiceStrategy(getAt(subs, 0), ctx));
                String bVal = val(getAt(subs, 1));
                if (bVal != null) bp.setPriorityActivated(Boolean.parseBoolean(bVal));
                model.setImpatienceParameter(key, classKey, bp);
            } else {
                model.setImpatienceParameter(key, classKey, null);
            }
            model.setImpatienceType(key, classKey, type);
        });
    }

    // =========================================================================================
    // --- WRITING PHASE (Model -> DTO -> XML) ---
    // =========================================================================================

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        writeQueueSize(sec, model, key);
        writeDropRulesAndRetrials(sec, model, key, context);
        writeGetStrategy(sec, model, key);
        writePutStrategies(sec, model, key);
        writeImpatience(sec, model, key, context);

        if (CommonConstants.STATION_TYPE_SERVER.equals(stationType) || CommonConstants.STATION_TYPE_DELAY.equals(stationType)) {
            mapTransitionLikeConditionsForServerDelay(key, sec, model);
        }

    }

    private void writeQueueSize(SectionDescriptor sec, CommonModel model, Object key) {
        Integer capacity = model.getStationQueueCapacity(key);
        String capStr = capacity == null ? "-1" : capacity.toString();
        sec.addParameter(makeParam(XMLConstantNames.PARAM_SIZE, "java.lang.Integer", capStr));
    }

    private void writeDropRulesAndRetrials(SectionDescriptor sec, CommonModel model, Object key, WritingContext context) {
        List<Object> classes = model.getClassKeys();

        ParameterDescriptor dropStrategies = makeContainer(XMLConstantNames.PARAM_DROP_STRATEGIES, String.class.getName());
        dropStrategies.setArray(true);

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
        if (isRetrialEnabled) {
            sec.addParameter(retrialDistributions);
        }
    }

    private void writeGetStrategy(SectionDescriptor sec, CommonModel model, Object key) {
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
    }

    private void writePutStrategies(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor putStrategies = makeContainer(XMLConstantNames.PARAM_QUEUE_PUT_STRATEGY, STRATEGIES_BASE + "QueuePutStrategy");
        putStrategies.setArray(true);

        String stationQueueStrategy = model.getStationQueueStrategy(key);
        boolean isPriority = CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY.equals(stationQueueStrategy) || CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY.equals(stationQueueStrategy);
        boolean isPS = CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER.equals(stationQueueStrategy) || CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY.equals(stationQueueStrategy);
        Map<String, String> strategyMapping = isPriority ? Maps.PRIORITY_QUEUE_PUT_MAPPING : Maps.QUEUE_PUT_MAPPING;

        for (Object classKey : model.getClassKeys()) {
            String strategy = isPS ? CommonConstants.QUEUE_STRATEGY_FCFS : model.getQueueStrategy(key, classKey);
            String strategyName = strategyMapping.get(strategy);
            putStrategies.addSubParameter(makeRefParam(strategyName, STRATEGIES_BASE + QUEUE_PUT_SUFFIX + strategyName, model.getClassName(classKey), null));
        }
        sec.addParameter(putStrategies);
    }

    private void writeImpatience(SectionDescriptor sec, CommonModel model, Object key, WritingContext context) {
        ParameterDescriptor impatience = makeContainer(XMLConstantNames.PARAM_IMPATIENCE, ImpatienceType.class.getPackage().getName() + ".Impatience");
        impatience.setArray(true);

        boolean hasImpatience = false;
        for (Object classKey : model.getClassKeys()) {
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

        if (hasImpatience) {
            sec.addParameter(impatience);
        }
    }
}