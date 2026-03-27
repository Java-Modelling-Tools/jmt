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
;

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
        ParameterDescriptor serviceStrategy = new ParameterDescriptor();
        serviceStrategy.setName("ServiceStrategy");
        serviceStrategy.setClassPath("jmt.engine.random.ServiceStrategy");
        serviceStrategy.setArray(true); // FIX: Must be true for maps/arrays in legacy XML

        for (Object classKey : model.getClassKeys()) {
            Object serviceDist = model.getServiceTimeDistribution(key, classKey);
            if (serviceDist != null) {
                ParameterDescriptor stStrategy = getDistributionParameter(serviceDist, model, classKey, "ServiceTimeStrategy", "jmt.engine.random.ServiceTimeStrategy", ctx);
                serviceStrategy.addSubParameter(stStrategy);
            }
        }
        if (!serviceStrategy.getSubParameters().isEmpty()) {
            sec.addParameter(serviceStrategy);
        }
    }

    // =========================================================================================
    // --- SHARED WRITING (Model -> DTO -> XML) ---
    // =========================================================================================

    protected ParameterDescriptor getDistributionParameter(Object distrObj, CommonModel model, Object classKey, String paramName, String classPath, WritingContext ctx) {
        ParameterDescriptor p = new ParameterDescriptor();
        p.setName(paramName);
        p.setClassPath(classPath);
        if (classKey != null) {
            p.setRefClass(model.getClassName(classKey));
        }

        // FIX 1: Se l'oggetto è null, emettiamo la ZeroStrategy legacy
        if (distrObj == null) {
            p.setName("ZeroServiceTimeStrategy");
            p.setClassPath(ZeroStrategy.getEngineClassPath());
            return p;
        }

        if (distrObj instanceof Distribution) {
            Distribution dist = (Distribution) distrObj;
            ParameterDescriptor distDesc = new ParameterDescriptor();
            distDesc.setName(dist.getName());
            distDesc.setClassPath(dist.getClassPath());

            ParameterDescriptor distParDesc = new ParameterDescriptor();
            distParDesc.setName("distrPar");
            distParDesc.setClassPath(dist.getParameterClassPath());

            for (int i = 0; i < dist.getNumberOfParameters(); i++) {
                Distribution.Parameter param = dist.getParameter(i);
                ParameterDescriptor paramDesc = createParameterDescriptor(param, ctx);
                if (param.isDirectParameter()) distDesc.addSubParameter(paramDesc);
                else distParDesc.addSubParameter(paramDesc);
            }
            p.addSubParameter(distDesc);
            p.addSubParameter(distParDesc);
        } else if (distrObj instanceof ZeroStrategy) {
            p.setName("ZeroServiceTimeStrategy"); // FIX 1
            p.setClassPath(ZeroStrategy.getEngineClassPath());
        } else if (distrObj instanceof DisabledStrategy) {
            p.setName("DisabledServiceTimeStrategy"); // FIX 1
            p.setClassPath(DisabledStrategy.getEngineClassPath());
        } else if (distrObj instanceof LDStrategy) {
            p.setName("LoadDependentStrategy"); // FIX 4: Il legacy reader esige questo nome esatto!
            p.setClassPath("jmt.engine.NetStrategies.ServiceStrategies.LoadDependentStrategy");
            LDStrategy strategy = (LDStrategy) distrObj;
            ParameterDescriptor ldArray = new ParameterDescriptor();
            ldArray.setName("LDParameter");
            ldArray.setClassPath("jmt.engine.NetStrategies.ServiceStrategies.LDParameter");
            ldArray.setArray(true);
            for (Object rKey : strategy.getAllRanges()) {
                ParameterDescriptor range = new ParameterDescriptor();
                range.setName("LDParameter");
                range.setClassPath("jmt.engine.NetStrategies.ServiceStrategies.LDParameter");

                ParameterDescriptor from = new ParameterDescriptor();
                from.setName("from");
                from.setClassPath("java.lang.Integer");
                from.setValue(String.valueOf(strategy.getRangeFrom(rKey)));
                range.addSubParameter(from);

                Distribution d = strategy.getRangeDistribution(rKey);
                ParameterDescriptor dP = getDistributionParameter(d, model, null, "distr", "jmt.engine.random.Distribution", ctx);
                if (dP != null && dP.getSubParameters().size() >= 2) {
                    range.addSubParameter(dP.getSubParameters().get(0));
                    range.addSubParameter(dP.getSubParameters().get(1));
                }

                ParameterDescriptor mean = new ParameterDescriptor();
                mean.setName("mean");
                mean.setClassPath("java.lang.Double");
                mean.setValue(String.valueOf(strategy.getRangeDistributionMean(rKey)));
                range.addSubParameter(mean);

                ldArray.addSubParameter(range);
            }
            p.addSubParameter(ldArray);
        }
        return p;
    }

    private ParameterDescriptor createParameterDescriptor(Distribution.Parameter param, WritingContext ctx) {
        ParameterDescriptor p = new ParameterDescriptor();
        p.setName(param.getName());
        Object val = param.getValue();

        if (val instanceof Distribution) {
            p.setClassPath("jmt.engine.random.DistributionContainer");
            ParameterDescriptor nested = getDistributionParameter(val, null, null, "distr", "jmt.engine.random.Distribution", ctx);
            p.addSubParameter(nested.getSubParameters().get(0));
            p.addSubParameter(nested.getSubParameters().get(1));
        } else if (val instanceof Object[][]) {
            p.setArray(true);
            p.setClassPath(param.getValueClass().getName());
            Object[][] matrix = (Object[][]) val;
            for (Object[] row : matrix) {
                ParameterDescriptor rowP = new ParameterDescriptor();
                rowP.setName(param.getName());
                rowP.setClassPath(param.getValueClass().getName());
                for (Object cell : row) {
                    ParameterDescriptor cellP = new ParameterDescriptor();
                    cellP.setName(param.getName());
                    cellP.setClassPath(param.getValueClass().getName());
                    cellP.setValue(cell != null ? cell.toString() : "0");
                    rowP.addSubParameter(cellP);
                }
                p.addSubParameter(rowP);
            }
        } else {
            p.setClassPath(param.getValueClass().getName());
            if (val != null) p.setValue(val.toString());
        }
        return p;
    }

    protected void mapTransitionConditions(Object key, SectionDescriptor sec, CommonModel model, WritingContext ctx) {
        mapTransitionConditions(key, sec, model, ctx, true);
    }

    protected void mapTransitionConditions(Object key, SectionDescriptor sec, CommonModel model, WritingContext ctx, boolean includeResource) {
        ParameterDescriptor enablingConditions = createConditionMatrix("enablingConditions", "enablingCondition", "enablingVector", "enablingEntry", key, model, ctx, (s, m, i, from, cl) -> model.getEnablingCondition(s, i, from, cl));
        if (enablingConditions != null) sec.addParameter(enablingConditions);

        ParameterDescriptor inhibitingConditions = createConditionMatrix("inhibitingConditions", "inhibitingCondition", "inhibitingVector", "inhibitingEntry", key, model, ctx, (s, m, i, from, cl) -> model.getInhibitingCondition(s, i, from, cl));
        if (inhibitingConditions != null) sec.addParameter(inhibitingConditions);

        if (includeResource) {
            ParameterDescriptor resourceConditions = createConditionMatrix("resourceConditions", "resourceCondition", "resourceVector", "resourceEntry", key, model, ctx, (s, m, i, from, cl) -> model.getResourceCondition(s, i, from, cl));
            if (resourceConditions != null) sec.addParameter(resourceConditions);
        }
    }

    protected ParameterDescriptor createConditionMatrix(String name, String condName, String vecName, String entryName, Object key, CommonModel model, WritingContext ctx, ConditionGetter getter) {
        return createConditionMatrix(name, condName, vecName, entryName, key, model, ctx, getter, true);
    }

    protected ParameterDescriptor createConditionMatrix(String name, String condName, String vecName, String entryName, Object key, CommonModel model, WritingContext ctx, ConditionGetter getter, boolean backward) {
        int modes = model.getTransitionModeListSize(key);
        if (modes <= 0) return null;

        List<Object> stationKeys = backward ? model.getBackwardConnections(key) : model.getForwardConnections(key);
        if (stationKeys == null || stationKeys.isEmpty()) return null;

        ParameterDescriptor matrix = new ParameterDescriptor();
        matrix.setName(name);
        matrix.setClassPath("jmt.engine.NetStrategies.TransitionUtilities.TransitionMatrix");

        List<Object> classKeys = model.getClassKeys();
        boolean hasNonZero = false; // FIX: Do not emit huge empty matrices for non-transitions

        for (int i = 0; i < modes; i++) {
            ParameterDescriptor condition = new ParameterDescriptor();
            condition.setName(condName);
            condition.setClassPath("jmt.engine.NetStrategies.TransitionUtilities.TransitionMatrix");

            ParameterDescriptor vectors = new ParameterDescriptor();
            vectors.setName(vecName + "s");
            vectors.setClassPath("jmt.engine.NetStrategies.TransitionUtilities.TransitionVector");

            for (Object stationKey : stationKeys) {
                ParameterDescriptor vector = new ParameterDescriptor();
                vector.setName(vecName);
                vector.setClassPath("jmt.engine.NetStrategies.TransitionUtilities.TransitionVector");

                ParameterDescriptor stationName = new ParameterDescriptor();
                stationName.setName("stationName");
                stationName.setClassPath("java.lang.String");
                stationName.setValue(model.getStationName(stationKey));
                vector.addSubParameter(stationName);

                ParameterDescriptor entries = new ParameterDescriptor();
                entries.setName(entryName + "ies");
                entries.setClassPath("java.lang.Integer");

                for (Object classKey : classKeys) {
                    Integer val = getter.get(key, model, i, stationKey, classKey);
                    if (val != null && val > 0) hasNonZero = true;

                    ParameterDescriptor entry = new ParameterDescriptor();
                    entry.setName(entryName);
                    entry.setClassPath("java.lang.Integer");
                    entry.setRefClass(model.getClassName(classKey));
                    entry.setValue(val != null ? val.toString() : "0");
                    entries.addSubParameter(entry);
                }
                vector.addSubParameter(entries);
                vectors.addSubParameter(vector);
            }
            condition.addSubParameter(vectors);
            matrix.addSubParameter(condition);
        }
        return hasNonZero ? matrix : null;
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

        // FIX 4: Il vecchio classpath restituiva "LoadDependentStrategy", non "LDStrategy"
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
        String cp = distr.getClassPath();
        Distribution template = ctx.getEngineToGuiDistr().get(cp);
        if (template == null && cp != null) {
            for (String key : ctx.getEngineToGuiDistr().keySet()) {
                if (cp.endsWith(key) || key.endsWith(cp)) {
                    template = ctx.getEngineToGuiDistr().get(key);
                    break;
                }
            }
        }
        if (template == null) return null;

        Distribution dist = template.clone();
        List<ParameterDescriptor> params = new ArrayList<>(getSubParams(distr));
        if (distrPar != null) params.addAll(getSubParams(distrPar));

        for (ParameterDescriptor p : params) {
            String name = p.getName();
            if ("jmt.engine.random.DistributionContainer".equals(p.getClassPath())) {
                List<ParameterDescriptor> nested = getSubParams(p);
                if (!nested.isEmpty())
                    dist.getParameter(name).setValue(parseDistribution(getAt(nested, 0), getAt(nested, 1), ctx));
            } else if (p.isArray()) {
                List<ParameterDescriptor> vectors = getSubParams(p);
                Object[][] val = new Object[vectors.size()][];
                for (int j = 0; j < vectors.size(); j++) {
                    List<ParameterDescriptor> entries = getSubParams(getAt(vectors, j));
                    val[j] = new Object[entries.size()];
                    for (int k = 0; k < entries.size(); k++) {
                        String valueStr = val(getAt(entries, k));
                        val[j][k] = dist.getParameter(p.getName()).parseValue(valueStr != null ? valueStr : "0");
                    }
                }
                dist.getParameter(name).setValue(val);
            } else {
                String valStr = val(p);
                if (valStr != null) dist.getParameter(name).setValue(valStr);
            }
        }
        dist.updateCM();
        return dist;
    }

    protected void extractTransitionConditions(Object key, ParameterDescriptor en, ParameterDescriptor in, ParameterDescriptor res, MappingContext ctx) {
        if (en != null) populateConditionMap(key, en, ctx.enablingConditionMap, ctx);
        if (in != null) populateConditionMap(key, in, ctx.inhibitingConditionMap, ctx);
        if (res != null) populateConditionMap(key, res, ctx.resourceConditionMap, ctx);
    }

    private void populateConditionMap(Object key, ParameterDescriptor condParam, Map<Object[], Integer> targetMap, MappingContext ctx) {
        List<ParameterDescriptor> modes = getSubParams(condParam);
        for (int i = 0; i < modes.size(); i++) {
            final int modeIndex = i; // <--- FIX: Create an effectively final copy of the loop counter

            List<ParameterDescriptor> modeElements = getSubParams(modes.get(i));
            if (modeElements.isEmpty()) continue;

            for (ParameterDescriptor vector : getSubParams(modeElements.get(0))) {
                List<ParameterDescriptor> vectorElements = getSubParams(vector);
                if (vectorElements.size() < 2) continue;

                String stationName = val(vectorElements.get(0));
                getRefClassMap(vectorElements.get(1)).forEach((className, entryParam) -> {
                    Integer vVal = valI(entryParam);
                    if (stationName != null && vVal != null) {
                        // Use the effectively final 'modeIndex' instead of 'i'
                        targetMap.put(new Object[]{key, modeIndex, stationName, ctx.classes.get(className)}, vVal);
                    }
                });
            }
        }
    }
}