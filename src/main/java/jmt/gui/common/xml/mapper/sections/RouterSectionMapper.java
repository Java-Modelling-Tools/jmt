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

import java.util.*;

public class RouterSectionMapper extends AbstractSectionMapper {

    private static final String STRATEGIES_BASE = "jmt.engine.NetStrategies.";
    private static final String ROUTING_SUFFIX = "RoutingStrategies.";
    private static final String EMPIRICAL_ENTRY_CLASS = EmpiricalEntry.class.getName();
    private static final String LD_ROUTING_PARAM_CLASS = STRATEGIES_BASE + ROUTING_SUFFIX + "LoadDependentRoutingParameter";
    private static final String WEIGHT_ENTRY_CLASS = STRATEGIES_BASE + ROUTING_SUFFIX + "WeightEntry";
    private static final String CS_ROUTING_PARAM_CLASS = ClassSwitchRoutingParameter.class.getName();

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor routingParam = getFirst(sec.getParameters());
        if (routingParam == null) return;

        getRefClassMap(routingParam).forEach((className, p) -> {
            String cp = p.getClassPath();
            Object classKey = ctx.classes.get(className);

            if (cp != null && (cp.contains("Empirical") || cp.contains("Probabilities"))) model.setRoutingStrategy(key, classKey, new ProbabilityRouting());
            else if (cp != null && cp.contains("LoadDependent")) model.setRoutingStrategy(key, classKey, new LoadDependentRouting());
            else if (cp != null && cp.contains("PowerOfK")) model.setRoutingStrategy(key, classKey, new PowerOfKRouting());
            else if (cp != null && cp.contains("WeightedRoundRobin")) model.setRoutingStrategy(key, classKey, new WeightedRoundRobinRouting());
            else if (cp != null && cp.contains("ClassSwitch")) model.setRoutingStrategy(key, classKey, new ClassSwitchRouting());
            else model.setRoutingStrategy(key, classKey, new RandomRouting());

            RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(key, classKey);

            if (rs instanceof ProbabilityRouting) {
                ParameterDescriptor entryArray = getFirst(getSubParams(p));
                Map<String, Double> tmp = new TreeMap<>();
                if (entryArray != null) {
                    for (ParameterDescriptor entry : getSubParams(entryArray)) {
                        List<ParameterDescriptor> subEntries = getSubParams(entry);
                        String sName = val(getAt(subEntries, 0));
                        Double dVal = valD(getAt(subEntries, 1));
                        if (sName != null && dVal != null) tmp.put(sName, dVal);
                    }
                }
                ctx.empiricalRouting.put(new Object[]{key, classKey}, tmp);
            } else if (rs instanceof LoadDependentRouting) {
                ParameterDescriptor entryArray = getFirst(getSubParams(p));
                if (entryArray != null) {
                    for (ParameterDescriptor fromEntry : getSubParams(entryArray)) {
                        List<ParameterDescriptor> subFrom = getSubParams(fromEntry);
                        Integer fromValue = valI(getAt(subFrom, 0));
                        if (fromValue == null) continue;
                        Map<String, Double> tmp = new TreeMap<>();
                        ParameterDescriptor subArray = getAt(subFrom, 1);
                        if (subArray != null) {
                            for (ParameterDescriptor entry : getSubParams(subArray)) {
                                List<ParameterDescriptor> subEntries = getSubParams(entry);
                                String sName = val(getAt(subEntries, 0));
                                Double dVal = valD(getAt(subEntries, 1));
                                if (sName != null && dVal != null) tmp.put(sName, dVal);
                            }
                        }
                        ctx.empiricalLDRouting.put(new Object[]{key, classKey, fromValue}, tmp);
                    }
                }
            } else if (rs instanceof PowerOfKRouting) {
                List<ParameterDescriptor> sps = getSubParams(p);
                Integer kVal = valI(getAt(sps, 0));
                String bVal = val(getAt(sps, 1));
                if (kVal != null) ((PowerOfKRouting) rs).setK(kVal);
                if (bVal != null) ((PowerOfKRouting) rs).setWithMemory(Boolean.parseBoolean(bVal));
            } else if (rs instanceof WeightedRoundRobinRouting) {
                ParameterDescriptor entryArray = getFirst(getSubParams(p));
                Map<String, Integer> tmp = new TreeMap<>();
                if (entryArray != null) {
                    for (ParameterDescriptor entry : getSubParams(entryArray)) {
                        List<ParameterDescriptor> subEntries = getSubParams(entry);
                        String sName = val(getAt(subEntries, 0));
                        Integer iVal = valI(getAt(subEntries, 1));
                        if (sName != null && iVal != null) tmp.put(sName, iVal);
                    }
                }
                ctx.wrrRouting.put(new Object[]{key, classKey}, tmp);
            } else if (rs instanceof ClassSwitchRouting) {
                Map<String, Double> branchTmp = new TreeMap<>();
                Map<String, Map<String, Double>> classTmp = new TreeMap<>();
                ParameterDescriptor paramArray = getFirst(getSubParams(p));
                if (paramArray != null) {
                    for (ParameterDescriptor csp : getSubParams(paramArray)) {
                        List<ParameterDescriptor> subCsp = getSubParams(csp);
                        String sName = val(getAt(subCsp, 0));
                        Double dVal = valD(getAt(subCsp, 1));
                        if (sName != null && dVal != null) branchTmp.put(sName, dVal);

                        Map<String, Double> innerMap = new TreeMap<>();
                        ParameterDescriptor subArray = getAt(subCsp, 2);
                        if (subArray != null) {
                            for (ParameterDescriptor entry : getSubParams(subArray)) {
                                List<ParameterDescriptor> subEntries = getSubParams(entry);
                                String cName = val(getAt(subEntries, 0));
                                Double cdVal = valD(getAt(subEntries, 1));
                                if (cName != null && cdVal != null) innerMap.put(cName, cdVal);
                            }
                        }
                        if (sName != null) classTmp.put(sName, innerMap);
                    }
                }
                ctx.csBranchRouting.put(new Object[]{key, classKey}, branchTmp);
                ctx.csClassRouting.put(new Object[]{key, classKey}, classTmp);
            }
        });
    }

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        ParameterDescriptor routingStrategy = makeContainer("RoutingStrategy", STRATEGIES_BASE + "RoutingStrategy");
        routingStrategy.setArray(true); // FIX

        for (Object classKey : model.getClassKeys()) {
            RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(key, classKey);
            ParameterDescriptor param = makeContainer(rs.getName(), rs.getClassPath());
            param.setRefClass(model.getClassName(classKey));

            if (rs instanceof ProbabilityRouting) {
                Vector<Object> outputs = model.getForwardConnections(key);
                Map<Object, Double> values = ((ProbabilityRouting) rs).getValues();
                ParameterDescriptor entryArray = makeContainer("EmpiricalEntryArray", EMPIRICAL_ENTRY_CLASS);

                for (Object output : outputs) {
                    ParameterDescriptor entry = makeContainer(XMLConstantNames.PARAM_EMPIRICAL_ENTRY, EMPIRICAL_ENTRY_CLASS);
                    entry.addSubParameter(makeParam("stationName", String.class.getName(), model.getStationName(output)));
                    entry.addSubParameter(makeParam("probability", Double.class.getName(), values.getOrDefault(output, 0.0).toString()));
                    entryArray.addSubParameter(entry);
                }
                param.addSubParameter(entryArray);
            } else if (rs instanceof LoadDependentRouting) {
                Vector<Object> outputs = model.getForwardConnections(key);
                Map<Integer, Map<Object, Double>> allEntries = ((LoadDependentRouting) rs).getAllEmpiricalEntries();
                List<Integer> fromList = new ArrayList<>(allEntries.keySet());
                Collections.sort(fromList);

                ParameterDescriptor ldArray = makeContainer("LoadDependentRoutingParameter", LD_ROUTING_PARAM_CLASS);
                for (Integer from : fromList) {
                    ParameterDescriptor range = makeContainer("LoadDependentRoutingParameter", LD_ROUTING_PARAM_CLASS);
                    range.addSubParameter(makeParam("from", Integer.class.getName(), from.toString()));

                    ParameterDescriptor entryArray = makeContainer("EmpiricalEntryArray", EMPIRICAL_ENTRY_CLASS);
                    Map<Object, Double> entries = allEntries.get(from);
                    for (Object output : outputs) {
                        ParameterDescriptor entry = makeContainer(XMLConstantNames.PARAM_EMPIRICAL_ENTRY, EMPIRICAL_ENTRY_CLASS);
                        entry.addSubParameter(makeParam("stationName", String.class.getName(), model.getStationName(output)));
                        entry.addSubParameter(makeParam("probability", Double.class.getName(), entries.getOrDefault(output, 0.0).toString()));
                        entryArray.addSubParameter(entry);
                    }
                    range.addSubParameter(entryArray);
                    ldArray.addSubParameter(range);
                }
                param.addSubParameter(ldArray);
            } else if (rs instanceof PowerOfKRouting) {
                param.addSubParameter(makeParam("k", Integer.class.getName(), ((PowerOfKRouting) rs).getK().toString()));
                param.addSubParameter(makeParam("withMemory", Boolean.class.getName(), ((PowerOfKRouting) rs).isWithMemory().toString()));
            } else if (rs instanceof WeightedRoundRobinRouting) {
                Vector<Object> outputs = model.getForwardConnections(key);
                Map<Object, Integer> weights = ((WeightedRoundRobinRouting) rs).getWeights();

                ParameterDescriptor weightArray = makeContainer("WeightEntryArray", WEIGHT_ENTRY_CLASS);
                for (Object output : outputs) {
                    ParameterDescriptor entry = makeContainer("WeightEntry", WEIGHT_ENTRY_CLASS);
                    entry.addSubParameter(makeParam("stationName", String.class.getName(), model.getStationName(output)));
                    entry.addSubParameter(makeParam("weight", Integer.class.getName(), weights.getOrDefault(output, 1).toString()));
                    weightArray.addSubParameter(entry);
                }
                param.addSubParameter(weightArray);
            } else if (rs instanceof ClassSwitchRouting) {
                Vector<Object> outputs = model.getForwardConnections(key);
                Vector<Object> classes = model.getClassKeys();
                Map<Object, Double> values = ((ClassSwitchRouting) rs).getValues();
                Map<Object, Map<Object, Double>> outPaths = ((ClassSwitchRouting) rs).getOutPaths();

                ParameterDescriptor csArray = makeContainer("ClassSwitchRoutingParameterArray", CS_ROUTING_PARAM_CLASS);
                for (Object output : outputs) {
                    ParameterDescriptor csParam = makeContainer("ClassSwitchRoutingParameter", CS_ROUTING_PARAM_CLASS);
                    csParam.addSubParameter(makeParam("stationName", String.class.getName(), model.getStationName(output)));
                    csParam.addSubParameter(makeParam("probability", Double.class.getName(), values.getOrDefault(output, 0.0).toString()));

                    ParameterDescriptor classEntryArray = makeContainer("EmpiricalEntryArray", EMPIRICAL_ENTRY_CLASS);
                    Map<Object, Double> outPath = outPaths.getOrDefault(output, Collections.emptyMap());
                    for (Object classK : classes) {
                        ParameterDescriptor classEntry = makeContainer(XMLConstantNames.PARAM_EMPIRICAL_ENTRY, EMPIRICAL_ENTRY_CLASS);
                        classEntry.addSubParameter(makeParam("className", String.class.getName(), model.getClassName(classK)));
                        classEntry.addSubParameter(makeParam("probability", Double.class.getName(), outPath.getOrDefault(classK, 0.0).toString()));
                        classEntryArray.addSubParameter(classEntry);
                    }
                    csParam.addSubParameter(classEntryArray);
                    csArray.addSubParameter(csParam);
                }
                param.addSubParameter(csArray);
            }
            routingStrategy.addSubParameter(param);
        }
        sec.addParameter(routingStrategy);
    }
}