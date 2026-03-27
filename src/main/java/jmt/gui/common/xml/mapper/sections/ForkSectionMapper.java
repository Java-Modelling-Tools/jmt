package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.forkStrategies.*;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ForkSectionMapper extends AbstractSectionMapper {

    private static final String EMPIRICAL_ENTRY_CLASS = "jmt.engine.random.EmpiricalEntry"; // FIX CLASS PATH

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        if (sec.getParameters() == null) return;

        for (ParameterDescriptor p : sec.getParameters()) {
            if (XMLConstantNames.PARAM_JOBS_PER_LINK.equals(p.getName())) {
                Integer v = valI(p); if (v != null) model.setStationNumberOfServers(key, v);
            } else if (XMLConstantNames.PARAM_BLOCK.equals(p.getName())) {
                Integer v = valI(p); if (v != null) model.setForkBlock(key, v);
            } else if (XMLConstantNames.PARAM_IS_SIMPLIFIED_FORK.equals(p.getName())) {
                String bVal = val(p); if (bVal != null) model.setIsSimplifiedFork(key, Boolean.parseBoolean(bVal));
            } else if (XMLConstantNames.PARAM_FORK_STRATEGY.equals(p.getName())) {
                getRefClassMap(p).forEach((className, fP) -> {
                    Object classKey = ctx.classes.get(className);
                    String cp = fP.getClassPath();

                    for (ForkStrategy strat : ctx.getEngineToGuiFork().values()) {
                        if (cp != null && cp.equals(strat.getClassPath())) model.setForkStrategy(key, classKey, strat.clone());
                    }

                    ForkStrategy fs = (ForkStrategy) model.getForkStrategy(key, classKey);
                    if (fs instanceof ProbabilitiesFork) {
                        Map<String, OutPath> map = new HashMap<>();
                        ParameterDescriptor entryArray = getFirst(getSubParams(fP));
                        if (entryArray != null) {
                            for (ParameterDescriptor entry : getSubParams(entryArray)) {
                                OutPath op = new OutPath();
                                List<ParameterDescriptor> entryParams = getSubParams(entry);
                                ParameterDescriptor outUnitProb = getAt(entryParams, 0);
                                List<ParameterDescriptor> outUnitSub = getSubParams(outUnitProb);
                                String sName = val(getAt(outUnitSub, 0));
                                Double pVal = valD(getAt(outUnitSub, 1));
                                if (pVal != null) op.setProbability(pVal);

                                ParameterDescriptor jobsPerLinkDis = getAt(entryParams, 1);
                                Map<Object, Object> opMap = new HashMap<>();
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
                                    if (targetClass != null && count != null) opMap.put(model.getClassByName(targetClass), count);
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

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        sec.addParameter(makeParam(XMLConstantNames.PARAM_JOBS_PER_LINK, Integer.class.getName(), String.valueOf(model.getStationNumberOfServers(key))));
        sec.addParameter(makeParam(XMLConstantNames.PARAM_BLOCK, Integer.class.getName(), String.valueOf(model.getForkBlock(key))));
        sec.addParameter(makeParam(XMLConstantNames.PARAM_IS_SIMPLIFIED_FORK, Boolean.class.getName(), String.valueOf(model.getIsSimplifiedFork(key))));

        ParameterDescriptor forkStrategy = makeContainer(XMLConstantNames.PARAM_FORK_STRATEGY, "jmt.engine.random.ForkStrategy");
        forkStrategy.setArray(true); // FIX

        for (Object classKey : model.getClassKeys()) {
            ForkStrategy fs = (ForkStrategy) model.getForkStrategy(key, classKey);
            if (fs != null) {
                ParameterDescriptor p = makeContainer(fs.getName(), fs.getClassPath());
                p.setRefClass(model.getClassName(classKey));

                if (fs instanceof ProbabilitiesFork) {
                    Map<Object, OutPath> outPaths = (Map<Object, OutPath>) fs.getOutDetails();
                    if (outPaths != null) {
                        ParameterDescriptor arrayPar = makeContainer("EmpiricalEntryArray", "jmt.engine.NetStrategies.ForkStrategies.OutPath");
                        arrayPar.setArray(true);
                        for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
                            ParameterDescriptor entryPar = makeContainer("OutPathEntry", "jmt.engine.NetStrategies.ForkStrategies.OutPath");

                            ParameterDescriptor outUnitProb = makeContainer("outUnitProbability", EMPIRICAL_ENTRY_CLASS);
                            outUnitProb.addSubParameter(makeParam("stationName", String.class.getName(), model.getStationName(entry.getKey())));
                            outUnitProb.addSubParameter(makeParam("probability", Double.class.getName(), entry.getValue().getProbability().toString()));
                            entryPar.addSubParameter(outUnitProb);

                            ParameterDescriptor jobsPerLinkDis = makeContainer("JobsPerLinkDis", EMPIRICAL_ENTRY_CLASS);
                            jobsPerLinkDis.setArray(true); // FIX
                            for (Map.Entry<Object, Object> subEntry : entry.getValue().getOutParameters().entrySet()) {
                                ParameterDescriptor numEntry = makeContainer("EmpiricalEntry", EMPIRICAL_ENTRY_CLASS);
                                numEntry.addSubParameter(makeParam("numbers", String.class.getName(), subEntry.getKey().toString()));
                                numEntry.addSubParameter(makeParam("probability", Double.class.getName(), subEntry.getValue().toString()));
                                jobsPerLinkDis.addSubParameter(numEntry);
                            }
                            entryPar.addSubParameter(jobsPerLinkDis);
                            arrayPar.addSubParameter(entryPar);
                        }
                        p.addSubParameter(arrayPar);
                    }
                } else if (fs instanceof CombFork) {
                    Map<Object, Double> probs = (Map<Object, Double>) fs.getOutDetails();
                    if (probs != null) {
                        ParameterDescriptor entryArray = makeContainer("EmpiricalEntry", EMPIRICAL_ENTRY_CLASS);
                        entryArray.setArray(true);
                        for (int i = 0; i < probs.size(); i++) {
                            String bKey = Integer.toString(i + 1);
                            Double val = probs.get(bKey);
                            if (val != null) {
                                ParameterDescriptor outUnitProb = makeContainer("outUnitProbability", EMPIRICAL_ENTRY_CLASS);
                                outUnitProb.addSubParameter(makeParam("Number of Branches", String.class.getName(), bKey));
                                outUnitProb.addSubParameter(makeParam("probability", Double.class.getName(), val.toString()));
                                entryArray.addSubParameter(outUnitProb);
                            }
                        }
                        p.addSubParameter(entryArray);
                    }
                } else if (fs instanceof ClassSwitchFork || fs instanceof MultiBranchClassSwitchFork) {
                    Map<Object, OutPath> outPaths = (Map<Object, OutPath>) fs.getOutDetails();
                    if (outPaths != null) {
                        ParameterDescriptor classJobNumArray = makeContainer("ClassJobNumArray", "jmt.engine.NetStrategies.ForkStrategies.ClassJobNum");
                        classJobNumArray.setArray(true);
                        for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
                            ParameterDescriptor entryPar = makeContainer("OutPathEntry", "jmt.engine.NetStrategies.ForkStrategies.ClassJobNum");
                            entryPar.addSubParameter(makeParam("stationName", String.class.getName(), model.getStationName(entry.getKey())));

                            ParameterDescriptor classesPar = makeContainer("Classes", String.class.getName());
                            classesPar.setArray(true);
                            ParameterDescriptor numbersPar = makeContainer("Numbers", String.class.getName());
                            numbersPar.setArray(true);

                            Map<Object, Integer> map = (Map) entry.getValue().getOutParameters();
                            for (Object cKey : model.getClassKeys()) {
                                Integer count = map.get(cKey);
                                if (count != null) {
                                    classesPar.addSubParameter(makeParam("class", String.class.getName(), model.getClassName(cKey)));
                                    numbersPar.addSubParameter(makeParam("numberOfJobs", String.class.getName(), count.toString()));
                                }
                            }
                            entryPar.addSubParameter(classesPar);
                            entryPar.addSubParameter(numbersPar);
                            classJobNumArray.addSubParameter(entryPar);
                        }
                        p.addSubParameter(classJobNumArray);
                    }
                }
                forkStrategy.addSubParameter(p);
            }
        }
        sec.addParameter(forkStrategy);
    }
}