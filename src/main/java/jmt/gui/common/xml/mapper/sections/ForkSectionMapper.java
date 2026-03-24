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
        if (sec.getParameters() == null) {
            return;
        }

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

                if (ctx.engineToGuiFork == null) {
                    initGuiForkCache(ctx);
                }

                getRefClassMap(p).forEach((className, fP) -> {
                    Object classKey = ctx.classes.get(className);

                    ctx.engineToGuiFork.values().stream()
                            .filter(fs -> fP.getClassPath().equals(fs.getClassPath()))
                            .findFirst()
                            .ifPresent(fs -> model.setForkStrategy(key, classKey, fs.clone()));

                    ForkStrategy fs = (ForkStrategy) model.getForkStrategy(key, classKey);

                    if (fs instanceof ProbabilitiesFork) {
                        Map<String, OutPath> map = new HashMap<>();
                        for (ParameterDescriptor entry : getSubParams(getFirst(getSubParams(fP)))) {
                            OutPath op = new OutPath();
                            Double pVal = valD(getAt(getSubParams(getFirst(getSubParams(entry))), 1));
                            if (pVal != null) op.setProbability(pVal);

                            Map<Object, Object> opMap = new HashMap<>();
                            for (ParameterDescriptor res : getSubParams(getAt(getSubParams(entry), 1))) {
                                Integer rKey = valI(res);
                                Double rVal = valD(getAt(getSubParams(res), 1));
                                if (rKey != null && rVal != null) opMap.put(rKey, rVal);
                            }
                            op.setOutParameters(opMap);
                            String sName = val(getFirst(getSubParams(entry)));
                            if (sName != null) map.put(sName, op);
                        }
                        ctx.empiricalFork.put(new Object[]{key, classKey}, map);

                    } else if (fs instanceof CombFork) {
                        Map<String, Double> tmp = new TreeMap<>();
                        for (ParameterDescriptor e : getSubParams(getFirst(getSubParams(fP)))) {
                            String cName = val(e);
                            Double cVal = valD(getAt(getSubParams(e), 1));
                            if (cName != null && cVal != null) tmp.put(cName, cVal);
                        }
                        ctx.combFork.put(new Object[]{key, classKey}, tmp);

                    } else if (fs instanceof ClassSwitchFork || fs instanceof MultiBranchClassSwitchFork) {
                        Map<String, OutPath> map = new HashMap<>();
                        for (ParameterDescriptor entry : getSubParams(getFirst(getSubParams(fP)))) {
                            OutPath op = new OutPath();
                            String sName = val(getFirst(getSubParams(entry)));

                            List<ParameterDescriptor> classParams = getSubParams(getAt(getSubParams(entry), 1));
                            List<ParameterDescriptor> numParams = getSubParams(getAt(getSubParams(entry), 2));

                            Map<Object, Object> opMap = new HashMap<>();
                            for (int k = 0; k < classParams.size(); k++) {
                                String targetClass = val(getAt(classParams, k));
                                Integer count = valI(getAt(numParams, k));
                                if (targetClass != null && count != null) {
                                    opMap.put(model.getClassByName(targetClass), count);
                                }
                            }
                            op.setOutParameters(opMap);
                            if (sName != null) {
                                map.put(sName, op);
                            }
                        }
                        ctx.empiricalFork.put(new Object[]{key, classKey}, map);
                    }
                });
            }
        }
    }
}