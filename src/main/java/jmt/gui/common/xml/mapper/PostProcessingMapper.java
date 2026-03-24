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

        // --- 1. Risoluzione delle stazioni di riferimento per ogni classe ---
        for (Map.Entry<Object, String> entry : context.refStations.entrySet()) {
            Object classKey = entry.getKey();
            String stationName = entry.getValue();
            if (stationName == null) continue;

            // Per nodi speciali (Fork, ClassSwitch, Scaler, Transition), OLDXMLReader
            // setta direttamente la stringa del tipo, anziché la chiave dell'oggetto
            if (CommonConstants.STATION_TYPE_FORK.equals(stationName)
                    || CommonConstants.STATION_TYPE_CLASSSWITCH.equals(stationName)
                    || CommonConstants.STATION_TYPE_SCALER.equals(stationName)
                    || CommonConstants.STATION_TYPE_TRANSITION.equals(stationName)) {
                model.setClassRefStation(classKey, stationName);
            } else {
                Object sKey = context.stations.get(stationName);
                if (sKey != null) {
                    model.setClassRefStation(classKey, sKey);
                }
            }
        }

        // --- 2. Risoluzione Routing Probabilistico ---
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

        // --- 3. Risoluzione Routing Load Dependent ---
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

        // --- 4. Risoluzione Routing Round Robin ---
        // FIX: Corretta la firma del Map.Entry e implementato il ciclo per risolvere le destinazioni
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

        // --- 5. Risoluzione Routing Class Switch (Branch Probabilities) ---
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

        // --- 6. Risoluzione Routing Class Switch (Class Probabilities) ---
        for (Map.Entry<Object[], Map<String, Map<String, Double>>> entry : context.csClassRouting.entrySet()) {
            Object[] dualkey = entry.getKey();
            ClassSwitchRouting rs = (ClassSwitchRouting) model.getRoutingStrategy(dualkey[0], dualkey[1]);
            Map<Object, Map<Object, Double>> outPaths = rs.getOutPaths();

            for (Map.Entry<String, Map<String, Double>> stationEntry : entry.getValue().entrySet()) {
                Object sKey = context.stations.get(stationEntry.getKey());
                if (sKey == null) continue;

                Map<Object, Double> outPath = new HashMap<>();
                for (Map.Entry<String, Double> classEntry : stationEntry.getValue().entrySet()) {
                    Object cKey = context.classes.get(classEntry.getKey());
                    if (cKey != null) {
                        outPath.put(cKey, classEntry.getValue());
                    }
                }
                outPaths.put(sKey, outPath);
            }
        }

        // --- 7. Risoluzione Fork Probabilistici e ClassSwitch Fork ---
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

        // --- 8. Risoluzione Fork Combinati (CombFork) ---
        for (Map.Entry<Object[], Map<String, Double>> entry : context.combFork.entrySet()) {
            Object[] dualkey = entry.getKey();
            ForkStrategy fs = (ForkStrategy) model.getForkStrategy(dualkey[0], dualkey[1]);
            Map<Object, Double> fork = (Map<Object, Double>) fs.getOutDetails();
            for (Map.Entry<String, Double> valEntry : entry.getValue().entrySet()) {
                // CombFork salva il nome (String) anziché la chiave dell'oggetto stazione
                fork.put(valEntry.getKey(), valEntry.getValue());
            }
        }


        // --- 9. Risoluzione Condizioni Reti di Petri ---

        context.enablingConditionMap.forEach((k, v) -> {
            if (k[0] == null || k[2] == null || k[3] == null) return;
            Object sKey = context.stations.get(k[2]);
            if (sKey != null) model.setEnablingCondition(k[0], (Integer) k[1], sKey, k[3], v);
        });

        context.inhibitingConditionMap.forEach((k, v) -> {
            if (k[0] == null || k[2] == null || k[3] == null) return;
            Object sKey = context.stations.get(k[2]);
            if (sKey != null) model.setInhibitingCondition(k[0], (Integer) k[1], sKey, k[3], v);
        });

        context.resourceConditionMap.forEach((k, v) -> {
            if (k[0] == null || k[2] == null || k[3] == null) return;
            Object sKey = context.stations.get(k[2]);
            if (sKey != null) model.setResourceCondition(k[0], (Integer) k[1], sKey, k[3], v);
        });

        context.firingOutcomeMap.forEach((k, v) -> {
            if (k[0] == null || k[2] == null || k[3] == null) return;
            Object sKey = context.stations.get(k[2]);
            if (sKey != null) model.setFiringOutcome(k[0], (Integer) k[1], sKey, k[3], v);
        });

        // --- 10. Salva ---
        model.resetSaveState();
    }
}