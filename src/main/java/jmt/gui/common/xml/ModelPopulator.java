package jmt.gui.common.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmt.framework.data.MacroReplacer;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.routingStrategies.ClassSwitchRouting;
import jmt.gui.common.routingStrategies.LoadDependentRouting;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.routingStrategies.WeightedRoundRobinRouting;
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

    private ModelPopulator() {
        // Utility class.
    }

    public static void populate(SimDescriptor descriptor, CommonModel model) {
        if (descriptor == null || model == null) {
            return;
        }

        Map<String, Object> classKeys = new HashMap<String, Object>();
        Map<String, Object> stationKeys = new HashMap<String, Object>();
        Map<String, Object> regionKeys = new HashMap<String, Object>();
        Map<Object, String> refStationsPending = new HashMap<Object, String>();

        applyGlobalParameters(descriptor, model);
        createBaseClasses(descriptor, model, classKeys, refStationsPending);
        createBaseStations(descriptor, model, stationKeys);
        resolveReferenceStationsPass3(model, stationKeys, refStationsPending);
        processNodeSectionsPass4(descriptor, model, classKeys, stationKeys);
        applyTopologyPass5(descriptor, model, classKeys, stationKeys, regionKeys);
    }

    private static void resolveReferenceStationsPass3(
            CommonModel model,
            Map<String, Object> stationKeys,
            Map<Object, String> refStationsPending) {
        for (Map.Entry<Object, String> entry : refStationsPending.entrySet()) {
            Object classKey = entry.getKey();
            String stationName = entry.getValue();
            if (!hasText(stationName)) {
                continue;
            }
            Object stationRef = resolveReferenceStation(stationName, stationKeys);
            model.setClassRefStation(classKey, stationRef);
        }
    }

    private static Object resolveReferenceStation(String stationName, Map<String, Object> stationKeys) {
        if (STATION_TYPE_FORK.equalsIgnoreCase(stationName)) {
            return STATION_TYPE_FORK;
        }
        if (STATION_TYPE_CLASSSWITCH.equalsIgnoreCase(stationName)) {
            return STATION_TYPE_CLASSSWITCH;
        }
        if (STATION_TYPE_SCALER.equalsIgnoreCase(stationName)) {
            return STATION_TYPE_SCALER;
        }
        if (STATION_TYPE_TRANSITION.equalsIgnoreCase(stationName)) {
            return STATION_TYPE_TRANSITION;
        }
        return stationKeys.get(stationName);
    }

    private static void processNodeSectionsPass4(
            SimDescriptor descriptor,
            CommonModel model,
            Map<String, Object> classKeys,
            Map<String, Object> stationKeys) {
        List<NodeDescriptor> nodes = descriptor.getNodes();
        if (nodes == null) {
            return;
        }

        for (NodeDescriptor node : nodes) {
            if (node == null) {
                continue;
            }
            Object stationKey = stationKeys.get(node.getName());
            if (stationKey == null) {
                continue;
            }

            List<SectionDescriptor> sections = node.getSections();
            if (sections == null) {
                continue;
            }

            for (SectionDescriptor section : sections) {
                if (section == null) {
                    continue;
                }
                String className = section.getClassName();
                if (!hasText(className)) {
                    continue;
                }

                if (CLASSNAME_QUEUE.equals(className)) {
                    buildQueueSection(section, stationKey, model, classKeys);
                } else if (CLASSNAME_SERVER.equals(className)
                        || CLASSNAME_PREEMPTIVESERVER.equals(className)
                        || CLASSNAME_PSSERVER.equals(className)
                        || CLASSNAME_PSSERVER_PRIORITY.equals(className)
                        || CLASSNAME_LKPSERVER.equals(className)
                        || CLASSNAME_GATEDPSERVER.equals(className)
                        || CLASSNAME_EXHAUSTIVEPSERVER.equals(className)) {
                    buildServerSection(section, stationKey, model, classKeys, stationKeys);
                } else if (CLASSNAME_DELAY.equals(className)) {
                    buildDelaySection(section, stationKey, model, classKeys);
                } else if (CLASSNAME_ROUTER.equals(className)
                        || CLASSNAME_FORK.equals(className)
                        || CLASSNAME_LINKAGE.equals(className)) {
                    buildRouterSection(section, stationKey, model, classKeys, stationKeys);
                } else if (CLASSNAME_CLASSSWITCH.equals(className)) {
                    buildClassSwitchSection(section, stationKey, model, classKeys);
                }
            }
        }
    }

    private static void applyTopologyPass5(
            SimDescriptor descriptor,
            CommonModel model,
            Map<String, Object> classKeys,
            Map<String, Object> stationKeys,
            Map<String, Object> regionKeys) {
        applyConnections(model, descriptor.getConnections(), stationKeys);
        applyBlockingRegions(model, descriptor.getBlockingRegions(), classKeys, stationKeys, regionKeys);
        applyMeasures(model, descriptor.getMeasures(), classKeys, stationKeys, regionKeys);
    }

    private static void applyConnections(
            CommonModel model,
            List<ConnectionDescriptor> connections,
            Map<String, Object> stationKeys) {
        if (connections == null) {
            return;
        }
        for (ConnectionDescriptor connection : connections) {
            if (connection == null) {
                continue;
            }
            Object sourceKey = stationKeys.get(connection.getSource());
            Object targetKey = stationKeys.get(connection.getTarget());
            if (sourceKey != null && targetKey != null) {
                model.setConnected(sourceKey, targetKey, true);
            }
        }
    }

    private static void applyMeasures(
            CommonModel model,
            List<MeasureDescriptor> measures,
            Map<String, Object> classKeys,
            Map<String, Object> stationKeys,
            Map<String, Object> regionKeys) {
        if (measures == null) {
            return;
        }
        for (MeasureDescriptor measure : measures) {
            if (measure == null) {
                continue;
            }

            Object stationKey;
            if (NODETYPE_REGION.equalsIgnoreCase(measure.getNodeType())) {
                stationKey = regionKeys.get(measure.getReferenceNode());
            } else {
                stationKey = stationKeys.get(measure.getReferenceNode());
            }

            Object classKey = classKeys.get(measure.getReferenceUserClass());
            Double alpha = parseDouble(measure.getAlpha());
            if (alpha != null) {
                alpha = Double.valueOf(1.0d - alpha.doubleValue());
            } else {
                alpha = Defaults.getAsDouble("measureAlpha");
            }

            Double precision = parseDouble(measure.getPrecision());
            if (precision == null) {
                precision = Defaults.getAsDouble("measurePrecision");
            }

            boolean verbose = Boolean.TRUE.equals(parseBoolean(measure.getVerbose()));
            model.addMeasure(measure.getType(), stationKey, classKey, alpha, precision, verbose);
        }
    }

    private static void applyBlockingRegions(
            CommonModel model,
            List<BlockingRegionDescriptor> regions,
            Map<String, Object> classKeys,
            Map<String, Object> stationKeys,
            Map<String, Object> regionKeys) {
        if (regions == null) {
            return;
        }

        for (BlockingRegionDescriptor region : regions) {
            if (region == null) {
                continue;
            }
            String type = hasText(region.getType()) ? region.getType() : Defaults.get("blockingRegionType");
            Object regionKey = model.addBlockingRegion(region.getName(), type);
            regionKeys.put(region.getName(), regionKey);

            for (String stationName : safeList(region.getRegionNodes())) {
                Object stationKey = stationKeys.get(stationName);
                if (stationKey != null) {
                    model.addRegionStation(regionKey, stationKey);
                }
            }

            Integer maxJobs = parseInteger(region.getGlobalConstraints().get(XML_A_GLOBALCONSTRAINT_MAXJOBS));
            if (maxJobs != null) {
                model.setRegionCustomerConstraint(regionKey, maxJobs);
            }
            Integer maxMemory = parseInteger(region.getGlobalConstraints().get(XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY));
            if (maxMemory != null) {
                model.setRegionMemorySize(regionKey, maxMemory);
            }

            for (Map.Entry<String, ? extends Map<String, String>> classEntry : region.getClassConstraints().entrySet()) {
                Object classKey = classKeys.get(classEntry.getKey());
                if (classKey == null) {
                    continue;
                }
                Map<String, String> constraints = classEntry.getValue();
                if (constraints == null) {
                    continue;
                }
                Integer classMaxJobs = parseInteger(constraints.get(XML_A_CLASSCONSTRAINT_MAXJOBS));
                if (classMaxJobs != null) {
                    model.setRegionClassCustomerConstraint(regionKey, classKey, classMaxJobs);
                }
                Integer classMaxMemory = parseInteger(constraints.get(XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY));
                if (classMaxMemory != null) {
                    model.setRegionClassMemorySize(regionKey, classKey, classMaxMemory);
                }
                Double softDeadline = parseDouble(constraints.get(XML_A_CLASSDUEDATE_DUEDATE));
                if (softDeadline != null) {
                    model.setRegionClassSoftDeadline(regionKey, classKey, softDeadline);
                }
            }

            for (Map.Entry<String, String> entry : region.getClassWeights().entrySet()) {
                Object classKey = classKeys.get(entry.getKey());
                Integer weight = parseInteger(entry.getValue());
                if (classKey != null && weight != null) {
                    model.setRegionClassWeight(regionKey, classKey, weight);
                }
            }

            for (Map.Entry<String, String> entry : region.getClassSizes().entrySet()) {
                Object classKey = classKeys.get(entry.getKey());
                Integer size = parseInteger(entry.getValue());
                if (classKey != null && size != null) {
                    model.setRegionClassSize(regionKey, classKey, size);
                }
            }

            for (Map.Entry<String, String> entry : region.getDropRules().entrySet()) {
                Object classKey = classKeys.get(entry.getKey());
                Boolean drop = parseDropRule(entry.getValue());
                if (classKey != null && drop != null) {
                    model.setRegionClassDropRule(regionKey, classKey, drop);
                }
            }
        }
    }

    private static void buildQueueSection(
            SectionDescriptor sec,
            Object stationKey,
            CommonModel model,
            Map<String, Object> classKeys) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if (param == null) {
                continue;
            }

            if ("size".equals(param.getName())) {
                Integer capacity = parseInteger(param.getValue());
                if (capacity != null) {
                    model.setStationQueueCapacity(stationKey, capacity);
                }
                continue;
            }

            if ("dropStrategies".equals(param.getName())) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = classKeys.get(sub.getRefClass());
                    if (classKey == null) {
                        continue;
                    }
                    String mapped = mapDropRule(sub.getValue());
                    if (mapped != null) {
                        model.setDropRule(stationKey, classKey, mapped);
                    }
                }
                continue;
            }

            if (QUEUE_PUT_STRATEGY.equals(param.getClassPath())) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    applyQueuePutStrategy(sub.getClassPath(), stationKey, classKeys.get(sub.getRefClass()), model);
                }
            }
        }
    }

    private static void buildServerSection(
            SectionDescriptor sec,
            Object stationKey,
            CommonModel model,
            Map<String, Object> classKeys,
            Map<String, Object> stationKeys) {
        applySectionStationStrategy(sec.getClassName(), stationKey, model, classKeys);

        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if (param == null) {
                continue;
            }
            if ("maxJobs".equals(param.getName())) {
                Integer servers = parseInteger(param.getValue());
                if (servers != null) {
                    model.setStationNumberOfServers(stationKey, servers);
                }
                continue;
            }

            if ("serviceWeights".equals(param.getName())) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = classKeys.get(sub.getRefClass());
                    Double weight = parseDouble(sub.getValue());
                    if (classKey != null && weight != null) {
                        model.setServiceWeight(stationKey, classKey, weight);
                    }
                }
                continue;
            }

            if (SERVICE_STRATEGY.equals(param.getClassPath())) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = classKeys.get(sub.getRefClass());
                    Distribution dist = instantiateDistributionFromParameter(sub);
                    if (classKey != null && dist != null) {
                        model.setServiceTimeDistribution(stationKey, classKey, dist);
                    }
                }
                continue;
            }

            if (PS_STRATEGY.equals(param.getClassPath())) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = classKeys.get(sub.getRefClass());
                    if (classKey != null) {
                        applyPsQueueStrategy(stationKey, classKey, sub.getClassPath(), model);
                    }
                }
                continue;
            }

            if ("RoutingStrategy".equals(param.getName())) {
                // Some station kinds encode routing-like parameters here.
                buildRouterParameter(param, stationKey, model, classKeys, stationKeys);
            }
        }
    }

    private static void buildDelaySection(
            SectionDescriptor sec,
            Object stationKey,
            CommonModel model,
            Map<String, Object> classKeys) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if (param == null) {
                continue;
            }
            if (SERVICE_STRATEGY.equals(param.getClassPath()) || "ServiceStrategy".equals(param.getName())) {
                for (ParameterDescriptor sub : safeList(param.getSubParameters())) {
                    Object classKey = classKeys.get(sub.getRefClass());
                    Distribution dist = instantiateDistributionFromParameter(sub);
                    if (classKey != null && dist != null) {
                        model.setServiceTimeDistribution(stationKey, classKey, dist);
                    }
                }
            }
        }
    }

    private static void buildRouterSection(
            SectionDescriptor sec,
            Object stationKey,
            CommonModel model,
            Map<String, Object> classKeys,
            Map<String, Object> stationKeys) {
        for (ParameterDescriptor param : safeList(sec.getParameters())) {
            if (param == null) {
                continue;
            }
            if ("RoutingStrategy".equals(param.getName()) || hasText(param.getClassPath())) {
                buildRouterParameter(param, stationKey, model, classKeys, stationKeys);
            }
        }
    }

    private static void buildRouterParameter(
            ParameterDescriptor routingRoot,
            Object stationKey,
            CommonModel model,
            Map<String, Object> classKeys,
            Map<String, Object> stationKeys) {
        for (ParameterDescriptor classRouting : safeList(routingRoot.getSubParameters())) {
            if (classRouting == null) {
                continue;
            }

            Object classKey = classKeys.get(classRouting.getRefClass());
            if (classKey == null) {
                continue;
            }

            RoutingStrategy strategy = instantiateRoutingStrategy(classRouting.getClassPath());
            if (strategy == null) {
                continue;
            }

            model.setRoutingStrategy(stationKey, classKey, strategy);

            if (strategy instanceof ProbabilityRouting) {
                ProbabilityRouting probabilityRouting = (ProbabilityRouting) model.getRoutingStrategy(stationKey, classKey);
                Map<String, Double> entries = extractStationToDoubleMap(classRouting);
                for (Map.Entry<String, Double> entry : entries.entrySet()) {
                    Object targetStationKey = stationKeys.get(entry.getKey());
                    if (targetStationKey != null) {
                        probabilityRouting.getValues().put(targetStationKey, entry.getValue());
                    }
                }
            } else if (strategy instanceof WeightedRoundRobinRouting) {
                WeightedRoundRobinRouting wrr = (WeightedRoundRobinRouting) model.getRoutingStrategy(stationKey, classKey);
                Map<String, Integer> weights = extractStationToIntegerMap(classRouting);
                for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                    Object targetStationKey = stationKeys.get(entry.getKey());
                    if (targetStationKey != null) {
                        wrr.getWeights().put(targetStationKey, entry.getValue());
                    }
                }
            } else if (strategy instanceof LoadDependentRouting) {
                LoadDependentRouting ldr = (LoadDependentRouting) model.getRoutingStrategy(stationKey, classKey);
                Map<Integer, Map<String, Double>> ranges = extractLoadDependentMap(classRouting);
                for (Map.Entry<Integer, Map<String, Double>> fromEntry : ranges.entrySet()) {
                    for (Map.Entry<String, Double> targetEntry : fromEntry.getValue().entrySet()) {
                        Object targetStationKey = stationKeys.get(targetEntry.getKey());
                        if (targetStationKey != null) {
                            ldr.addEmpiricalEntry(fromEntry.getKey(), targetStationKey, targetEntry.getValue());
                        }
                    }
                }
            } else if (strategy instanceof ClassSwitchRouting) {
                // Scaffolding: branch/class matrix parsing from recursive parameters will be completed in the next step.
                ClassSwitchRouting classSwitch = (ClassSwitchRouting) model.getRoutingStrategy(stationKey, classKey);
                if (classSwitch.getValues().isEmpty()) {
                    Map<String, Double> branchProbabilities = extractStationToDoubleMap(classRouting);
                    for (Map.Entry<String, Double> entry : branchProbabilities.entrySet()) {
                        Object targetStationKey = stationKeys.get(entry.getKey());
                        if (targetStationKey != null) {
                            classSwitch.getValues().put(targetStationKey, entry.getValue());
                        }
                    }
                }
            }
        }
    }

    private static void buildClassSwitchSection(
            SectionDescriptor sec,
            Object stationKey,
            CommonModel model,
            Map<String, Object> classKeys) {
        for (ParameterDescriptor matrix : safeList(sec.getParameters())) {
            for (ParameterDescriptor row : safeList(matrix.getSubParameters())) {
                Object inClassKey = classKeys.get(row.getRefClass());
                if (inClassKey == null) {
                    continue;
                }
                for (ParameterDescriptor cell : safeList(row.getSubParameters())) {
                    Object outClassKey = classKeys.get(cell.getRefClass());
                    Float value = parseFloat(cell.getValue());
                    if (outClassKey != null && value != null) {
                        model.setClassSwitchMatrix(stationKey, inClassKey, outClassKey, value.floatValue());
                    }
                }
            }
        }
    }

    private static RoutingStrategy instantiateRoutingStrategy(String classPath) {
        if (!hasText(classPath)) {
            return null;
        }
        for (RoutingStrategy strategy : RoutingStrategy.findAll()) {
            if (classPath.equals(strategy.getClassPath())) {
                return strategy.clone();
            }
        }
        return null;
    }

    private static Distribution instantiateDistributionFromParameter(ParameterDescriptor parameter) {
        if (parameter == null) {
            return null;
        }

        String classPath = parameter.getClassPath();
        if (!hasText(classPath)) {
            for (ParameterDescriptor sub : safeList(parameter.getSubParameters())) {
                if (hasText(sub.getClassPath())) {
                    classPath = sub.getClassPath();
                    break;
                }
            }
        }
        if (!hasText(classPath)) {
            return null;
        }

        for (Distribution distribution : Distribution.findAll()) {
            if (classPath.equals(distribution.getClassPath())) {
                return distribution.clone();
            }
        }
        return null;
    }

    private static Map<String, Double> extractStationToDoubleMap(ParameterDescriptor parameter) {
        Map<String, Double> result = new HashMap<>();
        collectStationDoublePairs(parameter, result);
        return result;
    }

    private static void collectStationDoublePairs(ParameterDescriptor parameter, Map<String, Double> output) {
        if (parameter == null) {
            return;
        }

        List<ParameterDescriptor> children = safeList(parameter.getSubParameters());
        if (children.size() >= 2) {
            String stationName = firstNonBlank(children.get(0).getValue(), children.get(0).getName());
            Double probability = parseDouble(children.get(1).getValue());
            if (hasText(stationName) && probability != null) {
                output.put(stationName, probability);
            }
        }

        for (ParameterDescriptor child : children) {
            collectStationDoublePairs(child, output);
        }
    }

    private static Map<String, Integer> extractStationToIntegerMap(ParameterDescriptor parameter) {
        Map<String, Integer> result = new HashMap<>();
        collectStationIntegerPairs(parameter, result);
        return result;
    }

    private static void collectStationIntegerPairs(ParameterDescriptor parameter, Map<String, Integer> output) {
        if (parameter == null) {
            return;
        }

        List<ParameterDescriptor> children = safeList(parameter.getSubParameters());
        if (children.size() >= 2) {
            String stationName = firstNonBlank(children.get(0).getValue(), children.get(0).getName());
            Integer weight = parseInteger(children.get(1).getValue());
            if (hasText(stationName) && weight != null) {
                output.put(stationName, weight);
            }
        }

        for (ParameterDescriptor child : children) {
            collectStationIntegerPairs(child, output);
        }
    }

    private static Map<Integer, Map<String, Double>> extractLoadDependentMap(ParameterDescriptor parameter) {
        Map<Integer, Map<String, Double>> result = new HashMap<>();
        for (ParameterDescriptor fromEntry : safeList(parameter.getSubParameters())) {
            Integer from = parseInteger(fromEntry.getValue());
            if (from == null) {
                continue;
            }
            Map<String, Double> stationProbabilities = new HashMap<>();
            collectStationDoublePairs(fromEntry, stationProbabilities);
            if (!stationProbabilities.isEmpty()) {
                result.put(from, stationProbabilities);
            }
        }
        return result;
    }

    private static void applySectionStationStrategy(
            String sectionClassName,
            Object stationKey,
            CommonModel model,
            Map<String, Object> classKeys) {
        if (CLASSNAME_PSSERVER.equals(sectionClassName)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_PSSERVER);
            for (Object classKey : classKeys.values()) {
                model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_PS);
            }
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

    private static void applyQueuePutStrategy(
            String strategyClassPath,
            Object stationKey,
            Object classKey,
            CommonModel model) {
        if (!hasText(strategyClassPath) || classKey == null) {
            return;
        }
        String strategy = strategyClassPath.substring(strategyClassPath.lastIndexOf('.') + 1);
        if ("TailStrategy".equals(strategy)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
            model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_FCFS);
        } else if ("TailStrategyPriority".equals(strategy)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
            model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_FCFS);
        } else if ("HeadStrategy".equals(strategy)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
            model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LCFS);
        } else if ("HeadStrategyPriority".equals(strategy)) {
            model.setStationQueueStrategy(stationKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
            model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_LCFS);
        }
    }

    private static void applyPsQueueStrategy(
            Object stationKey,
            Object classKey,
            String strategyClassPath,
            CommonModel model) {
        if (!hasText(strategyClassPath)) {
            return;
        }
        String strategy = strategyClassPath.substring(strategyClassPath.lastIndexOf('.') + 1);
        if ("EPSStrategy".equals(strategy) || "EPSStrategyPriority".equals(strategy)) {
            model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_PS);
        } else if ("GPSStrategy".equals(strategy) || "GPSStrategyPriority".equals(strategy)) {
            model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_GPS);
        } else if ("DPSStrategy".equals(strategy) || "DPSStrategyPriority".equals(strategy)) {
            model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_DPS);
        } else if ("QBPSStrategy".equals(strategy) || "QBPSStrategyPriority".equals(strategy)) {
            model.setQueueStrategy(stationKey, classKey, QUEUE_STRATEGY_QBPS);
        }
    }

    private static String mapDropRule(String value) {
        if (!hasText(value)) {
            return null;
        }
        if ("drop".equalsIgnoreCase(value)) {
            return FINITE_DROP;
        }
        if ("BAS blocking".equalsIgnoreCase(value)) {
            return FINITE_BLOCK;
        }
        if ("waiting queue".equalsIgnoreCase(value)) {
            return FINITE_WAITING;
        }
        if ("retrial".equalsIgnoreCase(value)) {
            return FINITE_RETRIAL;
        }
        return value;
    }

    private static Boolean parseDropRule(String value) {
        if (!hasText(value)) {
            return null;
        }
        if ("drop".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return Boolean.FALSE;
        }
        return Boolean.FALSE;
    }

    private static void applyGlobalParameters(SimDescriptor descriptor, CommonModel model) {
        Map<String, String> attrs = descriptor.getGlobalAttributes();
        if (attrs == null) {
            return;
        }

        Long seed = parseLong(firstNonBlank(attrs.get(XML_A_ROOT_SEED), attrs.get("seed")));
        if (seed != null) {
            model.setUseRandomSeed(Boolean.FALSE);
            model.setSimulationSeed(seed);
        } else {
            model.setUseRandomSeed(Boolean.TRUE);
        }

        Double maxDuration = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_DURATION), attrs.get("duration")));
        model.setMaximumDuration(maxDuration != null ? maxDuration : Double.valueOf(-1));

        Double maxSimulated = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_SIMULATED), attrs.get("maxSimulated")));
        model.setMaxSimulatedTime(maxSimulated != null ? maxSimulated : Double.valueOf(-1));

        Double polling = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_POLLING), attrs.get("polling")));
        if (polling != null) {
            model.setPollingInterval(polling);
        }

        Integer maxSamples = parseInteger(firstNonBlank(attrs.get(XML_A_ROOT_MAXSAMPLES), attrs.get("maxSamples")));
        if (maxSamples != null) {
            model.setMaxSimulationSamples(maxSamples);
        }

        Boolean disableStatistic = parseBoolean(firstNonBlank(
                attrs.get(XML_A_ROOT_DISABLESTATISTIC),
                attrs.get("disableStatistic"),
                attrs.get("disableStatisticStop")));
        if (disableStatistic != null) {
            model.setDisableStatistic(disableStatistic);
        }

        Integer maxEvents = parseInteger(firstNonBlank(attrs.get(XML_A_ROOT_MAXEVENTS), attrs.get("maxEvents")));
        model.setMaxSimulationEvents(maxEvents != null ? maxEvents : Integer.valueOf(-1));

        String logPath = firstNonBlank(attrs.get(XML_A_ROOT_LOGPATH), attrs.get("logPath"));
        if (hasText(logPath)) {
            File dir = new File(logPath);
            if (dir.isDirectory()) {
                model.setLoggingGlbParameter("path", dir.getAbsolutePath());
            } else {
                model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
            }
        } else {
            model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
        }

        String logReplaceMode = firstNonBlank(attrs.get(XML_A_ROOT_LOGREPLACE), attrs.get("logReplaceMode"));
        model.setLoggingGlbParameter("autoAppend", hasText(logReplaceMode)
                ? logReplaceMode
                : Defaults.get("loggerAutoAppend"));

        String logDelimiter = firstNonBlank(attrs.get(XML_A_ROOT_LOGDELIM), attrs.get("logDelimiter"));
        model.setLoggingGlbParameter("delim", hasText(logDelimiter)
                ? logDelimiter
                : Defaults.get("loggerDelimiter"));

        String logDecimalSeparator = firstNonBlank(
                attrs.get(XML_A_ROOT_LOGDECIMALSEPARATOR),
                attrs.get("logDecimalSeparator"));
        model.setLoggingGlbParameter("decimalSeparator", hasText(logDecimalSeparator)
                ? logDecimalSeparator
                : Defaults.get("loggerDecimalSeparator"));
    }

    private static void createBaseClasses(
            SimDescriptor descriptor,
            CommonModel model,
            Map<String, Object> classKeys,
            Map<Object, String> refStationsPending) {
        List<UserClassDescriptor> classes = descriptor.getUserClasses();
        if (classes == null) {
            return;
        }

        for (UserClassDescriptor userClass : classes) {
            if (userClass == null) {
                continue;
            }

            String name = userClass.getName();
            int type = "open".equalsIgnoreCase(userClass.getType())
                    ? CLASS_TYPE_OPEN
                    : CLASS_TYPE_CLOSED;

            Integer priority = parseInteger(userClass.getPriority());
            if (priority == null) {
                priority = Defaults.getAsInteger("classPriority");
            }

            Double softDeadline = parseDouble(userClass.getSoftDeadline());
            if (softDeadline == null) {
                softDeadline = Defaults.getAsDouble("classSoftDeadline");
            }

            Integer customers = parseInteger(userClass.getCustomers());
            if (customers == null) {
                customers = Defaults.getAsInteger("classPopulation");
            }

            Object key = model.addClass(name, type, priority, softDeadline, customers, null);
            classKeys.put(name, key);
            refStationsPending.put(key, userClass.getReferenceSource());
        }
    }

    private static void createBaseStations(
            SimDescriptor descriptor,
            CommonModel model,
            Map<String, Object> stationKeys) {
        List<NodeDescriptor> nodes = descriptor.getNodes();
        if (nodes == null) {
            return;
        }

        for (NodeDescriptor node : nodes) {
            if (node == null) {
                continue;
            }

            String stationType = determineStationType(node);
            Object key = model.addStation(node.getName(), stationType, 1, new ArrayList<ServerType>());
            stationKeys.put(node.getName(), key);
        }
    }

    private static String determineStationType(NodeDescriptor node) {
        String first = getSectionClassName(node, 0);
        String second = getSectionClassName(node, 1);
        String third = getSectionClassName(node, 2);

        if (CLASSNAME_SOURCE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_SOURCE;
        } else if (CLASSNAME_SINK.equals(first)) {
            return STATION_TYPE_SINK;
        } else if (CLASSNAME_TERMINAL.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_TERMINAL;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_ROUTER;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_DELAY.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_DELAY;
        } else if (CLASSNAME_QUEUE.equals(first)
                && (CLASSNAME_SERVER.equals(second)
                || CLASSNAME_PREEMPTIVESERVER.equals(second)
                || CLASSNAME_PSSERVER.equals(second)
                || CLASSNAME_PSSERVER_PRIORITY.equals(second)
                || CLASSNAME_EXHAUSTIVEPSERVER.equals(second)
                || CLASSNAME_LKPSERVER.equals(second)
                || CLASSNAME_GATEDPSERVER.equals(second))
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_SERVER;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_FORK.equals(third)) {
            return STATION_TYPE_FORK;
        } else if (CLASSNAME_JOIN.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_JOIN;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_LOGGER.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_LOGGER;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_CLASSSWITCH.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_CLASSSWITCH;
        } else if (CLASSNAME_SEMAPHORE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_SEMAPHORE;
        } else if (CLASSNAME_JOIN.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_FORK.equals(third)) {
            return STATION_TYPE_SCALER;
        } else if (CLASSNAME_STORAGE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_LINKAGE.equals(third)) {
            return STATION_TYPE_PLACE;
        } else if (CLASSNAME_ENABLING.equals(first)
                && CLASSNAME_TIMING.equals(second)
                && CLASSNAME_FIRING.equals(third)) {
            return STATION_TYPE_TRANSITION;
        }

        throw new IllegalArgumentException("Unable to determine station type for node: " + node.getName());
    }

    private static String getSectionClassName(NodeDescriptor node, int index) {
        List<SectionDescriptor> sections = node.getSections();
        if (sections == null || index < 0 || index >= sections.size()) {
            return null;
        }
        SectionDescriptor section = sections.get(index);
        return section != null ? section.getClassName() : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.<T>emptyList() : values;
    }

    private static Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double parseDouble(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean parseBoolean(String value) {
        if (!hasText(value)) {
            return null;
        }
        return Boolean.valueOf(value.trim());
    }

    private static Float parseFloat(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Float.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

