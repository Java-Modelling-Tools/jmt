package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.forkStrategies.*;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ForkSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        if (sec.getParameters() == null) return;

        for (ParameterDescriptor p : sec.getParameters()) {
            if (PARAM_JOBS_PER_LINK.equals(p.getName())) {
                Integer v = valI(p);
                if (v != null) model.setStationNumberOfServers(key, v);
            } else if (PARAM_BLOCK.equals(p.getName())) {
                Integer v = valI(p);
                if (v != null) model.setForkBlock(key, v);
            } else if (PARAM_IS_SIMPLIFIED_FORK.equals(p.getName())) {
                String bVal = val(p);
                if (bVal != null) model.setIsSimplifiedFork(key, Boolean.parseBoolean(bVal));
            } else if (PARAM_FORK_STRATEGY.equals(p.getName())) {

                if (ctx.engineToGuiFork == null) initGuiForkCache(ctx);

                getRefClassMap(p).forEach((className, fP) -> {
                    Object classKey = ctx.classes.get(className);
                    String cp = fP.getClassPath();

                    // Identico all'Oracolo: clona dal registry in base al classPath
                    for (ForkStrategy strat : ctx.engineToGuiFork.values()) {
                        if (cp != null && cp.equals(strat.getClassPath())) {
                            model.setForkStrategy(key, classKey, strat.clone());
                        }
                    }

                    ForkStrategy fs = (ForkStrategy) model.getForkStrategy(key, classKey);

                    if (fs instanceof ProbabilitiesFork) {
                        Map<String, OutPath> map = new HashMap<>();
                        ParameterDescriptor entryArray = getFirst(getSubParams(fP));
                        if (entryArray != null) {
                            for (ParameterDescriptor entry : getSubParams(entryArray)) {
                                OutPath op = new OutPath();
                                List<ParameterDescriptor> entryParams = getSubParams(entry);

                                // FIX ORACLE: entryParams.get(0) è "outUnitProbability", che contiene stationName e probability!
                                ParameterDescriptor outUnitProb = getAt(entryParams, 0);
                                List<ParameterDescriptor> outUnitSub = getSubParams(outUnitProb);

                                String sName = val(getAt(outUnitSub, 0));
                                Double pVal = valD(getAt(outUnitSub, 1));

                                if (pVal != null) op.setProbability(pVal);

                                Map<Object, Object> opMap = new HashMap<>();
                                // entryParams.get(1) è "JobsPerLinkDis"
                                ParameterDescriptor jobsPerLinkDis = getAt(entryParams, 1);
                                if (jobsPerLinkDis != null) {
                                    for (ParameterDescriptor res : getSubParams(jobsPerLinkDis)) {
                                        List<ParameterDescriptor> resParams = getSubParams(res);
                                        Integer rKey = valI(getAt(resParams, 0));
                                        Double rVal = valD(getAt(resParams, 1));
                                        if (rKey != null && rVal != null) opMap.put(rKey, rVal);
                                    }
                                }
                                op.setOutParameters(opMap);
                                if (sName != null) map.put(sName, op);
                            }
                        }
                        ctx.empiricalFork.put(new Object[]{key, classKey}, map);

                    } else if (fs instanceof CombFork) {
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

                    } else if (fs instanceof ClassSwitchFork || fs instanceof MultiBranchClassSwitchFork) {
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
                });
            }
        }
    }
}