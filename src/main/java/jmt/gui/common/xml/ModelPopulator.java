package jmt.gui.common.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jmt.framework.data.MacroReplacer;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.VacationParameters;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.forkStrategies.ClassSwitchFork;
import jmt.gui.common.forkStrategies.CombFork;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.forkStrategies.ProbabilitiesFork;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.joinStrategies.PartialJoin;
import jmt.gui.common.routingStrategies.ClassSwitchRouting;
import jmt.gui.common.routingStrategies.LoadDependentRouting;
import jmt.gui.common.routingStrategies.PowerOfKRouting;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.routingStrategies.WeightedRoundRobinRouting;
import jmt.gui.common.semaphoreStrategies.NormalSemaphore;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;
import jmt.engine.NetStrategies.ImpatienceStrategies.BalkingParameter;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.ImpatienceStrategies.RenegingParameter;
import jmt.engine.log.LoggerParameters;
import jmt.gui.common.xml.dto.BlockingRegionDescriptor;
import jmt.gui.common.xml.dto.ConnectionDescriptor;
import jmt.gui.common.xml.dto.MeasureDescriptor;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.dto.UserClassDescriptor;

public final class ModelPopulator implements CommonConstants, XMLConstantNames {

    private static final String QUEUE_PUT_STRATEGY = "jmt.engine.NetStrategies.QueuePutStrategy";
    private static final String SERVICE_STRATEGY = "jmt.engine.NetStrategies.ServiceStrategy";
    private static final String PS_STRATEGY = "jmt.engine.NetStrategies.PSStrategy";
    private static final String DISTR_CONTAINER = "jmt.engine.random.DistributionContainer";

    private ModelPopulator() {}

    public static void populate(SimDescriptor descriptor, CommonModel model) {
        if (descriptor == null || model == null) return;

        Map<String, Object> classKeys = new HashMap<String, Object>();
        Map<String, Object> stationKeys = new HashMap<String, Object>();
        Map<String, Object> regionKeys = new HashMap<String, Object>();
        Map<Object, String> refStationsPending = new HashMap<Object, String>();
        Map<StationClassPair, Map<String, OutPath>> pendingProbabilityForkOutPaths = new HashMap<StationClassPair, Map<String, OutPath>>();
        Map<StationClassPair, Map<String, OutPath>> pendingClassSwitchForkOutPaths = new HashMap<StationClassPair, Map<String, OutPath>>();
        List<PendingTransitionCondition> pendingTransitionConditions = new ArrayList<PendingTransitionCondition>();
        List<PendingFiringOutcome> pendingFiringOutcomes = new ArrayList<PendingFiringOutcome>();

        applyGlobalParameters(descriptor, model);
        createBaseClasses(descriptor, model, classKeys, refStationsPending);
        createBaseStations(descriptor, model, stationKeys);

        resolveReferenceStationsPass3(model, stationKeys, refStationsPending);
        processNodeSectionsPass4(descriptor, model, classKeys, stationKeys, pendingProbabilityForkOutPaths, pendingClassSwitchForkOutPaths, pendingTransitionConditions, pendingFiringOutcomes);
        applyTopologyPass5(descriptor, model, classKeys, stationKeys, regionKeys);
        reapplyRouterSectionsPass6(descriptor, model, classKeys, stationKeys);
        applyPendingProbabilityForkOutPaths(model, stationKeys, pendingProbabilityForkOutPaths);
        applyPendingClassSwitchForkOutPaths(model, stationKeys, pendingClassSwitchForkOutPaths);
        applyPendingTransitionConditions(model, stationKeys, pendingTransitionConditions);
        applyPendingFiringOutcomes(model, stationKeys, pendingFiringOutcomes);
        applyPreloads(descriptor, model, classKeys, stationKeys);
    }

    private static final class StationClassPair {
        private final Object stationKey;
        private final Object classKey;

        private StationClassPair(Object stationKey, Object classKey) {
            this.stationKey = stationKey;
            this.classKey = classKey;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof StationClassPair)) return false;
            StationClassPair that = (StationClassPair) other;
            return Objects.equals(stationKey, that.stationKey) && Objects.equals(classKey, that.classKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stationKey, classKey);
        }
    }

    private static final class PendingTransitionCondition {
        private final String conditionType;
        private final Object stationKey;
        private final int modeIndex;
        private final String targetStationName;
        private final Object classKey;
        private final int value;

        private PendingTransitionCondition(String conditionType, Object stationKey, int modeIndex, String targetStationName, Object classKey, int value) {
            this.conditionType = conditionType;
            this.stationKey = stationKey;
            this.modeIndex = modeIndex;
            this.targetStationName = targetStationName;
            this.classKey = classKey;
            this.value = value;
        }
    }

    private static final class PendingFiringOutcome {
        private final Object stationKey;
        private final int modeIndex;
        private final String targetStationName;
        private final Object classKey;
        private final int value;

        private PendingFiringOutcome(Object stationKey, int modeIndex, String targetStationName, Object classKey, int value) {
            this.stationKey = stationKey;
            this.modeIndex = modeIndex;
            this.targetStationName = targetStationName;
            this.classKey = classKey;
            this.value = value;
        }
    }


    private static Object getClassKey(String className, Map<String, Object> classKeys, CommonModel model) {
        if (!hasText(className)) return null;
        Object key = classKeys.get(className);
        return key != null ? key : model.getClassByName(className);
    }

    private static Object getStationKey(String stationName, Map<String, Object> stationKeys, CommonModel model) {
        if (!hasText(stationName)) return null;
        Object key = stationKeys.get(stationName);
        return key != null ? key : model.getStationByName(stationName);
    }

    private static Object resolveClassKey(ParameterDescriptor parameter, ParameterDescriptor parent, Map<String, Object> classKeys, CommonModel model) {
        if (parameter == null) return null;
        String refClass = hasText(parameter.getRefClass()) ? parameter.getRefClass() : (parent != null ? parent.getRefClass() : null);
        return getClassKey(refClass, classKeys, model);
    }

    private static String getParamValue(ParameterDescriptor param) {
        if (param == null) return null;
        if (hasText(param.getValue())) return param.getValue();
        for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
            String val = getParamValue(sub);
            if (hasText(val)) return val;
        }
        return null;
    }

    private static boolean robustMatch(String targetPath, String knownPath, Class<?> knownClass) {
        if (targetPath == null || knownPath == null) return false;
        if (targetPath.equals(knownPath)) return true;
        return targetPath.endsWith("." + knownClass.getSimpleName());
    }

    private static void resolveReferenceStationsPass3(CommonModel model, Map<String, Object> stationKeys, Map<Object, String> refStationsPending) {
        for (Map.Entry<Object, String> entry : refStationsPending.entrySet()) {
            Object classKey = entry.getKey();
            String stationName = entry.getValue();
            if (!hasText(stationName)) continue;
            Object stationRef;
            if (STATION_TYPE_FORK.equalsIgnoreCase(stationName)) stationRef = STATION_TYPE_FORK;
            else if (STATION_TYPE_CLASSSWITCH.equalsIgnoreCase(stationName)) stationRef = STATION_TYPE_CLASSSWITCH;
            else if (STATION_TYPE_SCALER.equalsIgnoreCase(stationName)) stationRef = STATION_TYPE_SCALER;
            else if (STATION_TYPE_TRANSITION.equalsIgnoreCase(stationName)) stationRef = STATION_TYPE_TRANSITION;
            else stationRef = getStationKey(stationName, stationKeys, model);
            model.setClassRefStation(classKey, stationRef);
        }
    }

    private static void processNodeSectionsPass4(SimDescriptor descriptor, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys, Map<StationClassPair, Map<String, OutPath>> pendingProbabilityForkOutPaths, Map<StationClassPair, Map<String, OutPath>> pendingClassSwitchForkOutPaths, List<PendingTransitionCondition> pendingTransitionConditions, List<PendingFiringOutcome> pendingFiringOutcomes) {
        for (NodeDescriptor node : safeList(descriptor.getNodes())) {
            Object stationKey = getStationKey(node.getName(), stationKeys, model);
            if (stationKey == null) continue;

            applyStationNodeParameters(node, stationKey, model);

            for (SectionDescriptor section : safeList(node.getSections())) {
                String className = section.getClassName();
                if (!hasText(className)) continue;

                if (CLASSNAME_SOURCE.equals(className)) buildSourceSection(section, stationKey, model, classKeys);
                else if (CLASSNAME_QUEUE.equals(className)) buildQueueSection(section, stationKey, model, classKeys, stationKeys, pendingTransitionConditions);
                else if (CLASSNAME_SERVER.equals(className) || CLASSNAME_PREEMPTIVESERVER.equals(className) || CLASSNAME_PSSERVER.equals(className) || CLASSNAME_PSSERVER_PRIORITY.equals(className) || CLASSNAME_LKPSERVER.equals(className) || CLASSNAME_GATEDPSERVER.equals(className) || CLASSNAME_EXHAUSTIVEPSERVER.equals(className)) buildServerSection(section, stationKey, model, classKeys, stationKeys);
                else if (CLASSNAME_DELAY.equals(className)) buildDelaySection(section, stationKey, model, classKeys, stationKeys, pendingTransitionConditions);
                else if (CLASSNAME_ROUTER.equals(className) || CLASSNAME_LINKAGE.equals(className)) buildRouterSection(section, stationKey, model, classKeys, stationKeys);
                else if (CLASSNAME_FORK.equals(className)) buildForkSection(section, stationKey, model, classKeys, stationKeys, pendingProbabilityForkOutPaths, pendingClassSwitchForkOutPaths);
                else if (CLASSNAME_JOIN.equals(className)) buildJoinSection(section, stationKey, model, classKeys);
                else if (CLASSNAME_LOGGER.equals(className)) buildLoggerSection(section, stationKey, model);
                else if (CLASSNAME_CLASSSWITCH.equals(className)) buildClassSwitchSection(section, stationKey, model, classKeys);
                else if (CLASSNAME_SEMAPHORE.equals(className)) buildSemaphoreSection(section, stationKey, model, classKeys);
                else if (CLASSNAME_STORAGE.equals(className)) buildStorageSection(section, stationKey, model, classKeys);
                else if (CLASSNAME_ENABLING.equals(className)) buildEnablingSection(section, stationKey, model, classKeys, stationKeys, pendingTransitionConditions);
                else if (CLASSNAME_TIMING.equals(className)) buildTimingSection(section, stationKey, model);
                else if (CLASSNAME_FIRING.equals(className)) buildFiringSection(section, stationKey, model, classKeys, stationKeys, pendingFiringOutcomes);
            }
        }
    }

    private static void applyStationNodeParameters(NodeDescriptor node, Object stationKey, CommonModel model) {
        String stationType = model.getStationType(stationKey);

        if (STATION_TYPE_DELAY.equals(stationType) || STATION_TYPE_SERVER.equals(stationType)) {
            ParameterDescriptor stationDeadlines = findNodeParameter(node, XML_E_STATION_DUE_DATES);
            if (stationDeadlines != null) {
                List<Double> deadlines = new ArrayList<Double>();
                for (ParameterDescriptor softDeadlineParam : safeList(stationDeadlines.getSubParameters())) {
                    Double deadline = parseDouble(getParamValue(softDeadlineParam));
                    if (deadline != null) {
                        deadlines.add(deadline);
                    }
                }

                List<Object> classOrder = model.getClassKeys();
                int bound = Math.min(deadlines.size(), classOrder.size());
                for (int i = 0; i < bound; i++) {
                    model.setClassStationSoftDeadline(stationKey, classOrder.get(i), deadlines.get(i));
                }
            }
        }

        if (STATION_TYPE_SERVER.equals(stationType)) {
            ParameterDescriptor quantumSize = findNodeParameter(node, XML_E_STATION_QUANTUM_SIZE);
            Double quanta = parseDouble(getParamValue(quantumSize));
            if (quanta != null) {
                model.setQuantumSize(stationKey, quanta);
            }

            ParameterDescriptor quantumSwitchover = findNodeParameter(node, XML_E_STATION_QUANTUM_SWITCHOVER_TIME);
            Double switchover = parseDouble(getParamValue(quantumSwitchover));
            if (switchover != null) {
                model.setQuantumSwitchoverTime(stationKey, switchover);
            }
        }
    }

    private static ParameterDescriptor findNodeParameter(NodeDescriptor node, String parameterName) {
        if (node == null || !hasText(parameterName)) {
            return null;
        }
        for (ParameterDescriptor parameter : safeList(node.getParameters())) {
            if (parameterName.equals(parameter.getName())) {
                return parameter;
            }
        }
        return null;
    }

    private static void applyTopologyPass5(SimDescriptor descriptor, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys, Map<String, Object> regionKeys) {
        for (ConnectionDescriptor connection : safeList(descriptor.getConnections())) {
            Object sourceKey = getStationKey(connection.getSource(), stationKeys, model);
            Object targetKey = getStationKey(connection.getTarget(), stationKeys, model);
            if (sourceKey != null && targetKey != null) model.setConnected(sourceKey, targetKey, true);
        }

        for (BlockingRegionDescriptor region : safeList(descriptor.getBlockingRegions())) {
            String type = hasText(region.getType()) ? region.getType() : Defaults.get("blockingRegionType");
            Object regionKey = model.addBlockingRegion(region.getName(), type);
            regionKeys.put(region.getName(), regionKey);

            for (String stationName : safeList(region.getRegionNodes())) {
                Object stationKey = getStationKey(stationName, stationKeys, model);
                if (stationKey != null) model.addRegionStation(regionKey, stationKey);
            }

            Integer maxJobs = parseInteger(firstNonBlank(region.getGlobalConstraints().get(XML_A_GLOBALCONSTRAINT_MAXJOBS), region.getGlobalConstraints().get("maxJobs")));
            if (maxJobs != null) model.setRegionCustomerConstraint(regionKey, maxJobs);
            Integer maxMem = parseInteger(firstNonBlank(region.getGlobalConstraints().get(XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY), region.getGlobalConstraints().get("maxMemory")));
            if (maxMem != null) model.setRegionMemorySize(regionKey, maxMem);

            for (Map.Entry<String, ? extends Map<String, String>> classEntry : region.getClassConstraints().entrySet()) {
                Object classKey = getClassKey(classEntry.getKey(), classKeys, model);
                Map<String, String> constraints = classEntry.getValue();
                if (classKey == null || constraints == null) continue;

                Integer classMaxJobs = parseInteger(firstNonBlank(constraints.get(XML_A_CLASSCONSTRAINT_MAXJOBS), constraints.get("maxJobsPerClass"), constraints.get("maxJobs")));
                if (classMaxJobs != null) model.setRegionClassCustomerConstraint(regionKey, classKey, classMaxJobs);
                Integer classMaxMemory = parseInteger(firstNonBlank(constraints.get(XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY), constraints.get("maxMemoryPerClass"), constraints.get("maxMemory")));
                if (classMaxMemory != null) model.setRegionClassMemorySize(regionKey, classKey, classMaxMemory);
                Double softDeadline = parseDouble(firstNonBlank(constraints.get(XML_A_CLASSDUEDATE_DUEDATE), constraints.get("softDeadline"), constraints.get("dueDate")));
                if (softDeadline != null) model.setRegionClassSoftDeadline(regionKey, classKey, softDeadline);
            }

            for (Map.Entry<String, String> entry : region.getClassWeights().entrySet()) {
                Object classKey = getClassKey(entry.getKey(), classKeys, model);
                Integer weight = parseInteger(entry.getValue());
                if (classKey != null && weight != null) model.setRegionClassWeight(regionKey, classKey, weight);
            }
            for (Map.Entry<String, String> entry : region.getClassSizes().entrySet()) {
                Object classKey = getClassKey(entry.getKey(), classKeys, model);
                Integer size = parseInteger(entry.getValue());
                if (classKey != null && size != null) model.setRegionClassSize(regionKey, classKey, size);
            }
            for (Map.Entry<String, String> entry : region.getDropRules().entrySet()) {
                Object classKey = getClassKey(entry.getKey(), classKeys, model);
                Boolean drop = parseDropRule(entry.getValue());
                if (classKey != null && drop != null) model.setRegionClassDropRule(regionKey, classKey, drop);
            }

            int groupIndex = 0;
            for (Map.Entry<String, String> entry : region.getGroupConstraints().entrySet()) {
                model.addRegionGroup(regionKey, entry.getKey());

                Integer maxJobsPerGroup = parseInteger(entry.getValue());
                if (maxJobsPerGroup != null) model.setRegionGroupCustomerConstraint(regionKey, groupIndex, maxJobsPerGroup);

                Integer maxMemoryPerGroup = parseInteger(region.getGroupMemoryConstraints().get(entry.getKey()));
                if (maxMemoryPerGroup != null) model.setRegionGroupMemorySize(regionKey, groupIndex, maxMemoryPerGroup);

                for (String className : safeList(region.getGroupClassLists().get(entry.getKey()))) {
                    Object classKey = getClassKey(className, classKeys, model);
                    if (classKey != null) model.addClassIntoRegionGroup(regionKey, groupIndex, classKey);
                }

                groupIndex++;
            }
        }

        for (MeasureDescriptor measure : safeList(descriptor.getMeasures())) {
            Object stationKey = NODETYPE_REGION.equalsIgnoreCase(measure.getNodeType()) ? regionKeys.get(measure.getReferenceNode()) : getStationKey(measure.getReferenceNode(), stationKeys, model);
            Object classKey;
            if (SimulationDefinition.MEASURE_FX.equals(measure.getType())) classKey = measure.getReferenceUserClass();
            else classKey = getClassKey(measure.getReferenceUserClass(), classKeys, model);

            Double alpha = parseDouble(measure.getAlpha());
            alpha = (alpha != null) ? Double.valueOf(1.0d - alpha.doubleValue()) : Defaults.getAsDouble("measureAlpha");
            Double precision = parseDouble(measure.getPrecision());
            precision = (precision != null) ? precision : Defaults.getAsDouble("measurePrecision");
            boolean verbose = Boolean.TRUE.equals(parseBoolean(measure.getVerbose()));
            model.addMeasure(measure.getType(), stationKey, classKey, alpha, precision, verbose);
        }
    }

    private static void applyPreloads(SimDescriptor descriptor, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys) {
        for (NodeDescriptor preloadNode : safeList(descriptor.getPreloads())) {
            Object stationKey = getStationKey(preloadNode.getName(), stationKeys, model);
            if (stationKey == null) continue;
            for (SectionDescriptor classPop : safeList(preloadNode.getSections())) {
                Object classKey = getClassKey(classPop.getClassName(), classKeys, model);
                if (classPop.getParameters() != null && !classPop.getParameters().isEmpty()) {
                    Integer jobs = parseInteger(classPop.getParameters().get(0).getValue());
                    if (classKey != null && jobs != null) {
                        model.setPreloadedJobs(stationKey, classKey, jobs);
                    }
                }
            }
        }
    }

    private static void buildSourceSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if (!(SERVICE_STRATEGY.equals(param.getClassPath()) || "ServiceStrategy".equals(param.getName()))) {
                continue;
            }
            for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                Object classKey = resolveClassKey(sub, param, classKeys, model);
                if (classKey == null || model.getClassType(classKey) != CLASS_TYPE_OPEN) continue;
                Object currentRefStation = model.getClassRefStation(classKey);
                if (currentRefStation == null || !currentRefStation.equals(stationKey)) continue;

                Object dist = parseServiceStrategy(sub);
                if (dist != null) {
                    model.setClassDistribution(classKey, dist);
                }
            }
        }
    }

    private static void buildQueueSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys, List<PendingTransitionCondition> pendingTransitionConditions) {
        ParameterDescriptor retrialDistributions = null;

        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            String name = param.getName();
            if ("size".equals(name)) {
                Integer capacity = parseInteger(getParamValue(param));
                if (capacity != null) model.setStationQueueCapacity(stationKey, capacity);
            } else if ("FCFSstrategy".equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    if ("pollingKValue".equals(sub.getName())) {
                        Integer kVal = parseInteger(getParamValue(sub));
                        if (kVal != null) model.setStationPollingServerKValue(stationKey, kVal);
                    }
                }
            } else if ("retrialDistributions".equals(name)) {
                retrialDistributions = param;
            } else if ("Impatience".equals(name)) {
                parseAndSetImpatienceParameters(param, stationKey, model, classKeys);
            } else if ("enablingConditions".equals(name) || "inhibitingConditions".equals(name) || "resourceConditions".equals(name)) {
                collectTransitionConditions(param, stationKey, classKeys, model, pendingTransitionConditions);
            } else if (QUEUE_PUT_STRATEGY.equals(param.getClassPath())) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    applyQueuePutStrategy(sub.getClassPath(), stationKey, resolveClassKey(sub, param, classKeys, model), model);
                }
            }
        }

        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if ("dropStrategies".equals(param.getName())) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    if (classKey == null) continue;
                    String ruleVal = getParamValue(sub);
                    String mapped = mapDropRule(ruleVal);
                    if (mapped != null) {
                        model.setDropRule(stationKey, classKey, mapped);
                        if ("retrial".equalsIgnoreCase(ruleVal) && retrialDistributions != null) {
                            for (ParameterDescriptor retSub : safeList(retrialDistributions.getSubParameters())) {
                                if (classKey.equals(resolveClassKey(retSub, retrialDistributions, classKeys, model))) {
                                    Object dist = parseServiceStrategy(retSub);
                                    if (dist != null) model.setRetrialDistribution(stationKey, classKey, dist);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void buildDelaySection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys, List<PendingTransitionCondition> pendingTransitionConditions) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            String name = param.getName();
            if ("enablingConditions".equals(name) || "inhibitingConditions".equals(name) || "resourceConditions".equals(name)) {
                collectTransitionConditions(param, stationKey, classKeys, model, pendingTransitionConditions);
            } else if (SERVICE_STRATEGY.equals(param.getClassPath()) || "ServiceStrategy".equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    Object dist = parseServiceStrategy(sub);
                    if (classKey != null && dist != null) model.setServiceTimeDistribution(stationKey, classKey, dist);
                }
            }
        }
    }

    private static void buildServerSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys) {
        applySectionStationStrategy(sec.getClassName(), stationKey, model, classKeys);
        List<String> serverNames = new ArrayList<>();
        List<Integer> serversPerServerType = new ArrayList<>();
        List<List<Boolean>> serverCompatibilities = new ArrayList<>();
        List<Object> heterogeneousServiceStrategies = new ArrayList<>();

        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            String name = param.getName();
            String classpath = param.getClassPath();

            if ("maxJobs".equals(name)) {
                Integer servers = parseInteger(getParamValue(param));
                if (servers != null) model.setStationNumberOfServers(stationKey, servers);
            } else if ("maxRunning".equals(name)) {
                Integer running = parseInteger(getParamValue(param));
                if (running != null) model.setStationMaxRunningJobs(stationKey, running);
            } else if (SERVER_FEATURE_SWITCHOVER_STRATEGIES.equals(name) || SERVER_FEATURE_SETUP_STRATEGIES.equals(name)) {
                if (model.getStationQueueStrategy(stationKey) == STATION_QUEUE_STRATEGY_POLLING) {
                    for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                        Object classKey = resolveClassKey(sub, param, classKeys, model);
                        Object dist = parseServiceStrategy(sub);
                        if (classKey != null && dist != null) model.setPollingSwitchoverDistribution(stationKey, classKey, dist);
                    }
                } else {
                    model.setSwitchoverTimesEnabled(stationKey, Boolean.TRUE);
                    for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                        Object fromClassKey = resolveClassKey(sub, param, classKeys, model);
                        for (ParameterDescriptor toSub : safeList(sub.getSubParameters())) {
                            Object toClassKey = resolveClassKey(toSub, sub, classKeys, model);
                            Object dist = parseServiceStrategy(toSub);
                            if (fromClassKey != null && toClassKey != null && dist != null) {
                                if (SERVER_FEATURE_SETUP_STRATEGIES.equals(name)) model.setSetupTimeDistribution(stationKey, fromClassKey, dist);
                                else model.setSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey, dist);
                            }
                        }
                    }
                }
            } else if (SERVER_FEATURE_DELAY_OFF_STRATEGIES.equals(name)) {
                model.setDelayOffTimesEnabled(stationKey, Boolean.TRUE);
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    for (ParameterDescriptor distParam : safeList(sub.getSubParameters())) {
                        Object dist = parseServiceStrategy(distParam);
                        if (classKey != null && dist != null) model.setDelayOffTimeDistribution(stationKey, classKey, dist);
                    }
                }
            } else if (SERVICE_STRATEGY.equals(classpath)) {
                List<ParameterDescriptor> subs = safeList(param.getSubParameters());
                if (subs.size() > 0 && !hasText(subs.get(0).getRefClass())) {
                    for (ParameterDescriptor sub : subs) heterogeneousServiceStrategies.add(parseServiceStrategy(sub));
                } else {
                    for (ParameterDescriptor sub : subs) {
                        Object classKey = resolveClassKey(sub, param, classKeys, model);
                        Object dist = parseServiceStrategy(sub);
                        if (classKey != null && dist != null) model.setServiceTimeDistribution(stationKey, classKey, dist);
                    }
                }
            } else if (PS_STRATEGY.equals(classpath)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    if (classKey != null) applyPsQueueStrategy(sub.getClassPath(), stationKey, classKey, model);
                }
            } else if ("serviceWeights".equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    Double weight = parseDouble(getParamValue(sub));
                    if (classKey != null && weight != null) model.setServiceWeight(stationKey, classKey, weight);
                }
            } else if ("PSPriorityUsed".equals(name)) {
                if (Boolean.TRUE.equals(parseBoolean(getParamValue(param)))) model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY);
            } else if (SERVER_FEATURE_CLASS_PARALLELISM.equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    Integer num = parseInteger(getParamValue(sub));
                    if (classKey != null && num != null) model.setServerNumRequired(stationKey, classKey, num);
                }
            } else if (SERVER_FEATURE_SERVER_NAMES.equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) serverNames.add(getParamValue(sub));
            } else if (SERVER_FEATURE_SERVERS_PER_SERVER_TYPE.equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) serversPerServerType.add(parseInteger(getParamValue(sub)));
            } else if (SERVER_FEATURE_SERVER_COMPATIBILITIES.equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    if ("serverTypesCompatibilities".equals(sub.getName())) {
                        List<Boolean> compats = new ArrayList<>();
                        for (ParameterDescriptor ssub : safeList(sub.getSubParameters())) compats.add(parseBoolean(getParamValue(ssub)));
                        serverCompatibilities.add(compats);
                    }
                }
            } else if (SERVER_FEATURE_VACATION_STRATEGY.equals(name)) {
                model.setStationVacationPolicy(stationKey, STATION_VACATION_POLICY_NONE);
                VacationParameters vp = model.getStationVacationParameters(stationKey);
                List<ParameterDescriptor> subps = safeList(param.getSubParameters());
                if (classpath != null && classpath.endsWith("VacationWhenIdleStrategy") && subps.size() >= 3) {
                    String type = getParamValue(subps.get(0));
                    if ("0".equals(type)) model.setStationVacationPolicy(stationKey, STATION_VACATION_POLICY_SINGLE);
                    else if ("1".equals(type)) model.setStationVacationPolicy(stationKey, STATION_VACATION_POLICY_MULTIPLE);
                    Object vt = parseServiceStrategy(param);
                    if (vt instanceof Distribution) vp.setVacationTimeDistribution((Distribution) vt);
                } else if (classpath != null && classpath.endsWith("BernoulliVacationStrategy") && subps.size() >= 3) {
                    model.setStationVacationPolicy(stationKey, STATION_VACATION_POLICY_BERNOULLI);
                    Double p = parseDouble(getParamValue(subps.get(0)));
                    if (p != null) vp.setBernoulliP(p);
                    Object vt = parseServiceStrategy(param);
                    if (vt instanceof Distribution) vp.setVacationTimeDistribution((Distribution) vt);
                }
            } else if ("RoutingStrategy".equals(name)) {
                buildRouterParameter(param, stationKey, model, classKeys, stationKeys);
            }
        }

        for (int i = 0; i < serverNames.size(); i++) {
            model.addServerType(stationKey, serverNames.get(i), serversPerServerType.get(i), serverCompatibilities.get(i), true);
        }
        if (!heterogeneousServiceStrategies.isEmpty()) {
            model.setHeterogeneousServersEnabled(stationKey, true);
            List<Object> allClassKeys = model.getClassKeys();
            List<Object> serverKeys = model.getServerTypeKeys();
            for (int s = 0; s < serverNames.size(); s++) {
                for (int c = 0; c < allClassKeys.size(); c++) {
                    int index = s * allClassKeys.size() + c;
                    if (index < heterogeneousServiceStrategies.size()) {
                        model.setServiceTimeDistribution(stationKey, allClassKeys.get(c), serverKeys.get(s), heterogeneousServiceStrategies.get(index));
                    }
                }
            }
        }
    }

    private static void buildRouterSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if ("RoutingStrategy".equals(param.getName()) || hasText(param.getClassPath())) {
                buildRouterParameter(param, stationKey, model, classKeys, stationKeys);
            }
        }
    }

    private static void buildRouterParameter(ParameterDescriptor routingRoot, Object stationKey, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys) {
        for (ParameterDescriptor classRouting : safeList(routingRoot.getSubParameters())) {
            Object classKey = resolveClassKey(classRouting, routingRoot, classKeys, model);
            if (classKey == null) continue;

            RoutingStrategy strategy = instantiateRoutingStrategy(resolveRoutingClassPath(classRouting));
            if (strategy == null) continue;

            model.setRoutingStrategy(stationKey, classKey, strategy);

            if (strategy instanceof ProbabilityRouting) {
                ProbabilityRouting pr = (ProbabilityRouting) model.getRoutingStrategy(stationKey, classKey);
                Map<String, Double> entries = extractStationToDoubleMap(classRouting);
                for (Map.Entry<String, Double> entry : entries.entrySet()) {
                    Object targetStationKey = getStationKey(entry.getKey(), stationKeys, model);
                    if (targetStationKey != null) pr.getValues().put(targetStationKey, entry.getValue());
                }
            } else if (strategy instanceof WeightedRoundRobinRouting) {
                WeightedRoundRobinRouting wrr = (WeightedRoundRobinRouting) model.getRoutingStrategy(stationKey, classKey);
                Map<String, Integer> weights = extractStationToIntegerMap(classRouting);
                for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                    Object targetStationKey = getStationKey(entry.getKey(), stationKeys, model);
                    if (targetStationKey != null) wrr.getWeights().put(targetStationKey, entry.getValue());
                }
            } else if (strategy instanceof LoadDependentRouting) {
                LoadDependentRouting ldr = (LoadDependentRouting) model.getRoutingStrategy(stationKey, classKey);
                Map<Integer, Map<String, Double>> ranges = extractLoadDependentMap(classRouting);
                for (Map.Entry<Integer, Map<String, Double>> fromEntry : ranges.entrySet()) {
                    for (Map.Entry<String, Double> targetEntry : fromEntry.getValue().entrySet()) {
                        Object targetStationKey = getStationKey(targetEntry.getKey(), stationKeys, model);
                        if (targetStationKey != null) ldr.addEmpiricalEntry(fromEntry.getKey(), targetStationKey, targetEntry.getValue());
                    }
                }
            } else if (strategy instanceof PowerOfKRouting) {
                PowerOfKRouting pok = (PowerOfKRouting) strategy;
                for (ParameterDescriptor sub : safeList(classRouting.getSubParameters())) {
                    if ("k".equals(sub.getName())) {
                        Integer kVal = parseInteger(getParamValue(sub));
                        if (kVal != null) pok.setK(kVal);
                    } else if ("withMemory".equals(sub.getName())) {
                        Boolean mem = parseBoolean(getParamValue(sub));
                        if (mem != null) pok.setWithMemory(mem);
                    }
                }
            } else if (strategy instanceof ClassSwitchRouting) {
                ClassSwitchRouting classSwitch = (ClassSwitchRouting) model.getRoutingStrategy(stationKey, classKey);
                for (ClassSwitchBranch branch : extractClassSwitchBranches(classRouting)) {
                    Object targetStationKey = getStationKey(branch.stationName, stationKeys, model);
                    if (targetStationKey == null) continue;
                    if (branch.probability != null) {
                        classSwitch.getValues().put(targetStationKey, branch.probability);
                    }

                    Map<Object, Double> outPath = new HashMap<Object, Double>();
                    for (Map.Entry<String, Double> classEntry : branch.classSwitchProbabilities.entrySet()) {
                        Object targetClassKey = getClassKey(classEntry.getKey(), classKeys, model);
                        if (targetClassKey != null && classEntry.getValue() != null) {
                            outPath.put(targetClassKey, classEntry.getValue());
                        }
                    }
                    if (!outPath.isEmpty()) {
                        classSwitch.getOutPaths().put(targetStationKey, outPath);
                    }
                }
            }
        }
    }

    private static void buildForkSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys, Map<StationClassPair, Map<String, OutPath>> pendingProbabilityForkOutPaths, Map<StationClassPair, Map<String, OutPath>> pendingClassSwitchForkOutPaths) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            String name = param.getName();
            if ("jobsPerLink".equals(name)) {
                Integer jobs = parseInteger(getParamValue(param));
                if (jobs != null) model.setStationNumberOfServers(stationKey, jobs);
            } else if ("block".equals(name)) {
                Integer block = parseInteger(getParamValue(param));
                if (block != null) model.setForkBlock(stationKey, block);
            } else if ("isSimplifiedFork".equals(name)) {
                Boolean isSimplified = parseBoolean(getParamValue(param));
                if (isSimplified != null) model.setIsSimplifiedFork(stationKey, isSimplified);
            } else if ("ForkStrategy".equals(name)) {
                for (ParameterDescriptor classFork : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(classFork, param, classKeys, model);
                    if (classKey == null) continue;

                    ForkStrategy fs = null;
                    for (ForkStrategy s : ForkStrategy.findAll()) {
                        if (robustMatch(classFork.getClassPath(), s.getClassPath(), s.getClass())) { fs = s.clone(); break; }
                    }
                    if (fs != null) {
                        model.setForkStrategy(stationKey, classKey, fs);
                        List<ParameterDescriptor> arrayWrapper = safeList(classFork.getSubParameters());
                        if (arrayWrapper.isEmpty()) continue;
                        List<ParameterDescriptor> entries = safeList(arrayWrapper.get(0).getSubParameters());

                        if (fs instanceof ProbabilitiesFork) {
                            Map<String, OutPath> stationOutPaths = new HashMap<String, OutPath>();
                            for (ParameterDescriptor entry : entries) {
                                List<ParameterDescriptor> vals = safeList(entry.getSubParameters());
                                if (vals.size() >= 1) {
                                    ParameterDescriptor outUnitProbability = findDirectSubParameter(entry, "outUnitProbability");
                                    if (outUnitProbability == null && !vals.isEmpty()) outUnitProbability = vals.get(0);

                                    String stationName = null;
                                    Double branchProbability = null;
                                    for (ParameterDescriptor outUnitValue : safeList(outUnitProbability != null ? outUnitProbability.getSubParameters() : null)) {
                                        if ("stationName".equals(outUnitValue.getName())) stationName = getParamValue(outUnitValue);
                                        else if ("probability".equals(outUnitValue.getName())) branchProbability = parseDouble(getParamValue(outUnitValue));
                                    }
                                    if (!hasText(stationName) && !vals.isEmpty()) stationName = getParamValue(vals.get(0));
                                    if (branchProbability == null && vals.size() >= 2) branchProbability = parseDouble(getParamValue(vals.get(1)));

                                    Object targetKey = getStationKey(stationName, stationKeys, model);
                                    if (targetKey != null) {
                                        OutPath op = new OutPath();
                                        op.setProbability(branchProbability);

                                        Map<Object, Object> tempMap = new HashMap<>();
                                        ParameterDescriptor jobsPerLink = findDirectSubParameter(entry, "JobsPerLinkDis");
                                        if (jobsPerLink == null && vals.size() > 2) jobsPerLink = vals.get(2);
                                        if (jobsPerLink != null) {
                                            collectIntegerDoubleEmpiricalEntries(jobsPerLink, tempMap);
                                        }
                                        if (!tempMap.isEmpty()) {
                                            op.setOutParameters(tempMap);
                                        }
                                        stationOutPaths.put(stationName, op);
                                    }
                                }
                            }
                            if (!stationOutPaths.isEmpty()) {
                                pendingProbabilityForkOutPaths.put(new StationClassPair(stationKey, classKey), stationOutPaths);
                            }
                        } else if (fs instanceof CombFork) {
                            Map<Object, Double> combMap = (Map<Object, Double>) fs.getOutDetails();
                            for (ParameterDescriptor entry : entries) {
                                List<ParameterDescriptor> vals = safeList(entry.getSubParameters());
                                if (vals.size() >= 2) {
                                    combMap.put(getParamValue(vals.get(0)), parseDouble(getParamValue(vals.get(1))));
                                }
                            }
                        } else if (fs instanceof ClassSwitchFork || fs instanceof MultiBranchClassSwitchFork) {
                            Map<String, OutPath> stationOutPaths = new HashMap<String, OutPath>();
                            for (ParameterDescriptor entry : entries) {
                                List<ParameterDescriptor> vals = safeList(entry.getSubParameters());
                                if (vals.size() >= 3) {
                                    String stationName = getParamValue(vals.get(0));
                                    if (!hasText(stationName)) continue;

                                    OutPath op = new OutPath();
                                    List<String> cStr = new ArrayList<String>();
                                    List<Integer> nums = new ArrayList<Integer>();
                                    collectNamedValues(vals.get(1), "class", cStr);
                                    collectNamedIntegerValues(vals.get(2), "numberOfJobs", nums);

                                    Map<Object, Object> tempMap = new HashMap<Object, Object>();
                                    for (int k = 0; k < cStr.size() && k < nums.size(); k++) {
                                        Object outClassKey = model.getClassByName(cStr.get(k));
                                        if (outClassKey != null && nums.get(k) != null) tempMap.put(outClassKey, nums.get(k));
                                    }
                                    op.setOutParameters(tempMap);
                                    stationOutPaths.put(stationName, op);
                                }
                            }
                            if (!stationOutPaths.isEmpty()) {
                                pendingClassSwitchForkOutPaths.put(new StationClassPair(stationKey, classKey), stationOutPaths);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void applyPendingProbabilityForkOutPaths(CommonModel model, Map<String, Object> stationKeys, Map<StationClassPair, Map<String, OutPath>> pendingProbabilityForkOutPaths) {
        for (Map.Entry<StationClassPair, Map<String, OutPath>> pendingEntry : pendingProbabilityForkOutPaths.entrySet()) {
            StationClassPair pair = pendingEntry.getKey();
            Object strategy = model.getForkStrategy(pair.stationKey, pair.classKey);
            if (!(strategy instanceof ProbabilitiesFork)) continue;

            Map<Object, OutPath> outPaths = (Map<Object, OutPath>) ((ProbabilitiesFork) strategy).getOutDetails();
            for (Map.Entry<String, OutPath> outEntry : pendingEntry.getValue().entrySet()) {
                Object targetStationKey = getStationKey(outEntry.getKey(), stationKeys, model);
                if (targetStationKey != null) {
                    outPaths.put(targetStationKey, (OutPath) outEntry.getValue().clone());
                }
            }
        }
    }

    private static void applyPendingClassSwitchForkOutPaths(CommonModel model, Map<String, Object> stationKeys, Map<StationClassPair, Map<String, OutPath>> pendingClassSwitchForkOutPaths) {
        for (Map.Entry<StationClassPair, Map<String, OutPath>> pendingEntry : pendingClassSwitchForkOutPaths.entrySet()) {
            StationClassPair pair = pendingEntry.getKey();
            Object strategy = model.getForkStrategy(pair.stationKey, pair.classKey);
            if (!(strategy instanceof ClassSwitchFork) && !(strategy instanceof MultiBranchClassSwitchFork)) continue;

            Map<Object, OutPath> outPaths = (Map<Object, OutPath>) ((ForkStrategy) strategy).getOutDetails();
            for (Map.Entry<String, OutPath> outEntry : pendingEntry.getValue().entrySet()) {
                Object targetStationKey = getStationKey(outEntry.getKey(), stationKeys, model);
                if (targetStationKey != null) {
                    outPaths.put(targetStationKey, (OutPath) outEntry.getValue().clone());
                }
            }
        }
    }

    private static void reapplyRouterSectionsPass6(SimDescriptor descriptor, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys) {
        for (NodeDescriptor node : safeList(descriptor.getNodes())) {
            Object stationKey = getStationKey(node.getName(), stationKeys, model);
            if (stationKey == null) continue;
            for (SectionDescriptor section : safeList(node.getSections())) {
                String className = section.getClassName();
                if (CLASSNAME_ROUTER.equals(className) || CLASSNAME_LINKAGE.equals(className)) {
                    buildRouterSection(section, stationKey, model, classKeys, stationKeys);
                }
            }
        }
    }

    private static void buildJoinSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys) {
        ParameterDescriptor param = sec.getParameters().isEmpty() ? null : sec.getParameters().get(0);
        if (param != null) {
            for (ParameterDescriptor classJoin : safeList(param.getSubParameters())) {
                Object classKey = resolveClassKey(classJoin, param, classKeys, model);
                if (classKey == null) continue;

                JoinStrategy js = null;
                for (JoinStrategy s : JoinStrategy.findAll()) {
                    if (robustMatch(classJoin.getClassPath(), s.getClassPath(), s.getClass())) { js = s.clone(); break; }
                }
                if (js != null) {
                    model.setJoinStrategy(stationKey, classKey, js);
                    if (js instanceof PartialJoin) {
                        for (ParameterDescriptor sub : safeList(classJoin.getSubParameters())) {
                            if ("numRequired".equals(sub.getName()) || "numRequired".equals(sub.getRefClass())) {
                                Integer req = parseInteger(getParamValue(sub));
                                if (req != null) ((PartialJoin)js).setRequiredNum(req);
                            }
                        }
                    } else if (js instanceof GuardJoin) {
                        Map<Object, Integer> mix = new HashMap<>();
                        List<String> guardClasses = new ArrayList<>();
                        List<Integer> numbers = new ArrayList<>();
                        for (ParameterDescriptor sub : safeList(classJoin.getSubParameters())) {
                            String subName = sub.getName() != null ? sub.getName() : sub.getRefClass();
                            if ("Classes".equals(subName)) {
                                for (ParameterDescriptor c : safeList(sub.getSubParameters())) guardClasses.add(getParamValue(c));
                            } else if ("Numbers".equals(subName)) {
                                for (ParameterDescriptor n : safeList(sub.getSubParameters())) numbers.add(parseInteger(getParamValue(n)));
                            }
                        }
                        for (int k = 0; k < guardClasses.size() && k < numbers.size(); k++) {
                            Object cKey = model.getClassByName(guardClasses.get(k));
                            if (cKey != null && numbers.get(k) != null) mix.put(cKey, numbers.get(k));
                        }
                        ((GuardJoin)js).setGuard(mix);
                    }
                }
            }
        } else {
            for (Object classKey : classKeys.values()) model.setJoinStrategy(stationKey, classKey, JoinStrategy.findAll()[0].clone());
        }
    }

    private static void buildSemaphoreSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys) {
        ParameterDescriptor param = sec.getParameters().isEmpty() ? null : sec.getParameters().get(0);
        if (param != null) {
            for (ParameterDescriptor classSem : safeList(param.getSubParameters())) {
                Object classKey = resolveClassKey(classSem, param, classKeys, model);
                if (classKey == null) continue;

                SemaphoreStrategy ss = null;
                for (SemaphoreStrategy s : SemaphoreStrategy.findAll()) {
                    if (robustMatch(classSem.getClassPath(), s.getClassPath(), s.getClass())) { ss = s.clone(); break; }
                }
                if (ss != null) {
                    model.setSemaphoreStrategy(stationKey, classKey, ss);
                    Object modelStrategy = model.getSemaphoreStrategy(stationKey, classKey);
                    if (modelStrategy instanceof NormalSemaphore) {
                        Integer th = findNamedInteger(classSem, "threshold");
                        if (th != null) ((NormalSemaphore) modelStrategy).setThreshold(th);
                    }
                }
            }
        } else {
            for (Object classKey : classKeys.values()) model.setSemaphoreStrategy(stationKey, classKey, SemaphoreStrategy.findAll()[0].clone());
        }
    }

    private static void buildStorageSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            String name = param.getName();
            if ("totalCapacity".equals(name)) {
                Integer cap = parseInteger(getParamValue(param));
                if (cap != null) model.setStationQueueCapacity(stationKey, cap);
            } else if ("capacities".equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    Integer cap = parseInteger(getParamValue(sub));
                    if (classKey != null && cap != null) model.setQueueCapacity(stationKey, classKey, cap);
                }
            } else if ("dropRules".equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    String rule = mapDropRule(getParamValue(sub));
                    if (classKey != null && rule != null) model.setDropRule(stationKey, classKey, rule);
                }
            } else if ("putStrategies".equals(name)) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = resolveClassKey(sub, param, classKeys, model);
                    if (classKey != null) applyQueuePutStrategy(sub.getClassPath(), stationKey, classKey, model);
                }
            }
        }
    }

    private static void buildEnablingSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys, List<PendingTransitionCondition> pendingTransitionConditions) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if ("enablingConditions".equals(param.getName()) || "inhibitingConditions".equals(param.getName()) || "resourceConditions".equals(param.getName())) {
                collectTransitionConditions(param, stationKey, classKeys, model, pendingTransitionConditions);
            }
        }
    }

    private static void buildTimingSection(SectionDescriptor sec, Object stationKey, CommonModel model) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            String name = param.getName();
            if ("modeNames".equals(name)) {
                model.deleteTransitionMode(stationKey, 0);
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) model.addTransitionMode(stationKey, getParamValue(sub));
            } else if ("numbersOfServers".equals(name)) {
                int i = 0;
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Integer num = parseInteger(getParamValue(sub));
                    if (num != null) model.setNumberOfServers(stationKey, i, num);
                    i++;
                }
            } else if ("timingStrategies".equals(name)) {
                int i = 0;
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object strat = parseServiceStrategy(sub);
                    if (strat != null) model.setFiringTimeDistribution(stationKey, i, strat);
                    i++;
                }
            } else if ("firingPriorities".equals(name)) {
                int i = 0;
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Integer prio = parseInteger(getParamValue(sub));
                    if (prio != null) model.setFiringPriority(stationKey, i, prio);
                    i++;
                }
            } else if ("firingWeights".equals(name)) {
                int i = 0;
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Double weight = parseDouble(getParamValue(sub));
                    if (weight != null) model.setFiringWeight(stationKey, i, weight);
                    i++;
                }
            }
        }
    }

    private static void buildFiringSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys, Map<String, Object> stationKeys, List<PendingFiringOutcome> pendingFiringOutcomes) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if ("firingOutcomes".equals(param.getName())) {
                List<ParameterDescriptor> outcomes = safeList(param.getSubParameters());
                for (int i = 0; i < outcomes.size(); i++) {
                    List<ParameterDescriptor> outElements = safeList(outcomes.get(i).getSubParameters());
                    if (outElements.isEmpty()) continue;
                    for (ParameterDescriptor vector : safeList(outElements.get(0).getSubParameters())) {
                        List<ParameterDescriptor> vecElements = safeList(vector.getSubParameters());
                        if (vecElements.size() < 2) continue;
                        String targetStationName = getParamValue(vecElements.get(0));
                        if (!hasText(targetStationName)) continue;
                        for (ParameterDescriptor entry : safeList(vecElements.get(1).getSubParameters())) {
                            Object classKey = resolveClassKey(entry, vecElements.get(1), classKeys, model);
                            Integer val = parseInteger(getParamValue(entry));
                            if (classKey != null && val != null) {
                                pendingFiringOutcomes.add(new PendingFiringOutcome(stationKey, i, targetStationName, classKey, val.intValue()));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void buildLoggerSection(SectionDescriptor sec, Object stationKey, CommonModel model) {
        LoggerParameters logParams = new LoggerParameters();
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            String name = param.getName();
            String val = getParamValue(param);
            if (XML_LOG_FILENAME.equals(name)) logParams.name = val;
            else if (XML_LOG_FILEPATH.equals(name)) logParams.path = MacroReplacer.replace(model.getLoggingGlbParameter("path"));
            else if (XML_LOG_B_EXECTIMESTAMP.equals(name)) logParams.boolExecTimestamp = parseBoolean(val);
            else if (XML_LOG_B_LOGGERNAME.equals(name)) logParams.boolLoggername = parseBoolean(val);
            else if (XML_LOG_B_TIMESTAMP.equals(name)) logParams.boolTimeStamp = parseBoolean(val);
            else if (XML_LOG_B_JOBID.equals(name)) logParams.boolJobID = parseBoolean(val);
            else if (XML_LOG_B_JOBCLASS.equals(name)) logParams.boolJobClass = parseBoolean(val);
            else if (XML_LOG_B_TIMESAMECLS.equals(name)) logParams.boolTimeSameClass = parseBoolean(val);
            else if (XML_LOG_B_TIMEANYCLS.equals(name)) logParams.boolTimeAnyClass = parseBoolean(val);
        }
        model.setLoggingParameters(stationKey, logParams);
    }

    private static void buildClassSwitchSection(SectionDescriptor sec, Object stationKey, CommonModel model, Map<String, Object> classKeys) {
        for (ParameterDescriptor matrix : safeList(sec.getParameters())) {
            for (ParameterDescriptor row : safeList(matrix.getSubParameters())) {
                Object inClassKey = resolveClassKey(row, matrix, classKeys, model);
                if (inClassKey == null) continue;
                for (ParameterDescriptor cell : safeList(row.getSubParameters())) {
                    Object outClassKey = resolveClassKey(cell, row, classKeys, model);
                    Float value = parseFloat(getParamValue(cell));
                    if (outClassKey != null && value != null) model.setClassSwitchMatrix(stationKey, inClassKey, outClassKey, value.floatValue());
                }
            }
        }
    }

    private static Object parseServiceStrategy(ParameterDescriptor param) {
        if (param == null || param.getSubParameters().isEmpty()) {
            if (param == null || param.getClassPath() == null || (!param.getClassPath().endsWith("ZeroStrategy") && !param.getClassPath().endsWith("DisabledStrategy"))) {
                return null;
            }
        }

        String classPath = resolveServiceStrategyClassPath(param);
        if (!hasText(classPath)) return null;

        if (classPath.endsWith("ZeroStrategy")) return new ZeroStrategy();
        if (classPath.endsWith("DisabledStrategy")) return new DisabledStrategy();
        if (classPath.endsWith("LDStrategy") || classPath.endsWith("LoadDependentStrategy")) {
            LDStrategy lds = new LDStrategy();
            for (ParameterDescriptor rangeParam : extractLdRangeParameters(param)) {
                Integer from = findDirectNamedInteger(rangeParam, "from");
                String mean = findDirectNamedValue(rangeParam, "function");
                Object distrObj = parseServiceStrategy(rangeParam);
                if (from != null && distrObj instanceof Distribution) {
                    Object key = (from.intValue() == 1) ? lds.getAllRanges()[0] : lds.addRange();
                    if (from.intValue() != 1) lds.setRangeFrom(key, from);
                    lds.setRangeDistributionNoCheck(key, (Distribution)distrObj);
                    lds.setRangeDistributionMeanNoCheck(key, mean);
                }
            }
            return lds;
        }

        Distribution dist = null;
        for (Distribution d : Distribution.findAll()) {
            if (robustMatch(classPath, d.getClassPath(), d.getClass())) { dist = d.clone(); break; }
        }
        if (dist == null) {
            for (Distribution d : Distribution.findAllDiscrete()) {
                if (robustMatch(classPath, d.getClassPath(), d.getClass())) { dist = d.clone(); break; }
            }
        }
        if (dist == null) return null;

        applyDistributionParameters(dist, param);
        dist.updateCM();
        return dist;
    }

    private static String resolveServiceStrategyClassPath(ParameterDescriptor param) {
        if (param == null) return null;
        if (isKnownServiceClassPath(param.getClassPath())) return param.getClassPath();
        for (ParameterDescriptor child : safeList(param.getSubParameters())) {
            String childPath = resolveServiceStrategyClassPath(child);
            if (isKnownServiceClassPath(childPath)) return childPath;
        }
        return null;
    }

    private static void applyDistributionParameters(Distribution dist, ParameterDescriptor node) {
        if (dist == null || node == null) return;

        String name = node.getName();
        if (hasText(name) && !"distrPar".equals(name) && dist.getParameter(name) != null) {
            if (node.isArray()) {
                List<ParameterDescriptor> vectors = safeList(node.getSubParameters());
                Object[][] paramValue = new Object[vectors.size()][];
                for (int j = 0; j < vectors.size(); j++) {
                    List<ParameterDescriptor> entries = safeList(vectors.get(j).getSubParameters());
                    paramValue[j] = new Object[entries.size()];
                    for (int k = 0; k < entries.size(); k++) {
                        Object parsed = dist.getParameter(name).parseValue(getParamValue(entries.get(k)));
                        if (parsed == null) parsed = dist.getParameter(name).parseValue("0");
                        paramValue[j][k] = parsed;
                    }
                }
                dist.getParameter(name).setValue(paramValue);
            } else if (DISTR_CONTAINER.equals(node.getClassPath())) {
                Object nested = parseDistributionFromContainer(node);
                if (nested != null) dist.getParameter(name).setValue(nested);
            } else {
                String val = getParamValue(node);
                if (val != null) dist.getParameter(name).setValue(val);
            }
        }

        for (ParameterDescriptor child : safeList(node.getSubParameters())) {
            applyDistributionParameters(dist, child);
        }
    }

    private static Object parseDistributionFromContainer(ParameterDescriptor container) {
        if (container == null) return null;
        ParameterDescriptor synthetic = new ParameterDescriptor();
        synthetic.setSubParameters(new ArrayList<ParameterDescriptor>(safeList(container.getSubParameters())));
        return parseServiceStrategy(synthetic);
    }

    private static List<ParameterDescriptor> extractLdRangeParameters(ParameterDescriptor root) {
        List<ParameterDescriptor> result = new ArrayList<ParameterDescriptor>();
        collectLdRangeParameters(root, result);
        return result;
    }

    private static void collectLdRangeParameters(ParameterDescriptor node, List<ParameterDescriptor> output) {
        if (node == null || output == null) return;
        if ("LDParameter".equals(node.getName()) && findDirectNamedInteger(node, "from") != null) {
            output.add(node);
            return;
        }
        for (ParameterDescriptor child : safeList(node.getSubParameters())) {
            collectLdRangeParameters(child, output);
        }
    }

    private static boolean isKnownServiceClassPath(String classPath) {
        if (!hasText(classPath)) return false;
        if (classPath.endsWith("ZeroStrategy") || classPath.endsWith("DisabledStrategy") || classPath.endsWith("LDStrategy") || classPath.endsWith("LoadDependentStrategy")) {
            return true;
        }
        for (Distribution d : Distribution.findAll()) {
            if (robustMatch(classPath, d.getClassPath(), d.getClass())) return true;
        }
        for (Distribution d : Distribution.findAllDiscrete()) {
            if (robustMatch(classPath, d.getClassPath(), d.getClass())) return true;
        }
        return false;
    }

    private static void collectTransitionConditions(ParameterDescriptor param, Object stationKey, Map<String, Object> classKeys, CommonModel model, List<PendingTransitionCondition> pendingTransitionConditions) {
        List<ParameterDescriptor> conditions = safeList(param.getSubParameters());
        for (int i = 0; i < conditions.size(); i++) {
            List<ParameterDescriptor> condElements = safeList(conditions.get(i).getSubParameters());
            if (condElements.isEmpty()) continue;
            for (ParameterDescriptor vector : safeList(condElements.get(0).getSubParameters())) {
                List<ParameterDescriptor> vecElements = safeList(vector.getSubParameters());
                if (vecElements.size() < 2) continue;
                String targetStationName = getParamValue(vecElements.get(0));
                if (!hasText(targetStationName)) continue;

                for (ParameterDescriptor entry : safeList(vecElements.get(1).getSubParameters())) {
                    Object classKey = resolveClassKey(entry, vecElements.get(1), classKeys, model);
                    Integer val = parseInteger(getParamValue(entry));
                    if (classKey != null && val != null) {
                        pendingTransitionConditions.add(new PendingTransitionCondition(param.getName(), stationKey, i, targetStationName, classKey, val.intValue()));
                    }
                }
            }
        }
    }

    private static void applyPendingTransitionConditions(CommonModel model, Map<String, Object> stationKeys, List<PendingTransitionCondition> pendingTransitionConditions) {
        for (PendingTransitionCondition condition : pendingTransitionConditions) {
            Object targetKey = getStationKey(condition.targetStationName, stationKeys, model);
            if (targetKey == null) continue;
            if ("enablingConditions".equals(condition.conditionType)) model.setEnablingCondition(condition.stationKey, condition.modeIndex, targetKey, condition.classKey, condition.value);
            else if ("inhibitingConditions".equals(condition.conditionType)) model.setInhibitingCondition(condition.stationKey, condition.modeIndex, targetKey, condition.classKey, condition.value);
            else if ("resourceConditions".equals(condition.conditionType)) model.setResourceCondition(condition.stationKey, condition.modeIndex, targetKey, condition.classKey, condition.value);
        }
    }

    private static void applyPendingFiringOutcomes(CommonModel model, Map<String, Object> stationKeys, List<PendingFiringOutcome> pendingFiringOutcomes) {
        for (PendingFiringOutcome outcome : pendingFiringOutcomes) {
            Object targetKey = getStationKey(outcome.targetStationName, stationKeys, model);
            if (targetKey != null) {
                model.setFiringOutcome(outcome.stationKey, outcome.modeIndex, targetKey, outcome.classKey, outcome.value);
            }
        }
    }

    private static void parseAndSetImpatienceParameters(ParameterDescriptor param, Object stationKey, CommonModel model, Map<String, Object> classKeys) {
        for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
            Object classKey = resolveClassKey(sub, param, classKeys, model);
            if (classKey == null) continue;

            ImpatienceType impatienceType = ImpatienceType.getType(sub.getName());
            if (impatienceType == ImpatienceType.RENEGING) {
                Object distObj = parseServiceStrategy(sub);
                if (distObj instanceof Distribution) {
                    model.setImpatienceParameter(stationKey, classKey, new RenegingParameter((Distribution)distObj));
                    model.setImpatienceType(stationKey, classKey, impatienceType);
                }
            } else if (impatienceType == ImpatienceType.BALKING) {
                ParameterDescriptor ldParam = findFirstByClassPathSuffix(sub, "LoadDependentStrategy");
                Object ldObj = parseServiceStrategy(ldParam != null ? ldParam : sub);
                if (ldObj instanceof LDStrategy) {
                    BalkingParameter bp = new BalkingParameter((LDStrategy)ldObj);
                    Boolean priority = parseBoolean(findNamedValue(sub, "priorityActivated"));
                    if (priority != null) bp.setPriorityActivated(priority);
                    model.setImpatienceParameter(stationKey, classKey, bp);
                    model.setImpatienceType(stationKey, classKey, impatienceType);
                }
            } else {
                model.setImpatienceParameter(stationKey, classKey, null);
                model.setImpatienceType(stationKey, classKey, ImpatienceType.NONE);
            }
        }
    }

    private static RoutingStrategy instantiateRoutingStrategy(String classPath) {
        if (!hasText(classPath)) return null;
        if (classPath.endsWith(".ClassSwitchStrategy")) {
            classPath = classPath.substring(0, classPath.length() - "ClassSwitchStrategy".length()) + "ClassSwitchRoutingStrategy";
        }
        for (RoutingStrategy strategy : RoutingStrategy.findAll()) {
            if (robustMatch(classPath, strategy.getClassPath(), strategy.getClass())) return strategy.clone();
        }
        return null;
    }

    private static Map<String, Double> extractStationToDoubleMap(ParameterDescriptor parameter) {
        Map<String, Double> result = new HashMap<>();
        collectStationDoublePairs(parameter, result);
        return result;
    }

    private static void collectStationDoublePairs(ParameterDescriptor parameter, Map<String, Double> output) {
        if (parameter == null) return;
        List<ParameterDescriptor> children = safeList(parameter.getSubParameters());
        if (children.size() >= 2) {
            String stationName = firstNonBlank(getParamValue(children.get(0)), children.get(0).getName());
            Double probability = parseDouble(getParamValue(children.get(1)));
            if (hasText(stationName) && probability != null) output.put(stationName, probability);
        }
        for (ParameterDescriptor child : children) collectStationDoublePairs(child, output);
    }

    private static Map<String, Integer> extractStationToIntegerMap(ParameterDescriptor parameter) {
        Map<String, Integer> result = new HashMap<>();
        collectStationIntegerPairs(parameter, result);
        return result;
    }

    private static void collectStationIntegerPairs(ParameterDescriptor parameter, Map<String, Integer> output) {
        if (parameter == null) return;
        List<ParameterDescriptor> children = safeList(parameter.getSubParameters());
        if (children.size() >= 2) {
            String stationName = firstNonBlank(getParamValue(children.get(0)), children.get(0).getName());
            Integer weight = parseInteger(getParamValue(children.get(1)));
            if (hasText(stationName) && weight != null) output.put(stationName, weight);
        }
        for (ParameterDescriptor child : children) collectStationIntegerPairs(child, output);
    }

    private static Map<Integer, Map<String, Double>> extractLoadDependentMap(ParameterDescriptor parameter) {
        Map<Integer, Map<String, Double>> result = new HashMap<>();
        collectLoadDependentEntries(parameter, result);
        return result;
    }

    private static void collectLoadDependentEntries(ParameterDescriptor parameter, Map<Integer, Map<String, Double>> output) {
        if (parameter == null || output == null) return;

        Integer from = null;
        for (ParameterDescriptor child : safeList(parameter.getSubParameters())) {
            if ("from".equals(child.getName())) {
                from = parseInteger(getParamValue(child));
                break;
            }
        }
        if (from != null) {
            Map<String, Double> stationProbabilities = new HashMap<String, Double>();
            collectEmpiricalStationProbabilities(parameter, stationProbabilities);
            if (!stationProbabilities.isEmpty()) {
                output.put(from, stationProbabilities);
            }
        }

        for (ParameterDescriptor child : safeList(parameter.getSubParameters())) {
            collectLoadDependentEntries(child, output);
        }
    }

    private static void collectEmpiricalStationProbabilities(ParameterDescriptor parameter, Map<String, Double> output) {
        if (parameter == null || output == null) return;
        List<ParameterDescriptor> children = safeList(parameter.getSubParameters());

        String stationName = null;
        Double probability = null;
        for (ParameterDescriptor child : children) {
            if ("stationName".equals(child.getName())) stationName = getParamValue(child);
            else if ("probability".equals(child.getName())) probability = parseDouble(getParamValue(child));
        }
        if (hasText(stationName) && probability != null) {
            output.put(stationName, probability);
        }

        for (ParameterDescriptor child : children) {
            collectEmpiricalStationProbabilities(child, output);
        }
    }

    private static final class ClassSwitchBranch {
        private final String stationName;
        private final Double probability;
        private final Map<String, Double> classSwitchProbabilities;

        private ClassSwitchBranch(String stationName, Double probability, Map<String, Double> classSwitchProbabilities) {
            this.stationName = stationName;
            this.probability = probability;
            this.classSwitchProbabilities = classSwitchProbabilities;
        }
    }

    private static List<ClassSwitchBranch> extractClassSwitchBranches(ParameterDescriptor parameter) {
        List<ClassSwitchBranch> branches = new ArrayList<ClassSwitchBranch>();
        collectClassSwitchBranches(parameter, branches);
        return branches;
    }

    private static void collectClassSwitchBranches(ParameterDescriptor parameter, List<ClassSwitchBranch> output) {
        if (parameter == null || output == null) return;
        List<ParameterDescriptor> children = safeList(parameter.getSubParameters());

        String stationName = null;
        Double branchProbability = null;
        Map<String, Double> classProbs = new HashMap<String, Double>();
        for (ParameterDescriptor child : children) {
            if ("stationName".equals(child.getName())) stationName = getParamValue(child);
            else if ("probability".equals(child.getName())) branchProbability = parseDouble(getParamValue(child));
            else {
                collectClassProbabilityPairs(child, classProbs);
            }
        }
        if (hasText(stationName) && branchProbability != null) {
            output.add(new ClassSwitchBranch(stationName, branchProbability, classProbs));
        }

        for (ParameterDescriptor child : children) {
            collectClassSwitchBranches(child, output);
        }
    }

    private static void collectClassProbabilityPairs(ParameterDescriptor parameter, Map<String, Double> output) {
        if (parameter == null || output == null) return;
        List<ParameterDescriptor> children = safeList(parameter.getSubParameters());

        String className = null;
        Double probability = null;
        for (ParameterDescriptor child : children) {
            if ("className".equals(child.getName()) || "class".equals(child.getName())) className = getParamValue(child);
            else if ("probability".equals(child.getName()) || "classProb".equals(child.getName())) probability = parseDouble(getParamValue(child));
        }
        if (hasText(className) && probability != null) {
            output.put(className, probability);
        }

        for (ParameterDescriptor child : children) {
            collectClassProbabilityPairs(child, output);
        }
    }

    private static ParameterDescriptor findDirectSubParameter(ParameterDescriptor parent, String name) {
        if (parent == null || !hasText(name)) return null;
        for (ParameterDescriptor child : safeList(parent.getSubParameters())) {
            if (name.equals(child.getName())) return child;
        }
        return null;
    }

    private static void collectIntegerDoubleEmpiricalEntries(ParameterDescriptor parameter, Map<Object, Object> output) {
        if (parameter == null || output == null) return;
        List<ParameterDescriptor> children = safeList(parameter.getSubParameters());
        if (children.size() >= 2) {
            Integer jobs = parseInteger(getParamValue(children.get(0)));
            Double probability = parseDouble(getParamValue(children.get(1)));
            if (jobs != null && probability != null) output.put(jobs, probability);
        }
        for (ParameterDescriptor child : children) {
            collectIntegerDoubleEmpiricalEntries(child, output);
        }
    }

    private static String findNamedValue(ParameterDescriptor node, String parameterName) {
        if (node == null || !hasText(parameterName)) return null;
        if (parameterName.equals(node.getName())) {
            String value = getParamValue(node);
            if (hasText(value)) return value;
        }
        for (ParameterDescriptor child : safeList(node.getSubParameters())) {
            String value = findNamedValue(child, parameterName);
            if (hasText(value)) return value;
        }
        return null;
    }

    private static Integer findNamedInteger(ParameterDescriptor node, String parameterName) {
        return parseInteger(findNamedValue(node, parameterName));
    }

    private static String findDirectNamedValue(ParameterDescriptor node, String parameterName) {
        if (node == null || !hasText(parameterName)) return null;
        for (ParameterDescriptor child : safeList(node.getSubParameters())) {
            if (parameterName.equals(child.getName())) {
                String value = getParamValue(child);
                if (hasText(value)) return value;
            }
        }
        return null;
    }

    private static Integer findDirectNamedInteger(ParameterDescriptor node, String parameterName) {
        return parseInteger(findDirectNamedValue(node, parameterName));
    }

    private static ParameterDescriptor findFirstByClassPathSuffix(ParameterDescriptor node, String classPathSuffix) {
        if (node == null || !hasText(classPathSuffix)) return null;
        if (hasText(node.getClassPath()) && node.getClassPath().endsWith(classPathSuffix)) return node;
        for (ParameterDescriptor child : safeList(node.getSubParameters())) {
            ParameterDescriptor found = findFirstByClassPathSuffix(child, classPathSuffix);
            if (found != null) return found;
        }
        return null;
    }

    private static void collectNamedValues(ParameterDescriptor node, String parameterName, List<String> output) {
        if (node == null || output == null || !hasText(parameterName)) return;
        if (parameterName.equals(node.getName())) {
            String value = getParamValue(node);
            if (hasText(value)) output.add(value);
        }
        for (ParameterDescriptor child : safeList(node.getSubParameters())) {
            collectNamedValues(child, parameterName, output);
        }
    }

    private static void collectNamedIntegerValues(ParameterDescriptor node, String parameterName, List<Integer> output) {
        if (node == null || output == null || !hasText(parameterName)) return;
        if (parameterName.equals(node.getName())) {
            Integer value = parseInteger(getParamValue(node));
            if (value != null) output.add(value);
        }
        for (ParameterDescriptor child : safeList(node.getSubParameters())) {
            collectNamedIntegerValues(child, parameterName, output);
        }
    }

    private static String resolveRoutingClassPath(ParameterDescriptor parameter) {
        if (parameter == null) return null;
        if (hasText(parameter.getClassPath())) return parameter.getClassPath();
        for (ParameterDescriptor child : safeList(parameter.getSubParameters())) {
            if (hasText(child.getClassPath())) return child.getClassPath();
        }
        return null;
    }

    private static void applySectionStationStrategy(String sectionClassName, Object stationKey, CommonModel model, Map<String, Object> classKeys) {
        if (CLASSNAME_PSSERVER.equals(sectionClassName)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PSSERVER);
            for (Object classKey : classKeys.values()) model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_PS);
        } else if (CLASSNAME_LKPSERVER.equals(sectionClassName)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(stationKey, STATION_QUEUE_STRATEGY_POLLING_LIMITED);
        } else if (CLASSNAME_GATEDPSERVER.equals(sectionClassName)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(stationKey, STATION_QUEUE_STRATEGY_POLLING_GATED);
        } else if (CLASSNAME_EXHAUSTIVEPSERVER.equals(sectionClassName)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(stationKey, STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);
        }
    }

    private static void applyQueuePutStrategy(String strategyClassPath, Object stationKey, Object classKey, CommonModel model) {
        if (!hasText(strategyClassPath) || classKey == null) return;
        String strategy = strategyClassPath.substring(strategyClassPath.lastIndexOf('.') + 1);

        if ("TailStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_FCFS); }
        else if ("TailStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_FCFS); }
        else if ("HeadStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LCFS); }
        else if ("HeadStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LCFS); }
        else if ("RandStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_RAND); }
        else if ("RandStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_RAND); }
        else if ("SJFStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_SJF); }
        else if ("SJFStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_SJF); }
        else if ("LJFStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LJF); }
        else if ("LJFStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LJF); }
        else if ("SEPTStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_SEPT); }
        else if ("SEPTStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_SEPT); }
        else if ("LEPTStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LEPT); }
        else if ("LEPTStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LEPT); }
        else if ("FCFSPRStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_FCFS_PR); }
        else if ("FCFSPRStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_FCFS_PR); }
        else if ("LCFSPRStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LCFS_PR); }
        else if ("LCFSPRStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LCFS_PR); }
        else if ("SRPTStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_SRPT); }
        else if ("SRPTStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_SRPT); }
        else if ("EDDStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_EDD); }
        else if ("EDDStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_EDD); }
        else if ("EDFStrategy".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_EDF); }
        else if ("EDFStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_EDF); }
        else if ("TBSStrategyPriority".equals(strategy)) { model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY); model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_TBS); }
    }

    private static void applyPsQueueStrategy(String strategyClassPath, Object stationKey, Object classKey, CommonModel model) {
        if (!hasText(strategyClassPath)) return;
        String strategy = strategyClassPath.substring(strategyClassPath.lastIndexOf('.') + 1);
        if ("EPSStrategy".equals(strategy) || "EPSStrategyPriority".equals(strategy)) model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_PS);
        else if ("GPSStrategy".equals(strategy) || "GPSStrategyPriority".equals(strategy)) model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_GPS);
        else if ("DPSStrategy".equals(strategy) || "DPSStrategyPriority".equals(strategy)) model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_DPS);
        else if ("QBPSStrategy".equals(strategy) || "QBPSStrategyPriority".equals(strategy)) model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_QBPS);
    }

    private static String mapDropRule(String value) {
        if (!hasText(value)) return null;
        if ("drop".equalsIgnoreCase(value)) return FINITE_DROP;
        if ("BAS blocking".equalsIgnoreCase(value)) return FINITE_BLOCK;
        if ("waiting queue".equalsIgnoreCase(value)) return FINITE_WAITING;
        if ("retrial".equalsIgnoreCase(value)) return FINITE_RETRIAL;
        return value;
    }

    private static Boolean parseDropRule(String value) {
        if (!hasText(value)) return null;
        if ("drop".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value)) return Boolean.TRUE;
        return Boolean.FALSE;
    }

    private static void applyGlobalParameters(SimDescriptor descriptor, CommonModel model) {
        Map<String, String> attrs = descriptor.getGlobalAttributes();
        if (attrs == null) return;
        Long seed = parseLong(firstNonBlank(attrs.get(XML_A_ROOT_SEED), attrs.get("seed")));
        if (seed != null) { model.setUseRandomSeed(Boolean.FALSE); model.setSimulationSeed(seed); } else { model.setUseRandomSeed(Boolean.TRUE); }
        Double maxDuration = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_DURATION), attrs.get("duration")));
        model.setMaximumDuration(maxDuration != null ? maxDuration : Double.valueOf(-1));
        Double maxSimulated = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_SIMULATED), attrs.get("maxSimulated")));
        model.setMaxSimulatedTime(maxSimulated != null ? maxSimulated : Double.valueOf(-1));
        Double polling = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_POLLING), attrs.get("polling")));
        if (polling != null) model.setPollingInterval(polling);
        Integer maxSamples = parseInteger(firstNonBlank(attrs.get(XML_A_ROOT_MAXSAMPLES), attrs.get("maxSamples")));
        if (maxSamples != null) model.setMaxSimulationSamples(maxSamples);
        Boolean disableStatistic = parseBoolean(firstNonBlank(attrs.get(XML_A_ROOT_DISABLESTATISTIC), attrs.get("disableStatistic"), attrs.get("disableStatisticStop")));
        if (disableStatistic != null) model.setDisableStatistic(disableStatistic);
        Integer maxEvents = parseInteger(firstNonBlank(attrs.get(XML_A_ROOT_MAXEVENTS), attrs.get("maxEvents")));
        model.setMaxSimulationEvents(maxEvents != null ? maxEvents : Integer.valueOf(-1));
        String logPath = firstNonBlank(attrs.get(XML_A_ROOT_LOGPATH), attrs.get("logPath"));
        if (hasText(logPath)) {
            File dir = new File(logPath);
            if (dir.isDirectory()) model.setLoggingGlbParameter("path", dir.getAbsolutePath());
            else model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
        } else {
            model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
        }
        String logReplaceMode = firstNonBlank(attrs.get(XML_A_ROOT_LOGREPLACE), attrs.get("logReplaceMode"));
        model.setLoggingGlbParameter("autoAppend", hasText(logReplaceMode) ? logReplaceMode : Defaults.get("loggerAutoAppend"));
        String logDelimiter = firstNonBlank(attrs.get(XML_A_ROOT_LOGDELIM), attrs.get("logDelimiter"));
        model.setLoggingGlbParameter("delim", hasText(logDelimiter) ? logDelimiter : Defaults.get("loggerDelimiter"));
        String logDecimalSeparator = firstNonBlank(attrs.get(XML_A_ROOT_LOGDECIMALSEPARATOR), attrs.get("logDecimalSeparator"));
        model.setLoggingGlbParameter("decimalSeparator", hasText(logDecimalSeparator) ? logDecimalSeparator : Defaults.get("loggerDecimalSeparator"));
    }

    private static void createBaseClasses(SimDescriptor descriptor, CommonModel model, Map<String, Object> classKeys, Map<Object, String> refStationsPending) {
        for (UserClassDescriptor userClass : safeList(descriptor.getUserClasses())) {
            String name = userClass.getName();
            int type = "open".equalsIgnoreCase(userClass.getType()) ? CLASS_TYPE_OPEN : CLASS_TYPE_CLOSED;
            Integer priority = parseInteger(userClass.getPriority());
            if (priority == null) priority = Defaults.getAsInteger("classPriority");
            Double softDeadline = parseDouble(userClass.getSoftDeadline());
            if (softDeadline == null) softDeadline = Defaults.getAsDouble("classSoftDeadline");
            Integer customers = parseInteger(userClass.getCustomers());
            if (customers == null) customers = Defaults.getAsInteger("classPopulation");
            Object key = model.addClass(name, type, priority, softDeadline, customers, null);
            classKeys.put(name, key);
            refStationsPending.put(key, userClass.getReferenceSource());
        }
    }

    private static void createBaseStations(SimDescriptor descriptor, CommonModel model, Map<String, Object> stationKeys) {
        for (NodeDescriptor node : safeList(descriptor.getNodes())) {
            String stationType = determineStationType(node);
            Object key = model.addStation(node.getName(), stationType, 1, new ArrayList<ServerType>());
            stationKeys.put(node.getName(), key);
        }
    }

    private static String determineStationType(NodeDescriptor node) {
        String first = getSectionClassName(node, 0); String second = getSectionClassName(node, 1); String third = getSectionClassName(node, 2);
        if (CLASSNAME_SOURCE.equals(first) && CLASSNAME_TUNNEL.equals(second) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_SOURCE;
        if (CLASSNAME_SINK.equals(first)) return STATION_TYPE_SINK;
        if (CLASSNAME_TERMINAL.equals(first) && CLASSNAME_TUNNEL.equals(second) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_TERMINAL;
        if (CLASSNAME_QUEUE.equals(first) && CLASSNAME_TUNNEL.equals(second) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_ROUTER;
        if (CLASSNAME_QUEUE.equals(first) && CLASSNAME_DELAY.equals(second) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_DELAY;
        if (CLASSNAME_QUEUE.equals(first) && (CLASSNAME_SERVER.equals(second) || CLASSNAME_PREEMPTIVESERVER.equals(second) || CLASSNAME_PSSERVER.equals(second) || CLASSNAME_PSSERVER_PRIORITY.equals(second) || CLASSNAME_EXHAUSTIVEPSERVER.equals(second) || CLASSNAME_LKPSERVER.equals(second) || CLASSNAME_GATEDPSERVER.equals(second)) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_SERVER;
        if (CLASSNAME_QUEUE.equals(first) && CLASSNAME_TUNNEL.equals(second) && CLASSNAME_FORK.equals(third)) return STATION_TYPE_FORK;
        if (CLASSNAME_JOIN.equals(first) && CLASSNAME_TUNNEL.equals(second) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_JOIN;
        if (CLASSNAME_QUEUE.equals(first) && CLASSNAME_LOGGER.equals(second) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_LOGGER;
        if (CLASSNAME_QUEUE.equals(first) && CLASSNAME_CLASSSWITCH.equals(second) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_CLASSSWITCH;
        if (CLASSNAME_SEMAPHORE.equals(first) && CLASSNAME_TUNNEL.equals(second) && CLASSNAME_ROUTER.equals(third)) return STATION_TYPE_SEMAPHORE;
        if (CLASSNAME_JOIN.equals(first) && CLASSNAME_TUNNEL.equals(second) && CLASSNAME_FORK.equals(third)) return STATION_TYPE_SCALER;
        if (CLASSNAME_STORAGE.equals(first) && CLASSNAME_TUNNEL.equals(second) && CLASSNAME_LINKAGE.equals(third)) return STATION_TYPE_PLACE;
        if (CLASSNAME_ENABLING.equals(first) && CLASSNAME_TIMING.equals(second) && CLASSNAME_FIRING.equals(third)) return STATION_TYPE_TRANSITION;
        throw new IllegalArgumentException("Unable to determine station type for node: " + node.getName());
    }

    private static String getSectionClassName(NodeDescriptor node, int index) {
        List<SectionDescriptor> sections = node.getSections();
        if (sections == null || index < 0 || index >= sections.size()) return null;
        SectionDescriptor section = sections.get(index);
        return section != null ? section.getClassName() : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) if (hasText(value)) return value;
        return null;
    }

    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private static <T> List<T> safeList(List<T> values) { return values == null ? Collections.<T>emptyList() : values; }
    private static Long parseLong(String value) { try { return hasText(value) ? Long.valueOf(value.trim()) : null; } catch (NumberFormatException e) { return null; } }
    private static Integer parseInteger(String value) { try { return hasText(value) ? Integer.valueOf(value.trim()) : null; } catch (NumberFormatException e) { return null; } }
    private static Double parseDouble(String value) { try { return hasText(value) ? Double.valueOf(value.trim()) : null; } catch (NumberFormatException e) { return null; } }
    private static Boolean parseBoolean(String value) { return hasText(value) ? Boolean.valueOf(value.trim()) : null; }
    private static Float parseFloat(String value) { try { return hasText(value) ? Float.valueOf(value.trim()) : null; } catch (NumberFormatException e) { return null; } }
}