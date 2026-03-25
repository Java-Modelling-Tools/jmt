package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.forkStrategies.*;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ForkSectionMapper extends AbstractSectionMapper {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        if (sec.getParameters() == null) return;

        for (ParameterDescriptor p : sec.getParameters()) {
            mapForkParameter(p, key, model, ctx);
        }
    }

    private void mapForkParameter(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        if (XMLConstantNames.PARAM_JOBS_PER_LINK.equals(p.getName())) {
            Integer v = valI(p);
            if (v != null) model.setStationNumberOfServers(key, v);
        } else if (XMLConstantNames.PARAM_BLOCK.equals(p.getName())) {
            Integer v = valI(p);
            if (v != null) model.setForkBlock(key, v);
        } else if (XMLConstantNames.PARAM_IS_SIMPLIFIED_FORK.equals(p.getName())) {
            String bVal = val(p);
            if (bVal != null) model.setIsSimplifiedFork(key, Boolean.parseBoolean(bVal));
        } else if (XMLConstantNames.PARAM_FORK_STRATEGY.equals(p.getName())) {
            getRefClassMap(p).forEach((className, fP) -> mapForkStrategy(className, fP, key, model, ctx));
        }
    }

    private void mapForkStrategy(String className, ParameterDescriptor fP, Object key, CommonModel model, MappingContext ctx) {
        Object classKey = ctx.classes.get(className);
        String cp = fP.getClassPath();

        // Identical to the Oracle: clones from the registry based on the classPath
        for (ForkStrategy strat : ctx.getEngineToGuiFork().values()) {
            if (cp != null && cp.equals(strat.getClassPath())) {
                model.setForkStrategy(key, classKey, strat.clone());
            }
        }

        ForkStrategy fs = (ForkStrategy) model.getForkStrategy(key, classKey);

        if (fs instanceof ProbabilitiesFork) {
            mapProbabilitiesFork(key, classKey, fP, ctx);
        } else if (fs instanceof CombFork) {
            mapCombFork(key, classKey, fP, ctx);
        } else if (fs instanceof ClassSwitchFork || fs instanceof MultiBranchClassSwitchFork) {
            mapClassSwitchFork(key, classKey, fP, model, ctx);
        }
    }

    private void mapProbabilitiesFork(Object key, Object classKey, ParameterDescriptor fP, MappingContext ctx) {
        Map<String, OutPath> map = new HashMap<>();
        ParameterDescriptor entryArray = getFirst(getSubParams(fP));
        if (entryArray != null) {
            for (ParameterDescriptor entry : getSubParams(entryArray)) {
                parseProbabilitiesOutPath(entry, map);
            }
        }
        ctx.empiricalFork.put(new Object[]{key, classKey}, map);
    }

    private void parseProbabilitiesOutPath(ParameterDescriptor entry, Map<String, OutPath> map) {
        OutPath op = new OutPath();
        List<ParameterDescriptor> entryParams = getSubParams(entry);

        // FIX ORACLE: entryParams.get(0) is "outUnitProbability", which contains stationName and probability!
        ParameterDescriptor outUnitProb = getAt(entryParams, 0);
        List<ParameterDescriptor> outUnitSub = getSubParams(outUnitProb);

        String sName = val(getAt(outUnitSub, 0));
        Double pVal = valD(getAt(outUnitSub, 1));

        if (pVal != null) op.setProbability(pVal);

        // entryParams.get(1) is "JobsPerLinkDis"
        ParameterDescriptor jobsPerLinkDis = getAt(entryParams, 1);
        op.setOutParameters(mapJobsPerLinkDis(jobsPerLinkDis));
        if (sName != null) map.put(sName, op);
    }

    private Map<Object, Object> mapJobsPerLinkDis(ParameterDescriptor jobsPerLinkDis) {
        Map<Object, Object> opMap = new HashMap<>();
        if (jobsPerLinkDis != null) {
            for (ParameterDescriptor res : getSubParams(jobsPerLinkDis)) {
                List<ParameterDescriptor> resParams = getSubParams(res);
                Integer rKey = valI(getAt(resParams, 0));
                Double rVal = valD(getAt(resParams, 1));
                if (rKey != null && rVal != null) opMap.put(rKey, rVal);
            }
        }
        return opMap;
    }

    private void mapCombFork(Object key, Object classKey, ParameterDescriptor fP, MappingContext ctx) {
        Map<String, Double> tmp = new TreeMap<>();
        ParameterDescriptor entryArray = getFirst(getSubParams(fP));
        if (entryArray != null) {
            for (ParameterDescriptor e : getSubParams(entryArray)) {
                List<ParameterDescriptor> eParams = getSubParams(e);
                String cName = val(getAt(eParams, 0));
                Double cVal = valD(getAt(eParams, 1));
                if (cName != null && cVal != null) tmp.put(cName, cVal);
            }
        }
        ctx.combFork.put(new Object[]{key, classKey}, tmp);
    }

    private void mapClassSwitchFork(Object key, Object classKey, ParameterDescriptor fP, CommonModel model, MappingContext ctx) {
        Map<String, OutPath> map = new HashMap<>();
        ParameterDescriptor entryArray = getFirst(getSubParams(fP));
        if (entryArray != null) {
            for (ParameterDescriptor entry : getSubParams(entryArray)) {
                OutPath op = new OutPath();
                List<ParameterDescriptor> entryParams = getSubParams(entry);

                String sName = val(getAt(entryParams, 0));
                List<ParameterDescriptor> classParams = getSubParams(getAt(entryParams, 1));
                List<ParameterDescriptor> numParams = getSubParams(getAt(entryParams, 2));

                Map<Object, Object> opMap = new HashMap<>();
                for (int k = 0; k < classParams.size(); k++) {
                    String targetClass = val(getAt(classParams, k));
                    Integer count = valI(getAt(numParams, k));
                    if (targetClass != null && count != null) {
                        opMap.put(model.getClassByName(targetClass), count);
                    }
                }
                op.setOutParameters(opMap);
                if (sName != null) map.put(sName, op);
            }
        }
        ctx.empiricalFork.put(new Object[]{key, classKey}, map);
    }
}