package jmt.gui.common.xml.mapper;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.routingStrategies.ClassSwitchRouting;
import jmt.gui.common.routingStrategies.LoadDependentRouting;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.WeightedRoundRobinRouting;
import jmt.gui.common.xml.dto.SimDescriptor;

import java.util.HashMap;
import java.util.Map;

public class PostProcessingMapper implements ModelElementMapper {

    @Override
    @SuppressWarnings("unchecked")
    public void map(SimDescriptor dto, MappingContext context) {
        CommonModel model = context.getModel();

        resolveReferenceStations(model, context);
        resolveProbabilisticRouting(model, context);
        resolveLoadDependentRouting(model, context);
        resolveWeightedRoundRobinRouting(model, context);
        resolveClassSwitchRouting(model, context);
        resolveForkStrategies(model, context);
        resolvePetriNetConditions(model, context);

        model.resetSaveState();
    }

    private void resolveReferenceStations(CommonModel model, MappingContext context) {
        for (Map.Entry<Object, String> entry : context.refStations.entrySet()) {
            Object classKey = entry.getKey();
            String stationName = entry.getValue();
            if (stationName == null) continue;

            if (isSpecialNode(stationName)) {
                model.setClassRefStation(classKey, stationName);
            } else {
                Object sKey = context.stations.get(stationName);
                if (sKey != null) {
                    model.setClassRefStation(classKey, sKey);
                }
            }
        }
    }

    private boolean isSpecialNode(String stationName) {
        return CommonConstants.STATION_TYPE_FORK.equals(stationName)
                || CommonConstants.STATION_TYPE_CLASSSWITCH.equals(stationName)
                || CommonConstants.STATION_TYPE_SCALER.equals(stationName)
                || CommonConstants.STATION_TYPE_TRANSITION.equals(stationName);
    }

    private void resolveProbabilisticRouting(CommonModel model, MappingContext context) {
        for (Map.Entry<Object[], Map<String, Double>> entry : context.empiricalRouting.entrySet()) {
            Object[] dualkey = entry.getKey(); // [stationKey, classKey]
            ProbabilityRouting rs = (ProbabilityRouting) model.getRoutingStrategy(dualkey[0], dualkey[1]);
            Map<Object, Double> routing = rs.getValues();
            for (Map.Entry<String, Double> valEntry : entry.getValue().entrySet()) {
                Object sKey = context.stations.get(valEntry.getKey());
                if (sKey != null) {
                    routing.put(sKey, valEntry.getValue());
                }
            }
        }
    }

    private void resolveLoadDependentRouting(CommonModel model, MappingContext context) {
        for (Map.Entry<Object[], Map<String, Double>> entry : context.empiricalLDRouting.entrySet()) {
            Object[] triplekey = entry.getKey(); // [stationKey, classKey, fromValue]
            LoadDependentRouting ldr = (LoadDependentRouting) model.getRoutingStrategy(triplekey[0], triplekey[1]);
            for (Map.Entry<String, Double> valEntry : entry.getValue().entrySet()) {
                Object sKey = context.stations.get(valEntry.getKey());
                if (sKey != null) {
                    ldr.addEmpiricalEntry((Integer) triplekey[2], sKey, valEntry.getValue());
                }
            }
        }
    }

    private void resolveWeightedRoundRobinRouting(CommonModel model, MappingContext context) {
        for (Map.Entry<Object[], Map<String, Integer>> entry : context.wrrRouting.entrySet()) {
            Object[] dualkey = entry.getKey(); // [stationKey, classKey]
            WeightedRoundRobinRouting rs = (WeightedRoundRobinRouting) model.getRoutingStrategy(dualkey[0], dualkey[1]);
            Map<Object, Integer> routing = rs.getWeights();
            for (Map.Entry<String, Integer> valEntry : entry.getValue().entrySet()) {
                Object sKey = context.stations.get(valEntry.getKey());
                if (sKey != null) {
                    routing.put(sKey, valEntry.getValue());
                }
            }
        }
    }

    private void resolveClassSwitchRouting(CommonModel model, MappingContext context) {
        resolveClassSwitchBranchRouting(model, context);
        resolveClassSwitchClassRouting(model, context);
    }

    private void resolveClassSwitchBranchRouting(CommonModel model, MappingContext context) {
        for (Map.Entry<Object[], Map<String, Double>> entry : context.csBranchRouting.entrySet()) {
            Object[] dualkey = entry.getKey();
            ClassSwitchRouting rs = (ClassSwitchRouting) model.getRoutingStrategy(dualkey[0], dualkey[1]);
            Map<Object, Double> routing = rs.getValues();
            for (Map.Entry<String, Double> valEntry : entry.getValue().entrySet()) {
                Object sKey = context.stations.get(valEntry.getKey());
                if (sKey != null) {
                    routing.put(sKey, valEntry.getValue());
                }
            }
        }
    }

    private void resolveClassSwitchClassRouting(CommonModel model, MappingContext context) {
        for (Map.Entry<Object[], Map<String, Map<String, Double>>> entry : context.csClassRouting.entrySet()) {
            Object[] dualkey = entry.getKey();
            ClassSwitchRouting rs = (ClassSwitchRouting) model.getRoutingStrategy(dualkey[0], dualkey[1]);
            Map<Object, Map<Object, Double>> outPaths = rs.getOutPaths();

            for (Map.Entry<String, Map<String, Double>> stationEntry : entry.getValue().entrySet()) {
                resolveOutPath(outPaths, stationEntry, context);
            }
        }
    }

    private void resolveOutPath(Map<Object, Map<Object, Double>> outPaths, Map.Entry<String, Map<String, Double>> stationEntry, MappingContext context) {
        Object sKey = context.stations.get(stationEntry.getKey());
        if (sKey == null) return;

        Map<Object, Double> outPath = new HashMap<>();
        for (Map.Entry<String, Double> classEntry : stationEntry.getValue().entrySet()) {
            Object cKey = context.classes.get(classEntry.getKey());
            if (cKey != null) {
                outPath.put(cKey, classEntry.getValue());
            }
        }
        outPaths.put(sKey, outPath);
    }

    @SuppressWarnings("unchecked")
    private void resolveForkStrategies(CommonModel model, MappingContext context) {
        resolveEmpiricalFork(model, context);
        resolveCombinedFork(model, context);
    }

    @SuppressWarnings("unchecked")
    private void resolveEmpiricalFork(CommonModel model, MappingContext context) {
        for (Map.Entry<Object[], Map<String, OutPath>> entry : context.empiricalFork.entrySet()) {
            Object[] dualkey = entry.getKey();
            ForkStrategy fs = (ForkStrategy) model.getForkStrategy(dualkey[0], dualkey[1]);
            Map<Object, OutPath> outPaths = (Map<Object, OutPath>) fs.getOutDetails();
            for (Map.Entry<String, OutPath> valEntry : entry.getValue().entrySet()) {
                Object sKey = context.stations.get(valEntry.getKey());
                if (sKey != null) {
                    outPaths.put(sKey, valEntry.getValue());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void resolveCombinedFork(CommonModel model, MappingContext context) {
        for (Map.Entry<Object[], Map<String, Double>> entry : context.combFork.entrySet()) {
            Object[] dualkey = entry.getKey();
            ForkStrategy fs = (ForkStrategy) model.getForkStrategy(dualkey[0], dualkey[1]);
            Map<Object, Double> fork = (Map<Object, Double>) fs.getOutDetails();
            for (Map.Entry<String, Double> valEntry : entry.getValue().entrySet()) {
                fork.put(valEntry.getKey(), valEntry.getValue());
            }
        }
    }

    private void resolvePetriNetConditions(CommonModel model, MappingContext context) {
        context.enablingConditionMap.forEach((k, v) -> setCondition(model::setEnablingCondition, k, v, context));
        context.inhibitingConditionMap.forEach((k, v) -> setCondition(model::setInhibitingCondition, k, v, context));
        context.resourceConditionMap.forEach((k, v) -> setCondition(model::setResourceCondition, k, v, context));
        context.firingOutcomeMap.forEach((k, v) -> setCondition(model::setFiringOutcome, k, v, context));
    }

    private void setCondition(ConditionSetter setter, Object[] k, Integer v, MappingContext context) {
        if (k[0] == null || k[2] == null || k[3] == null) return;
        Object sKey = context.stations.get(k[2]);
        if (sKey != null) setter.set(k[0], (Integer) k[1], sKey, k[3], v);
    }

    @FunctionalInterface
    private interface ConditionSetter {
        void set(Object stationKey, int modeIndex, Object stationInOutKey, Object classKey, Integer value);
    }
}