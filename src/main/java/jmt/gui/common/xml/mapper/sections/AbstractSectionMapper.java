package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.ModelElementMapper;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractSectionMapper implements ModelElementMapper {

    public abstract void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx);

    public abstract void mapCommonModelToDto(SectionDescriptor section, CommonModel model, Object stationKey, NodeDescriptor node, String stationType, WritingContext context);

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
        // This will be implemented by concrete mappers that operate on the whole model
    }

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        // This will be implemented by concrete mappers that operate on the whole model
    }

    // =========================================================================================
    // --- SHARED WRITING (Model -> DTO -> XML) ---
    // =========================================================================================

    protected void addServiceStrategyParameter(SectionDescriptor sec, CommonModel model, Object key, WritingContext ctx) {
        ParameterDescriptor serviceStrategy = makeContainer(PARAM_SERVICE_STRATEGY, SERVICE_STRATEGY_CLASS);
        serviceStrategy.setArray(true);

        for (Object classKey : model.getClassKeys()) {
            Object serviceDist = model.getServiceTimeDistribution(key, classKey);
            if (serviceDist != null) {
                ParameterDescriptor stStrategy = getDistributionParameter(serviceDist, model, classKey, PARAM_SERVICE_TIME_STRATEGY, SERVICE_TIME_STRATEGY_CLASS, ctx);
                serviceStrategy.addSubParameter(stStrategy);
            }
        }
        if (!serviceStrategy.getSubParameters().isEmpty()) {
            sec.addParameter(serviceStrategy);
        }
    }

    protected ParameterDescriptor getDistributionParameter(Object distrObj, CommonModel model, Object classKey, String paramName, String classPath, WritingContext ctx) {
        ParameterDescriptor p = makeContainer(paramName, classPath);
        if (classKey != null) {
            p.setRefClass(model.getClassName(classKey));
        }

        if (distrObj == null || distrObj instanceof ZeroStrategy) {
            p.setName("ZeroServiceTimeStrategy");
            p.setClassPath(ZeroStrategy.getEngineClassPath());
        } else if (distrObj instanceof DisabledStrategy) {
            p.setName("DisabledServiceTimeStrategy");
            p.setClassPath(DisabledStrategy.getEngineClassPath());
        } else if (distrObj instanceof Distribution) {
            handleDistribution(p, (Distribution) distrObj, ctx);
        } else if (distrObj instanceof LDStrategy) {
            handleLDStrategy(p, (LDStrategy) distrObj, model, ctx);
        }
        return p;
    }

    private void handleDistribution(ParameterDescriptor p, Distribution dist, WritingContext ctx) {
        ParameterDescriptor distDesc = makeContainer(dist.getName(), dist.getClassPath());
        ParameterDescriptor distParDesc = makeContainer("distrPar", dist.getParameterClassPath());

        for (int i = 0; i < dist.getNumberOfParameters(); i++) {
            Distribution.Parameter param = dist.getParameter(i);
            ParameterDescriptor paramDesc = createParameterDescriptor(param, ctx);
            if (param.isDirectParameter()) distDesc.addSubParameter(paramDesc);
            else distParDesc.addSubParameter(paramDesc);
        }
        p.addSubParameter(distDesc);
        p.addSubParameter(distParDesc);
    }

    private void handleLDStrategy(ParameterDescriptor p, LDStrategy strategy, CommonModel model, WritingContext ctx) {
        p.setName("LoadDependentStrategy");
        p.setClassPath("jmt.engine.NetStrategies.ServiceStrategies.LoadDependentStrategy");

        ParameterDescriptor ldArray = makeContainer("LDParameter", "jmt.engine.NetStrategies.ServiceStrategies.LDParameter");
        ldArray.setArray(true);

        for (Object rKey : strategy.getAllRanges()) {
            ParameterDescriptor range = makeContainer("LDParameter", "jmt.engine.NetStrategies.ServiceStrategies.LDParameter");
            range.addSubParameter(makeParam("from", XML_INTEGER_CLASS, String.valueOf(strategy.getRangeFrom(rKey))));

            Distribution d = strategy.getRangeDistribution(rKey);
            ParameterDescriptor dP = getDistributionParameter(d, model, null, "distr", "jmt.engine.random.Distribution", ctx);
            if (dP != null && dP.getSubParameters().size() >= 2) {
                range.addSubParameter(dP.getSubParameters().get(0));
                range.addSubParameter(dP.getSubParameters().get(1));
            }

            range.addSubParameter(makeParam("mean", "java.lang.Double", String.valueOf(strategy.getRangeDistributionMean(rKey))));
            ldArray.addSubParameter(range);
        }
        p.addSubParameter(ldArray);
    }

    private ParameterDescriptor createParameterDescriptor(Distribution.Parameter param, WritingContext ctx) {
        ParameterDescriptor p = makeContainer(param.getName(), null);
        Object val = param.getValue();

        if (val instanceof Distribution) {
            p.setClassPath("jmt.engine.random.DistributionContainer");
            ParameterDescriptor nested = getDistributionParameter(val, null, null, "distr", "jmt.engine.random.Distribution", ctx);
            p.addSubParameter(nested.getSubParameters().get(0));
            p.addSubParameter(nested.getSubParameters().get(1));
        } else if (val instanceof Object[][]) {
            p.setArray(true);
            p.setClassPath(param.getValueClass().getName());
            for (Object[] row : (Object[][]) val) {
                ParameterDescriptor rowP = makeContainer(param.getName(), param.getValueClass().getName());
                for (Object cell : row) {
                    rowP.addSubParameter(makeParam(param.getName(), param.getValueClass().getName(), cell != null ? cell.toString() : "0"));
                }
                p.addSubParameter(rowP);
            }
        } else {
            p.setClassPath(param.getValueClass().getName());
            if (val != null) p.setValue(val.toString());
        }
        return p;
    }

    protected void mapTransitionConditions(Object key, SectionDescriptor sec, CommonModel model) {
        mapTransitionConditions(key, sec, model, true);
    }

    protected void mapTransitionConditions(Object key, SectionDescriptor sec, CommonModel model, boolean includeResource) {
        ParameterDescriptor enablingConditions = createConditionMatrix("enabling", "Condition", key, model, (s, m, i, from, cl) -> model.getEnablingCondition(s, i, from, cl));
        if (enablingConditions != null) sec.addParameter(enablingConditions);

        ParameterDescriptor inhibitingConditions = createConditionMatrix("inhibiting", "Condition", key, model, (s, m, i, from, cl) -> model.getInhibitingCondition(s, i, from, cl));
        if (inhibitingConditions != null) sec.addParameter(inhibitingConditions);

        if (includeResource) {
            ParameterDescriptor resourceConditions = createConditionMatrix("resource", "Condition", key, model, (s, m, i, from, cl) -> model.getResourceCondition(s, i, from, cl));
            if (resourceConditions != null) sec.addParameter(resourceConditions);
        }
    }

    protected ParameterDescriptor createConditionMatrix(String baseName, String suffix, Object key, CommonModel model, ConditionGetter getter) {
        return createConditionMatrix(baseName, suffix, key, model, getter, true);
    }

    protected ParameterDescriptor createConditionMatrix(String baseName, String suffix, Object key, CommonModel model, ConditionGetter getter, boolean backward) {
        int modes = model.getTransitionModeListSize(key);
        if (modes <= 0) return null;

        List<Object> stationKeys = backward ? model.getBackwardConnections(key) : model.getForwardConnections(key);
        if (stationKeys == null || stationKeys.isEmpty()) return null;

        // Concatena baseName + suffix + "s" (es. "enablingConditions" o "firingOutcomes")
        ParameterDescriptor matrix = makeContainer(baseName + suffix + "s", "jmt.engine.NetStrategies.TransitionUtilities.TransitionMatrix");
        boolean hasNonZero = populateMatrixModes(matrix, baseName, suffix, modes, stationKeys, model.getClassKeys(), key, model, getter);

        return hasNonZero ? matrix : null;
    }

    private boolean populateMatrixModes(ParameterDescriptor matrix, String baseName, String suffix, int modes, List<Object> stationKeys, List<Object> classKeys, Object key, CommonModel model, ConditionGetter getter) {
        boolean hasNonZero = false;
        for (int i = 0; i < modes; i++) {
            ParameterDescriptor condition = makeContainer(baseName + suffix, "jmt.engine.NetStrategies.TransitionUtilities.TransitionMatrix");
            ParameterDescriptor vectors = makeContainer(baseName + "Vectors", "jmt.engine.NetStrategies.TransitionUtilities.TransitionVector");

            for (Object stationKey : stationKeys) {
                hasNonZero |= buildAndAddVector(vectors, baseName, stationKey, classKeys, key, model, i, getter);
            }
            condition.addSubParameter(vectors);
            matrix.addSubParameter(condition);
        }
        return hasNonZero;
    }

    private boolean buildAndAddVector(ParameterDescriptor vectors, String baseName, Object stationKey, List<Object> classKeys, Object key, CommonModel model, int modeIndex, ConditionGetter getter) {
        boolean hasNonZero = false;
        ParameterDescriptor vector = makeContainer(baseName + "Vector", "jmt.engine.NetStrategies.TransitionUtilities.TransitionVector");
        vector.addSubParameter(makeParam("stationName", XML_STRING_CLASS, model.getStationName(stationKey)));

        ParameterDescriptor entries = makeContainer(baseName + "Entries", XML_INTEGER_CLASS);

        for (Object classKey : classKeys) {
            Integer val = getter.get(key, model, modeIndex, stationKey, classKey);
            if (val != null && val > 0) hasNonZero = true;

            entries.addSubParameter(makeRefParam(baseName + "Entry", XML_INTEGER_CLASS, model.getClassName(classKey), val != null ? val.toString() : VAL_ZERO));
        }
        vector.addSubParameter(entries);
        vectors.addSubParameter(vector);
        return hasNonZero;
    }

    @FunctionalInterface
    protected interface ConditionGetter {
        Integer get(Object stationKey, CommonModel model, int modeIndex, Object fromStationKey, Object classKey);
    }

    // =========================================================================================
    // --- SHARED PARSING (XML -> DTO -> Model) ---
    // =========================================================================================
    protected Object parseServiceStrategy(ParameterDescriptor p, MappingContext ctx) {
        if (p == null) return null;
        String cp = p.getClassPath();

        if (cp != null && (cp.contains("ZeroStrategy") || ZeroStrategy.getEngineClassPath().equals(cp)))
            return new ZeroStrategy();
        if (cp != null && (cp.contains("DisabledStrategy") || DisabledStrategy.getEngineClassPath().equals(cp)))
            return new DisabledStrategy();

        if (cp != null && (cp.contains("LoadDependent") || LDStrategy.getEngineClassPath().equals(cp))) {
            return parseLDStrategy(p, ctx);
        }

        List<ParameterDescriptor> distribution = getSubParams(p);
        if (distribution.isEmpty()) return null;
        return parseDistribution(distribution.get(0), distribution.size() > 1 ? distribution.get(1) : null, ctx);
    }

    private LDStrategy parseLDStrategy(ParameterDescriptor p, MappingContext ctx) {
        LDStrategy strategy = new LDStrategy();
        ParameterDescriptor ldArray = getFirst(getSubParams(p));
        if (ldArray != null) getSubParams(ldArray).forEach(range -> parseLDRange(range, strategy, ctx));
        return strategy;
    }

    private void parseLDRange(ParameterDescriptor range, LDStrategy strategy, MappingContext ctx) {
        List<ParameterDescriptor> rParams = getSubParams(range);
        int from = valI(getAt(rParams, 0));
        Distribution distr = parseDistribution(getAt(rParams, 1), getAt(rParams, 2), ctx);
        String mean = val(getAt(rParams, 3));

        Object rKey = (from == 1) ? strategy.getAllRanges()[0] : strategy.addRange();
        if (from != 1) strategy.setRangeFrom(rKey, from);
        strategy.setRangeDistributionNoCheck((from != 1) ? strategy.getAllRanges()[strategy.getRangeNumber() - 1] : rKey, distr);
        strategy.setRangeDistributionMeanNoCheck((from != 1) ? strategy.getAllRanges()[strategy.getRangeNumber() - 1] : rKey, mean);
    }

    protected Distribution parseDistribution(ParameterDescriptor distr, ParameterDescriptor distrPar, MappingContext ctx) {
        if (distr == null) return null;

        Distribution template = findDistributionTemplate(distr.getClassPath(), ctx);
        if (template == null) return null;

        Distribution dist = template.clone();
        List<ParameterDescriptor> params = new ArrayList<>(getSubParams(distr));
        if (distrPar != null) params.addAll(getSubParams(distrPar));

        for (ParameterDescriptor p : params) {
            parseDistributionParam(p, dist, ctx);
        }
        dist.updateCM();
        return dist;
    }

    private Distribution findDistributionTemplate(String cp, MappingContext ctx) {
        Distribution template = ctx.getEngineToGuiDistr().get(cp);
        if (template == null && cp != null) {
            for (String key : ctx.getEngineToGuiDistr().keySet()) {
                if (cp.endsWith(key) || key.endsWith(cp)) {
                    return ctx.getEngineToGuiDistr().get(key);
                }
            }
        }
        return template;
    }

    private void parseDistributionParam(ParameterDescriptor p, Distribution dist, MappingContext ctx) {
        String name = p.getName();
        if ("jmt.engine.random.DistributionContainer".equals(p.getClassPath())) {
            List<ParameterDescriptor> nested = getSubParams(p);
            if (!nested.isEmpty()) {
                dist.getParameter(name).setValue(parseDistribution(getAt(nested, 0), getAt(nested, 1), ctx));
            }
        } else if (p.isArray()) {
            parseDistributionArrayParam(p, dist, name);
        } else {
            String valStr = val(p);
            if (valStr != null) dist.getParameter(name).setValue(valStr);
        }
    }

    private void parseDistributionArrayParam(ParameterDescriptor p, Distribution dist, String name) {
        List<ParameterDescriptor> vectors = getSubParams(p);
        Object[][] val = new Object[vectors.size()][];

        for (int j = 0; j < vectors.size(); j++) {
            List<ParameterDescriptor> entries = getSubParams(getAt(vectors, j));
            val[j] = new Object[entries.size()];
            for (int k = 0; k < entries.size(); k++) {
                String valueStr = val(getAt(entries, k));
                val[j][k] = dist.getParameter(name).parseValue(valueStr != null ? valueStr : "0");
            }
        }
        dist.getParameter(name).setValue(val);
    }

    protected void extractTransitionConditions(Object key, ParameterDescriptor en, ParameterDescriptor in, ParameterDescriptor res, MappingContext ctx) {
        if (en != null) populateConditionMap(key, en, ctx.enablingConditionMap, ctx);
        if (in != null) populateConditionMap(key, in, ctx.inhibitingConditionMap, ctx);
        if (res != null) populateConditionMap(key, res, ctx.resourceConditionMap, ctx);
    }

    private void populateConditionMap(Object key, ParameterDescriptor condParam, Map<Object[], Integer> targetMap, MappingContext ctx) {
        List<ParameterDescriptor> modes = getSubParams(condParam);
        for (int i = 0; i < modes.size(); i++) {
            final int modeIndex = i;

            List<ParameterDescriptor> modeElements = getSubParams(modes.get(i));
            if (modeElements.isEmpty()) continue;

            for (ParameterDescriptor vector : getSubParams(modeElements.get(0))) {
                List<ParameterDescriptor> vectorElements = getSubParams(vector);
                if (vectorElements.size() < 2) continue;

                String stationName = val(vectorElements.get(0));
                getRefClassMap(vectorElements.get(1)).forEach((className, entryParam) -> {
                    Integer vVal = valI(entryParam);
                    if (stationName != null && vVal != null) {
                        targetMap.put(new Object[]{key, modeIndex, stationName, ctx.classes.get(className)}, vVal);
                    }
                });
            }
        }
    }
}