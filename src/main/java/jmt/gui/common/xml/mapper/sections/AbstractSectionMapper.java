package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

import java.util.*;

public abstract class AbstractSectionMapper {

    public abstract void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx);

    // --- UTILITY ---
    protected ParameterDescriptor getParam(SectionDescriptor sec, String name) {
        if (sec == null || sec.getParameters() == null) return null;
        return sec.getParameters().stream().filter(p -> name.equals(p.getName())).findFirst().orElse(null);
    }

    protected List<ParameterDescriptor> getSubParams(ParameterDescriptor p) {
        return (p != null && p.getSubParameters() != null) ? p.getSubParameters() : Collections.emptyList();
    }

    // CRITICAL FIX RESTORED: Use the native getRefClass() of the DTO!
    protected Map<String, ParameterDescriptor> getRefClassMap(ParameterDescriptor p) {
        Map<String, ParameterDescriptor> map = new LinkedHashMap<>();
        if (p == null || p.getSubParameters() == null) return map;

        for (ParameterDescriptor sub : p.getSubParameters()) {
            String refClass = sub.getRefClass();
            if (refClass != null && !refClass.isEmpty()) {
                map.put(refClass, sub);
            }
        }
        return map;
    }

    protected ParameterDescriptor getFirst(List<ParameterDescriptor> list) {
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    protected ParameterDescriptor getAt(List<ParameterDescriptor> list, int index) {
        return (list != null && index >= 0 && index < list.size()) ? list.get(index) : null;
    }

    protected String val(ParameterDescriptor p) {
        if (p == null) return null;
        if (p.getValue() != null && !p.getValue().trim().isEmpty()) return p.getValue().trim();
        for (ParameterDescriptor sub : getSubParams(p)) {
            if ("value".equals(sub.getName()) && sub.getValue() != null) return sub.getValue().trim();
        }
        return null;
    }

    protected Double valD(ParameterDescriptor p) { String v = val(p); return v != null ? Double.valueOf(v) : null; }
    protected Integer valI(ParameterDescriptor p) { String v = val(p); return v != null ? Double.valueOf(v).intValue() : null; }

    // --- SHARED PARSING ---
    protected Object parseServiceStrategy(ParameterDescriptor p, MappingContext ctx) {
        if (p == null) return null;
        String cp = p.getClassPath();

        if (isZeroStrategy(cp)) return new ZeroStrategy();
        if (isDisabledStrategy(cp)) return new DisabledStrategy();

        if (isLDStrategy(cp)) {
            return parseLDStrategy(p, ctx);
        }

        List<ParameterDescriptor> distribution = getSubParams(p);
        if (distribution.isEmpty()) return null;
        return parseDistribution(distribution.get(0), distribution.size() > 1 ? distribution.get(1) : null, ctx);
    }

    private boolean isZeroStrategy(String cp) {
        return cp != null && (cp.contains("ZeroStrategy") || ZeroStrategy.getEngineClassPath().equals(cp));
    }

    private boolean isDisabledStrategy(String cp) {
        return cp != null && (cp.contains("DisabledStrategy") || DisabledStrategy.getEngineClassPath().equals(cp));
    }

    private boolean isLDStrategy(String cp) {
        return cp != null && (cp.contains("LDStrategy") || LDStrategy.getEngineClassPath().equals(cp));
    }

    private LDStrategy parseLDStrategy(ParameterDescriptor p, MappingContext ctx) {
        LDStrategy strategy = new LDStrategy();
        ParameterDescriptor ldArray = getFirst(getSubParams(p));
        if (ldArray != null) {
            getSubParams(ldArray).forEach(range -> parseLDRange(range, strategy, ctx));
        }
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
        Distribution template = resolveTemplate(cp, ctx);
        if (template == null) return null;

        Distribution dist = template.clone();
        List<ParameterDescriptor> params = new ArrayList<>(getSubParams(distr));
        if (distrPar != null) params.addAll(getSubParams(distrPar));

        for (ParameterDescriptor p : params) {
            applyParameterToDistribution(dist, p, ctx);
        }
        dist.updateCM();
        return dist;
    }

    private Distribution resolveTemplate(String cp, MappingContext ctx) {
        Distribution template = ctx.getEngineToGuiDistr().get(cp);
        if (template == null && cp != null) {
            for (String key : ctx.getEngineToGuiDistr().keySet()) {
                if (cp.endsWith(key) || key.endsWith(cp)) {
                    template = ctx.getEngineToGuiDistr().get(key);
                    break;
                }
            }
        }
        return template;
    }

    private void applyParameterToDistribution(Distribution dist, ParameterDescriptor p, MappingContext ctx) {
        String name = p.getName();
        if ("jmt.engine.random.DistributionContainer".equals(p.getClassPath())) {
            List<ParameterDescriptor> nested = getSubParams(p);
            if (!nested.isEmpty()) {
                dist.getParameter(name).setValue(parseDistribution(getAt(nested, 0), getAt(nested, 1), ctx));
            }
        } else if (p.isArray()) {
            dist.getParameter(name).setValue(parseArrayParameter(p, dist));
        } else {
            String valStr = val(p);
            if (valStr != null) dist.getParameter(name).setValue(valStr);
        }
    }

    private Object[][] parseArrayParameter(ParameterDescriptor p, Distribution dist) {
        List<ParameterDescriptor> vectors = getSubParams(p);
        Object[][] val = new Object[vectors.size()][];
        for (int j = 0; j < vectors.size(); j++) {
            List<ParameterDescriptor> entries = getSubParams(getAt(vectors, j));
            val[j] = new Object[entries.size()];
            for (int k = 0; k < entries.size(); k++) {
                String valueStr = val(getAt(entries, k));
                val[j][k] = (valueStr != null) ? dist.getParameter(p.getName()).parseValue(valueStr) : dist.getParameter(p.getName()).parseValue("0");
            }
        }
        return val;
    }


// --- METHODS FOR PETRI NETS AND RESTORED CONDITIONS ---

    protected void extractTransitionConditions(Object key, ParameterDescriptor en, ParameterDescriptor in, ParameterDescriptor res, MappingContext ctx) {
        if (en != null) {
            populateConditionMap(key, en, ctx.enablingConditionMap, ctx);
        }
        if (in != null) {
            populateConditionMap(key, in, ctx.inhibitingConditionMap, ctx);
        }
        if (res != null) {
            populateConditionMap(key, res, ctx.resourceConditionMap, ctx);
        }
    }

    private void populateConditionMap(Object key, ParameterDescriptor condParam, Map<Object[], Integer> targetMap, MappingContext ctx) {
        if (condParam == null) return;

        List<ParameterDescriptor> modes = getSubParams(condParam);
        for (int i = 0; i < modes.size(); i++) {
            populateModeConditions(key, i, modes.get(i), targetMap, ctx);
        }
    }

    private void populateModeConditions(Object key, int modeIndex, ParameterDescriptor modeParam, Map<Object[], Integer> targetMap, MappingContext ctx) {
        List<ParameterDescriptor> conditionElements = getSubParams(modeParam);
        if (conditionElements.isEmpty()) return;

        ParameterDescriptor vectorsArray = conditionElements.get(0);
        List<ParameterDescriptor> vectors = getSubParams(vectorsArray);

        for (ParameterDescriptor vector : vectors) {
            populateVectorConditions(key, modeIndex, vector, targetMap, ctx);
        }
    }

    private void populateVectorConditions(Object key, int modeIndex, ParameterDescriptor vector, Map<Object[], Integer> targetMap, MappingContext ctx) {
        List<ParameterDescriptor> vectorElements = getSubParams(vector);
        if (vectorElements.size() < 2) return;

        String stationName = val(vectorElements.get(0));
        ParameterDescriptor entriesParam = vectorElements.get(1);

        Map<String, ParameterDescriptor> refMap = getRefClassMap(entriesParam);

        refMap.forEach((className, entryParam) -> {
            Integer vVal = valI(entryParam);
            if (stationName != null && vVal != null) {
                Object classKey = ctx.classes.get(className);
                Object[] quadKey = new Object[]{key, modeIndex, stationName, classKey};
                targetMap.put(quadKey, vVal);
            }
        });
    }
}