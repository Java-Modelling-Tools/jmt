package jmt.gui.common.xml.mapper.sections;

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


    // =========================================================================================
    // --- READING PHASE (XML -> DTO -> Model) ---
    // =========================================================================================

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor routingParam = getFirst(sec.getParameters());
        if (routingParam == null) return;

        getRefClassMap(routingParam).forEach((className, p) -> {
            String cp = p.getClassPath();
            Object classKey = ctx.classes.get(className);
            RoutingStrategy rs = resolveRoutingStrategy(cp);

            model.setRoutingStrategy(key, classKey, rs);

            if (rs instanceof ProbabilityRouting) {
                parseProbabilityRouting(p, key, classKey, ctx);
            } else if (rs instanceof LoadDependentRouting) {
                parseLoadDependentRouting(p, key, classKey, ctx);
            } else if (rs instanceof PowerOfKRouting) {
                parsePowerOfKRouting(p, (PowerOfKRouting) rs);
            } else if (rs instanceof WeightedRoundRobinRouting) {
                parseWeightedRoundRobinRouting(p, key, classKey, ctx);
            } else if (rs instanceof ClassSwitchRouting) {
                parseClassSwitchRouting(p, key, classKey, ctx);
            }
        });
    }

    private RoutingStrategy resolveRoutingStrategy(String cp) {
        if (cp == null) return new RandomRouting();
        if (cp.contains(STR_EMPIRICAL) || cp.contains(STR_PROBABILITIES)) return new ProbabilityRouting();
        if (cp.contains(STR_LOAD_DEPENDENT)) return new LoadDependentRouting();
        if (cp.contains(STR_POWER_OF_K)) return new PowerOfKRouting();
        if (cp.contains(STR_WEIGHTED_ROUND_ROBIN)) return new WeightedRoundRobinRouting();
        if (cp.contains(STR_CLASS_SWITCH)) return new ClassSwitchRouting();
        return new RandomRouting();
    }

    private void parseProbabilityRouting(ParameterDescriptor p, Object key, Object classKey, MappingContext ctx) {
        ParameterDescriptor entryArray = getFirst(getSubParams(p));
        ctx.empiricalRouting.put(new Object[]{key, classKey}, parseStringDoubleMap(entryArray));
    }

    private void parseLoadDependentRouting(ParameterDescriptor p, Object key, Object classKey, MappingContext ctx) {
        ParameterDescriptor entryArray = getFirst(getSubParams(p));
        if (entryArray == null) return;

        for (ParameterDescriptor fromEntry : getSubParams(entryArray)) {
            List<ParameterDescriptor> subFrom = getSubParams(fromEntry);
            Integer fromValue = valI(getAt(subFrom, 0));
            if (fromValue == null) continue;

            Map<String, Double> tmp = parseStringDoubleMap(getAt(subFrom, 1));
            ctx.empiricalLDRouting.put(new Object[]{key, classKey, fromValue}, tmp);
        }
    }

    private void parsePowerOfKRouting(ParameterDescriptor p, PowerOfKRouting rs) {
        List<ParameterDescriptor> sps = getSubParams(p);
        Integer kVal = valI(getAt(sps, 0));
        String bVal = val(getAt(sps, 1));

        if (kVal != null) rs.setK(kVal);
        if (bVal != null) rs.setWithMemory(Boolean.parseBoolean(bVal));
    }

    private void parseWeightedRoundRobinRouting(ParameterDescriptor p, Object key, Object classKey, MappingContext ctx) {
        ParameterDescriptor entryArray = getFirst(getSubParams(p));
        ctx.wrrRouting.put(new Object[]{key, classKey}, parseStringIntegerMap(entryArray));
    }

    private void parseClassSwitchRouting(ParameterDescriptor p, Object key, Object classKey, MappingContext ctx) {
        ParameterDescriptor paramArray = getFirst(getSubParams(p));
        if (paramArray == null) return;

        Map<String, Double> branchTmp = new TreeMap<>();
        Map<String, Map<String, Double>> classTmp = new TreeMap<>();

        for (ParameterDescriptor csp : getSubParams(paramArray)) {
            parseClassSwitchEntry(csp, branchTmp, classTmp);
        }
        ctx.csBranchRouting.put(new Object[]{key, classKey}, branchTmp);
        ctx.csClassRouting.put(new Object[]{key, classKey}, classTmp);
    }

    private void parseClassSwitchEntry(ParameterDescriptor csp, Map<String, Double> branchTmp, Map<String, Map<String, Double>> classTmp) {
        List<ParameterDescriptor> subCsp = getSubParams(csp);
        String sName = val(getAt(subCsp, 0));
        Double dVal = valD(getAt(subCsp, 1));

        if (sName != null && dVal != null) {
            branchTmp.put(sName, dVal);
        }

        Map<String, Double> innerMap = parseStringDoubleMap(getAt(subCsp, 2));
        if (sName != null) {
            classTmp.put(sName, innerMap);
        }
    }

    // --- Helper Generici per Parsare le Mappe Array dell'XML ---

    private Map<String, Double> parseStringDoubleMap(ParameterDescriptor entryArray) {
        Map<String, Double> map = new TreeMap<>();
        if (entryArray != null) {
            for (ParameterDescriptor entry : getSubParams(entryArray)) {
                List<ParameterDescriptor> subEntries = getSubParams(entry);
                String sName = val(getAt(subEntries, 0));
                Double dVal = valD(getAt(subEntries, 1));
                if (sName != null && dVal != null) {
                    map.put(sName, dVal);
                }
            }
        }
        return map;
    }

    private Map<String, Integer> parseStringIntegerMap(ParameterDescriptor entryArray) {
        Map<String, Integer> map = new TreeMap<>();
        if (entryArray != null) {
            for (ParameterDescriptor entry : getSubParams(entryArray)) {
                List<ParameterDescriptor> subEntries = getSubParams(entry);
                String sName = val(getAt(subEntries, 0));
                Integer iVal = valI(getAt(subEntries, 1));
                if (sName != null && iVal != null) {
                    map.put(sName, iVal);
                }
            }
        }
        return map;
    }

    // =========================================================================================
    // --- WRITING PHASE (Model -> DTO -> XML) ---
    // =========================================================================================

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        ParameterDescriptor routingStrategy = makeContainer(XMLConstantNames.XML_E_ROUTING_STRATEGY, STRATEGIES_BASE + XMLConstantNames.XML_E_ROUTING_STRATEGY);
        routingStrategy.setArray(true);

        for (Object classKey : model.getClassKeys()) {
            RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(key, classKey);
            ParameterDescriptor param = makeContainer(rs.getName(), rs.getClassPath());
            param.setRefClass(model.getClassName(classKey));

            if (rs instanceof ProbabilityRouting) {
                writeProbabilityRouting(param, (ProbabilityRouting) rs, model, key);
            } else if (rs instanceof LoadDependentRouting) {
                writeLoadDependentRouting(param, (LoadDependentRouting) rs, model, key);
            } else if (rs instanceof PowerOfKRouting) {
                writePowerOfKRouting(param, (PowerOfKRouting) rs);
            } else if (rs instanceof WeightedRoundRobinRouting) {
                writeWeightedRoundRobinRouting(param, (WeightedRoundRobinRouting) rs, model, key);
            } else if (rs instanceof ClassSwitchRouting) {
                writeClassSwitchRouting(param, (ClassSwitchRouting) rs, model, key);
            }
            routingStrategy.addSubParameter(param);
        }
        sec.addParameter(routingStrategy);
    }

    private void writeProbabilityRouting(ParameterDescriptor param, ProbabilityRouting rs, CommonModel model, Object key) {
        List<Object> outputs = model.getForwardConnections(key);
        Map<Object, Double> values = rs.getValues();
        ParameterDescriptor entryArray = makeContainer(EMPIRICAL_ENTRY_ARRAY, EMPIRICAL_ENTRY_CLASS);

        for (Object output : outputs) {
            ParameterDescriptor entry = makeContainer(XMLConstantNames.PARAM_EMPIRICAL_ENTRY, EMPIRICAL_ENTRY_CLASS);
            entry.addSubParameter(makeParam(XML_A_PRELOADSTATION_NAME, String.class.getName(), model.getStationName(output)));
            entry.addSubParameter(makeParam(XMLConstantNames.XML_A_PROBABILITY, Double.class.getName(), values.getOrDefault(output, 0.0).toString()));
            entryArray.addSubParameter(entry);
        }
        param.addSubParameter(entryArray);
    }

    private void writeLoadDependentRouting(ParameterDescriptor param, LoadDependentRouting rs, CommonModel model, Object key) {
        List<Object> outputs = model.getForwardConnections(key);
        Map<Integer, Map<Object, Double>> allEntries = rs.getAllEmpiricalEntries();
        List<Integer> fromList = new ArrayList<>(allEntries.keySet());
        Collections.sort(fromList);

        ParameterDescriptor ldArray = makeContainer(XML_E_LD_ROUTING_PARAM, LD_ROUTING_PARAM_CLASS);
        for (Integer from : fromList) {
            ParameterDescriptor range = makeContainer(XML_E_LD_ROUTING_PARAM, LD_ROUTING_PARAM_CLASS);
            range.addSubParameter(makeParam(XML_A_FROM, Integer.class.getName(), from.toString()));

            ParameterDescriptor entryArray = makeContainer(EMPIRICAL_ENTRY_ARRAY, EMPIRICAL_ENTRY_CLASS);
            Map<Object, Double> entries = allEntries.get(from);
            for (Object output : outputs) {
                ParameterDescriptor entry = makeContainer(XMLConstantNames.PARAM_EMPIRICAL_ENTRY, EMPIRICAL_ENTRY_CLASS);
                entry.addSubParameter(makeParam(XML_A_PRELOADSTATION_NAME, String.class.getName(), model.getStationName(output)));
                entry.addSubParameter(makeParam(XMLConstantNames.XML_A_PROBABILITY, Double.class.getName(), entries.getOrDefault(output, 0.0).toString()));
                entryArray.addSubParameter(entry);
            }
            range.addSubParameter(entryArray);
            ldArray.addSubParameter(range);
        }
        param.addSubParameter(ldArray);
    }

    private void writePowerOfKRouting(ParameterDescriptor param, PowerOfKRouting rs) {
        param.addSubParameter(makeParam(XML_A_K, Integer.class.getName(), rs.getK().toString()));
        param.addSubParameter(makeParam(XML_A_WITH_MEMORY, Boolean.class.getName(), rs.isWithMemory().toString()));
    }

    private void writeWeightedRoundRobinRouting(ParameterDescriptor param, WeightedRoundRobinRouting rs, CommonModel model, Object key) {
        List<Object> outputs = model.getForwardConnections(key);
        Map<Object, Integer> weights = rs.getWeights();

        ParameterDescriptor weightArray = makeContainer(XML_E_WEIGHT_ENTRY_ARRAY, WEIGHT_ENTRY_CLASS);
        for (Object output : outputs) {
            ParameterDescriptor entry = makeContainer(WEIGHT_ENTRY, WEIGHT_ENTRY_CLASS);
            entry.addSubParameter(makeParam(XML_A_PRELOADSTATION_NAME, String.class.getName(), model.getStationName(output)));
            entry.addSubParameter(makeParam(XML_A_WEIGHT, Integer.class.getName(), weights.getOrDefault(output, 1).toString()));
            weightArray.addSubParameter(entry);
        }
        param.addSubParameter(weightArray);
    }

    private void writeClassSwitchRouting(ParameterDescriptor param, ClassSwitchRouting rs, CommonModel model, Object key) {
        List<Object> outputs = model.getForwardConnections(key);
        List<Object> classes = model.getClassKeys();
        Map<Object, Double> values = rs.getValues();
        Map<Object, Map<Object, Double>> outPaths = rs.getOutPaths();

        ParameterDescriptor csArray = makeContainer(XML_E_CS_ROUTING_PARAM_ARRAY, CS_ROUTING_PARAM_CLASS);
        for (Object output : outputs) {
            ParameterDescriptor csParam = makeContainer(XML_E_CS_ROUTING_PARAM, CS_ROUTING_PARAM_CLASS);
            csParam.addSubParameter(makeParam(XML_A_PRELOADSTATION_NAME, String.class.getName(), model.getStationName(output)));
            csParam.addSubParameter(makeParam(XMLConstantNames.XML_A_PROBABILITY, Double.class.getName(), values.getOrDefault(output, 0.0).toString()));

            ParameterDescriptor classEntryArray = makeContainer(EMPIRICAL_ENTRY_ARRAY, EMPIRICAL_ENTRY_CLASS);
            Map<Object, Double> outPath = outPaths.getOrDefault(output, Collections.emptyMap());
            for (Object classK : classes) {
                ParameterDescriptor classEntry = makeContainer(XMLConstantNames.PARAM_EMPIRICAL_ENTRY, EMPIRICAL_ENTRY_CLASS);
                classEntry.addSubParameter(makeParam(XML_A_STATION_SECTION_CLASSNAME, String.class.getName(), model.getClassName(classK)));
                classEntry.addSubParameter(makeParam(XMLConstantNames.XML_A_PROBABILITY, Double.class.getName(), outPath.getOrDefault(classK, 0.0).toString()));
                classEntryArray.addSubParameter(classEntry);
            }
            csParam.addSubParameter(classEntryArray);
            csArray.addSubParameter(csParam);
        }
        param.addSubParameter(csArray);
    }
}