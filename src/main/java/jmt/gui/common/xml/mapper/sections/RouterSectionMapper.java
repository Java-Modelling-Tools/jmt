/**
 * This class is responsible for mapping the Router section of a node.
 * It configures routing strategies (Probabilistic, Round Robin, Load Dependent, etc.) and transition paths.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper.sections;

import jmt.engine.NetStrategies.RoutingStrategies.ClassSwitchRoutingParameter;
import jmt.engine.random.EmpiricalEntry;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.routingStrategies.*;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class RouterSectionMapper extends AbstractSectionMapper {

    private static final String STRATEGIES_BASE = "jmt.engine.NetStrategies.";
    private static final String ROUTING_SUFFIX = "RoutingStrategies.";
    private static final String EMPIRICAL_ENTRY_CLASS = EmpiricalEntry.class.getName();
    private static final String LD_ROUTING_PARAM_CLASS = STRATEGIES_BASE + ROUTING_SUFFIX + "LoadDependentRoutingParameter";
    private static final String WEIGHT_ENTRY_CLASS = STRATEGIES_BASE + ROUTING_SUFFIX + "WeightEntry";
    private static final String CS_ROUTING_PARAM_CLASS = ClassSwitchRoutingParameter.class.getName();

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor routingParam = getFirst(sec.getParameters());
        if (routingParam == null) return;

        getRefClassMap(routingParam).forEach((className, p) -> mapRoutingStrategy(className, p, key, model, ctx));
    }

    private void mapRoutingStrategy(String className, ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        String cp = p.getClassPath();
        Object classKey = ctx.classes.get(className);

        setRoutingStrategy(key, classKey, cp, model);

        RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(key, classKey);

        if (rs instanceof ProbabilityRouting) {
            mapProbabilityRouting(key, classKey, p, ctx);
        } else if (rs instanceof LoadDependentRouting) {
            mapLoadDependentRouting(key, classKey, p, ctx);
        } else if (rs instanceof PowerOfKRouting) {
            mapPowerOfKRouting((PowerOfKRouting) rs, p);
        } else if (rs instanceof WeightedRoundRobinRouting) {
            mapWeightedRoundRobinRouting(key, classKey, p, ctx);
        } else if (rs instanceof ClassSwitchRouting) {
            mapClassSwitchRouting(key, classKey, p, ctx);
        }
    }

    private void setRoutingStrategy(Object key, Object classKey, String cp, CommonModel model) {
        if (cp != null && (cp.contains("Empirical") || cp.contains("Probabilities"))) model.setRoutingStrategy(key, classKey, new ProbabilityRouting());
        else if (cp != null && cp.contains("LoadDependent")) model.setRoutingStrategy(key, classKey, new LoadDependentRouting());
        else if (cp != null && cp.contains("PowerOfK")) model.setRoutingStrategy(key, classKey, new PowerOfKRouting());
        else if (cp != null && cp.contains("WeightedRoundRobin")) model.setRoutingStrategy(key, classKey, new WeightedRoundRobinRouting());
        else if (cp != null && cp.contains("ClassSwitch")) model.setRoutingStrategy(key, classKey, new ClassSwitchRouting());
        else model.setRoutingStrategy(key, classKey, new RandomRouting());
    }

    private void mapProbabilityRouting(Object key, Object classKey, ParameterDescriptor p, MappingContext ctx) {
        ParameterDescriptor entryArray = getFirst(getSubParams(p));
        Map<String, Double> tmp = mapDoubleValueMap(entryArray);
        ctx.empiricalRouting.put(new Object[]{key, classKey}, tmp);
    }

    private void mapLoadDependentRouting(Object key, Object classKey, ParameterDescriptor p, MappingContext ctx) {
        ParameterDescriptor entryArray = getFirst(getSubParams(p));
        if (entryArray == null) return;

        for (ParameterDescriptor fromEntry : getSubParams(entryArray)) {
            List<ParameterDescriptor> subFrom = getSubParams(fromEntry);
            Integer fromValue = valI(getAt(subFrom, 0));
            if (fromValue == null) continue;

            Map<String, Double> tmp = mapDoubleValueMap(getAt(subFrom, 1));
            ctx.empiricalLDRouting.put(new Object[]{key, classKey, fromValue}, tmp);
        }
    }

    private void mapPowerOfKRouting(PowerOfKRouting rs, ParameterDescriptor p) {
        List<ParameterDescriptor> sps = getSubParams(p);
        Integer kVal = valI(getAt(sps, 0));
        String bVal = val(getAt(sps, 1));
        if (kVal != null) rs.setK(kVal);
        if (bVal != null) rs.setWithMemory(Boolean.parseBoolean(bVal));
    }

    private void mapWeightedRoundRobinRouting(Object key, Object classKey, ParameterDescriptor p, MappingContext ctx) {
        ParameterDescriptor entryArray = getFirst(getSubParams(p));
        Map<String, Integer> tmp = mapIntegerValueMap(entryArray);
        ctx.wrrRouting.put(new Object[]{key, classKey}, tmp);
    }

    private void mapClassSwitchRouting(Object key, Object classKey, ParameterDescriptor p, MappingContext ctx) {
        Map<String, Double> branchTmp = new TreeMap<>();
        Map<String, Map<String, Double>> classTmp = new TreeMap<>();
        ParameterDescriptor paramArray = getFirst(getSubParams(p));

        if (paramArray != null) {
            for (ParameterDescriptor csp : getSubParams(paramArray)) {
                processClassSwitchParameter(csp, branchTmp, classTmp);
            }
        }
        ctx.csBranchRouting.put(new Object[]{key, classKey}, branchTmp);
        ctx.csClassRouting.put(new Object[]{key, classKey}, classTmp);
    }

    private Map<String, Double> mapDoubleValueMap(ParameterDescriptor entryArray) {
        Map<String, Double> tmp = new TreeMap<>();
        if (entryArray != null) {
            for (ParameterDescriptor entry : getSubParams(entryArray)) {
                List<ParameterDescriptor> subEntries = getSubParams(entry);
                String sName = val(getAt(subEntries, 0));
                Double dVal = valD(getAt(subEntries, 1));
                if (sName != null && dVal != null) tmp.put(sName, dVal);
            }
        }
        return tmp;
    }

    private Map<String, Integer> mapIntegerValueMap(ParameterDescriptor entryArray) {
        Map<String, Integer> tmp = new TreeMap<>();
        if (entryArray != null) {
            for (ParameterDescriptor entry : getSubParams(entryArray)) {
                List<ParameterDescriptor> subEntries = getSubParams(entry);
                String sName = val(getAt(subEntries, 0));
                Integer iVal = valI(getAt(subEntries, 1));
                if (sName != null && iVal != null) tmp.put(sName, iVal);
            }
        }
        return tmp;
    }

    private void processClassSwitchParameter(ParameterDescriptor csp, Map<String, Double> branchTmp, Map<String, Map<String, Double>> classTmp) {
        List<ParameterDescriptor> subCsp = getSubParams(csp);
        String sName = val(getAt(subCsp, 0));
        Double dVal = valD(getAt(subCsp, 1));
        if (sName != null && dVal != null) branchTmp.put(sName, dVal);

        Map<String, Double> innerMap = mapDoubleValueMap(getAt(subCsp, 2));
        if (sName != null) classTmp.put(sName, innerMap);
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        ParameterDescriptor routingStrategy = new ParameterDescriptor();
        routingStrategy.setName("RoutingStrategy");
        routingStrategy.setClassPath(STRATEGIES_BASE + "RoutingStrategy");

        for (Object classKey : model.getClassKeys()) {
            RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(key, classKey);
            routingStrategy.addSubParameter(createRoutingStrategyParam(rs, model, classKey, key));
        }

        sec.addParameter(routingStrategy);
    }

    private ParameterDescriptor createRoutingStrategyParam(RoutingStrategy rs, CommonModel model, Object classKey, Object stationKey) {
        ParameterDescriptor param = new ParameterDescriptor();
        param.setName(rs.getName());
        param.setClassPath(rs.getClassPath());
        param.setRefClass(model.getClassName(classKey));

        if (rs instanceof ProbabilityRouting) {
            addProbabilityRoutingParams(param, (ProbabilityRouting) rs, model, stationKey);
        } else if (rs instanceof LoadDependentRouting) {
            addLoadDependentRoutingParams(param, (LoadDependentRouting) rs, model, stationKey);
        } else if (rs instanceof PowerOfKRouting) {
            addPowerOfKRoutingParams(param, (PowerOfKRouting) rs);
        } else if (rs instanceof WeightedRoundRobinRouting) {
            addWeightedRoundRobinParams(param, (WeightedRoundRobinRouting) rs, model, stationKey);
        } else if (rs instanceof ClassSwitchRouting) {
            addClassSwitchRoutingParams(param, (ClassSwitchRouting) rs, model, stationKey);
        }

        return param;
    }

    private void addProbabilityRoutingParams(ParameterDescriptor param, ProbabilityRouting rs, CommonModel model, Object stationKey) {
        Vector<Object> outputs = model.getForwardConnections(stationKey);
        Map<Object, Double> values = rs.getValues();

        ParameterDescriptor entryArray = new ParameterDescriptor();
        entryArray.setName("EmpiricalEntryArray");
        entryArray.setClassPath(EMPIRICAL_ENTRY_CLASS);

        for (Object output : outputs) {
            entryArray.addSubParameter(createEmpiricalEntry(
                    model.getStationName(output), values.getOrDefault(output, 0.0).toString()));
        }

        param.addSubParameter(entryArray);
    }

    private void addLoadDependentRoutingParams(ParameterDescriptor param, LoadDependentRouting rs, CommonModel model, Object stationKey) {
        Vector<Object> outputs = model.getForwardConnections(stationKey);
        Map<Integer, Map<Object, Double>> allEntries = rs.getAllEmpiricalEntries();

        List<Integer> fromList = new ArrayList<>(allEntries.keySet());
        Collections.sort(fromList);

        ParameterDescriptor ldArray = new ParameterDescriptor();
        ldArray.setName("LoadDependentRoutingParameter");
        ldArray.setClassPath(LD_ROUTING_PARAM_CLASS);

        for (Integer from : fromList) {
            ldArray.addSubParameter(createLDRange(from, allEntries.get(from), outputs, model));
        }

        param.addSubParameter(ldArray);
    }

    private ParameterDescriptor createLDRange(Integer from, Map<Object, Double> entries, Vector<Object> outputs, CommonModel model) {
        ParameterDescriptor fromParam = new ParameterDescriptor();
        fromParam.setName("from");
        fromParam.setClassPath(Integer.class.getName());
        fromParam.setValue(from.toString());

        ParameterDescriptor entryArray = new ParameterDescriptor();
        entryArray.setName("EmpiricalEntryArray");
        entryArray.setClassPath(EMPIRICAL_ENTRY_CLASS);

        for (Object output : outputs) {
            entryArray.addSubParameter(createEmpiricalEntry(
                    model.getStationName(output), entries.getOrDefault(output, 0.0).toString()));
        }

        ParameterDescriptor range = new ParameterDescriptor();
        range.setName("LoadDependentRoutingParameter");
        range.setClassPath(LD_ROUTING_PARAM_CLASS);
        range.addSubParameter(fromParam);
        range.addSubParameter(entryArray);
        return range;
    }

    private void addPowerOfKRoutingParams(ParameterDescriptor param, PowerOfKRouting rs) {
        ParameterDescriptor kParam = new ParameterDescriptor();
        kParam.setName("k");
        kParam.setClassPath(Integer.class.getName());
        kParam.setValue(rs.getK().toString());
        param.addSubParameter(kParam);

        ParameterDescriptor memoryParam = new ParameterDescriptor();
        memoryParam.setName("withMemory");
        memoryParam.setClassPath(Boolean.class.getName());
        memoryParam.setValue(rs.isWithMemory().toString());
        param.addSubParameter(memoryParam);
    }

    private void addWeightedRoundRobinParams(ParameterDescriptor param, WeightedRoundRobinRouting rs, CommonModel model, Object stationKey) {
        Vector<Object> outputs = model.getForwardConnections(stationKey);
        Map<Object, Integer> weights = rs.getWeights();

        ParameterDescriptor weightArray = new ParameterDescriptor();
        weightArray.setName("WeightEntryArray");
        weightArray.setClassPath(WEIGHT_ENTRY_CLASS);

        for (Object output : outputs) {
            ParameterDescriptor stationName = newParam("stationName", String.class.getName(), model.getStationName(output));
            ParameterDescriptor weight = newParam("weight", Integer.class.getName(), weights.getOrDefault(output, 1).toString());

            ParameterDescriptor entry = new ParameterDescriptor();
            entry.setName("WeightEntry");
            entry.setClassPath(WEIGHT_ENTRY_CLASS);
            entry.addSubParameter(stationName);
            entry.addSubParameter(weight);
            weightArray.addSubParameter(entry);
        }

        param.addSubParameter(weightArray);
    }

    private void addClassSwitchRoutingParams(ParameterDescriptor param, ClassSwitchRouting rs, CommonModel model, Object stationKey) {
        Vector<Object> outputs = model.getForwardConnections(stationKey);
        Vector<Object> classes = model.getClassKeys();
        Map<Object, Double> values = rs.getValues();
        Map<Object, Map<Object, Double>> outPaths = rs.getOutPaths();

        ParameterDescriptor csArray = new ParameterDescriptor();
        csArray.setName("ClassSwitchRoutingParameterArray");
        csArray.setClassPath(CS_ROUTING_PARAM_CLASS);

        for (Object output : outputs) {
            csArray.addSubParameter(createCSEntry(output, classes, values, outPaths, model));
        }

        param.addSubParameter(csArray);
    }

    private ParameterDescriptor createCSEntry(Object output, Vector<Object> classes, Map<Object, Double> values,
                                              Map<Object, Map<Object, Double>> outPaths, CommonModel model) {
        ParameterDescriptor stationName = newParam("stationName", String.class.getName(), model.getStationName(output));
        ParameterDescriptor stationProb = newParam("probability", Double.class.getName(), values.getOrDefault(output, 0.0).toString());

        ParameterDescriptor classEntryArray = new ParameterDescriptor();
        classEntryArray.setName("EmpiricalEntryArray");
        classEntryArray.setClassPath(EMPIRICAL_ENTRY_CLASS);

        Map<Object, Double> outPath = outPaths.getOrDefault(output, Collections.emptyMap());
        for (Object classKey : classes) {
            ParameterDescriptor className = newParam("className", String.class.getName(), model.getClassName(classKey));
            ParameterDescriptor classProb = newParam("probability", Double.class.getName(), outPath.getOrDefault(classKey, 0.0).toString());

            ParameterDescriptor classEntry = new ParameterDescriptor();
            classEntry.setName(XMLConstantNames.PARAM_EMPIRICAL_ENTRY);
            classEntry.setClassPath(EMPIRICAL_ENTRY_CLASS);
            classEntry.addSubParameter(className);
            classEntry.addSubParameter(classProb);
            classEntryArray.addSubParameter(classEntry);
        }

        ParameterDescriptor csParam = new ParameterDescriptor();
        csParam.setName("ClassSwitchRoutingParameter");
        csParam.setClassPath(CS_ROUTING_PARAM_CLASS);
        csParam.addSubParameter(stationName);
        csParam.addSubParameter(stationProb);
        csParam.addSubParameter(classEntryArray);
        return csParam;
    }

    private ParameterDescriptor createEmpiricalEntry(String stationName, String probability) {
        ParameterDescriptor name = newParam("stationName", String.class.getName(), stationName);
        ParameterDescriptor prob = newParam("probability", Double.class.getName(), probability);

        ParameterDescriptor entry = new ParameterDescriptor();
        entry.setName(XMLConstantNames.PARAM_EMPIRICAL_ENTRY);
        entry.setClassPath(EMPIRICAL_ENTRY_CLASS);
        entry.addSubParameter(name);
        entry.addSubParameter(prob);
        return entry;
    }

    private ParameterDescriptor newParam(String name, String classPath, String value) {
        ParameterDescriptor p = new ParameterDescriptor();
        p.setName(name);
        p.setClassPath(classPath);
        p.setValue(value);
        return p;
    }
}