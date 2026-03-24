package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
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

        if (cp != null && (cp.contains("ZeroStrategy") || ZeroStrategy.getEngineClassPath().equals(cp))) return new ZeroStrategy();
        if (cp != null && (cp.contains("DisabledStrategy") || DisabledStrategy.getEngineClassPath().equals(cp))) return new DisabledStrategy();

        if (cp != null && (cp.contains("LDStrategy") || LDStrategy.getEngineClassPath().equals(cp))) {
            LDStrategy strategy = new LDStrategy();
            ParameterDescriptor ldArray = getFirst(getSubParams(p));
            if (ldArray != null) {
                getSubParams(ldArray).forEach(range -> {
                    List<ParameterDescriptor> rParams = getSubParams(range);
                    int from = valI(getAt(rParams, 0));
                    Distribution distr = parseDistribution(getAt(rParams, 1), getAt(rParams, 2), ctx);
                    String mean = val(getAt(rParams, 3));

                    Object rKey = (from == 1) ? strategy.getAllRanges()[0] : strategy.addRange();
                    if (from != 1) strategy.setRangeFrom(rKey, from);
                    strategy.setRangeDistributionNoCheck((from != 1) ? strategy.getAllRanges()[strategy.getRangeNumber() - 1] : rKey, distr);
                    strategy.setRangeDistributionMeanNoCheck((from != 1) ? strategy.getAllRanges()[strategy.getRangeNumber() - 1] : rKey, mean);
                });
            }
            return strategy;
        }

        List<ParameterDescriptor> distribution = getSubParams(p);
        if (distribution.isEmpty()) return null;
        return parseDistribution(distribution.get(0), distribution.size() > 1 ? distribution.get(1) : null, ctx);
    }

    protected Distribution parseDistribution(ParameterDescriptor distr, ParameterDescriptor distrPar, MappingContext ctx) {
        if (distr == null) return null;
        if (ctx.engineToGuiDistr == null) initGuiDistrCache(ctx);

        String cp = distr.getClassPath();
        Distribution template = ctx.engineToGuiDistr.get(cp);
        if (template == null && cp != null) {
            for (String key : ctx.engineToGuiDistr.keySet()) {
                if (cp.endsWith(key) || key.endsWith(cp)) { template = ctx.engineToGuiDistr.get(key); break; }
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
                if (!nested.isEmpty()) dist.getParameter(name).setValue(parseDistribution(getAt(nested, 0), getAt(nested, 1), ctx));
            } else if (p.isArray()) {
                List<ParameterDescriptor> vectors = getSubParams(p);
                Object[][] val = new Object[vectors.size()][];
                for (int j = 0; j < vectors.size(); j++) {
                    List<ParameterDescriptor> entries = getSubParams(getAt(vectors, j));
                    val[j] = new Object[entries.size()];
                    for (int k = 0; k < entries.size(); k++) {
                        String valueStr = val(getAt(entries, k));
                        val[j][k] = (valueStr != null) ? dist.getParameter(name).parseValue(valueStr) : dist.getParameter(name).parseValue("0");
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
        if (condParam == null) {
            return;
        }

        // Level 1: the direct children are the individual conditions for mode (e.g. "enablingCondition")
        List<ParameterDescriptor> modes = getSubParams(condParam);

        for (int i = 0; i < modes.size(); i++) {
            ParameterDescriptor condition = modes.get(i);

            // Level 2: inside "enablingCondition" there is a container (e.g. "enablingVectors")
            List<ParameterDescriptor> conditionElements = getSubParams(condition);
            if (conditionElements.isEmpty()) continue;

            ParameterDescriptor vectorsArray = conditionElements.get(0);

            // Level 3: the children of the container are the individual vectors (e.g. "enablingVector")
            List<ParameterDescriptor> vectors = getSubParams(vectorsArray);

            for (ParameterDescriptor vector : vectors) {
                // Level 4: inside the vector there are stationName and the refClass map (e.g. "enablingEntries")
                List<ParameterDescriptor> vectorElements = getSubParams(vector);

                if (vectorElements.size() < 2) continue;

                String stationName = val(vectorElements.get(0));
                ParameterDescriptor entriesParam = vectorElements.get(1);

                Map<String, ParameterDescriptor> refMap = getRefClassMap(entriesParam);

                for (Map.Entry<String, ParameterDescriptor> entry : refMap.entrySet()) {
                    String className = entry.getKey();
                    Integer vVal = valI(entry.getValue());

                    if (stationName != null && vVal != null) {
                        Object classKey = ctx.classes.get(className);
                        Object[] quadKey = new Object[]{key, i, stationName, classKey};
                        targetMap.put(quadKey, vVal);
                    }
                }
            }
        }
    }

    // --- CACHE ---
    protected void initGuiRoutingCache(MappingContext ctx) { ctx.engineToGuiRouting = new TreeMap<>(); for (RoutingStrategy rs : RoutingStrategy.findAll()) ctx.engineToGuiRouting.put(rs.getClass().getName(), rs); }
    protected void initGuiForkCache(MappingContext ctx) { ctx.engineToGuiFork = new TreeMap<>(); for (ForkStrategy fs : ForkStrategy.findAll()) ctx.engineToGuiFork.put(fs.getClass().getName(), fs); }
    protected void initGuiJoinCache(MappingContext ctx) { ctx.engineToGuiJoin = new TreeMap<>(); for (JoinStrategy js : JoinStrategy.findAll()) ctx.engineToGuiJoin.put(js.getClass().getName(), js); }
    protected void initGuiSemaphoreCache(MappingContext ctx) { ctx.engineToGuiSemaphore = new TreeMap<>(); for (SemaphoreStrategy ss : SemaphoreStrategy.findAll()) ctx.engineToGuiSemaphore.put(ss.getClass().getName(), ss); }
    private void initGuiDistrCache(MappingContext ctx) { ctx.engineToGuiDistr = new TreeMap<>(); for (Distribution d : Distribution.findAll()) ctx.engineToGuiDistr.put(d.getClassPath(), d); for (Distribution d : Distribution.findAllDiscrete()) ctx.engineToGuiDistr.put(d.getClassPath(), d); }
}