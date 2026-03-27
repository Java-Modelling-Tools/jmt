/**
 * This class is a section mapper responsible for mapping the "Fork" section of a station node.
 * It configures the fork behavior, including the fork strategy and its parameters.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
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

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
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

        ParameterDescriptor outUnitProb = getAt(entryParams, 0);
        List<ParameterDescriptor> outUnitSub = getSubParams(outUnitProb);

        String sName = val(getAt(outUnitSub, 0));
        Double pVal = valD(getAt(outUnitSub, 1));

        if (pVal != null) op.setProbability(pVal);

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

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        ParameterDescriptor jobsPerLink = new ParameterDescriptor();
        jobsPerLink.setName(XMLConstantNames.PARAM_JOBS_PER_LINK);
        jobsPerLink.setClassPath(Integer.class.getName());
        jobsPerLink.setValue(String.valueOf(model.getStationNumberOfServers(key)));
        sec.addParameter(jobsPerLink);

        ParameterDescriptor block = new ParameterDescriptor();
        block.setName(XMLConstantNames.PARAM_BLOCK);
        block.setClassPath(Integer.class.getName());
        block.setValue(String.valueOf(model.getForkBlock(key)));
        sec.addParameter(block);

        ParameterDescriptor isSim = new ParameterDescriptor();
        isSim.setName(XMLConstantNames.PARAM_IS_SIMPLIFIED_FORK);
        isSim.setClassPath(Boolean.class.getName());
        isSim.setValue(String.valueOf(model.getIsSimplifiedFork(key)));
        sec.addParameter(isSim);

        ParameterDescriptor forkStrategy = new ParameterDescriptor();
        forkStrategy.setName(XMLConstantNames.PARAM_FORK_STRATEGY);
        forkStrategy.setClassPath("jmt.engine.random.ForkStrategy");

        for (Object classKey : model.getClassKeys()) {
            ForkStrategy fs = (ForkStrategy) model.getForkStrategy(key, classKey);
            if (fs != null) {
                ParameterDescriptor fsP = mapForkStrategyToDto(fs, key, classKey, model, context);
                forkStrategy.addSubParameter(fsP);
            }
        }
        sec.addParameter(forkStrategy);
    }

    private ParameterDescriptor mapForkStrategyToDto(ForkStrategy fs, Object key, Object classKey, CommonModel model, WritingContext context) {
        ParameterDescriptor p = new ParameterDescriptor();
        p.setName(fs.getName());
        p.setClassPath(fs.getClassPath());
        p.setRefClass(model.getClassName(classKey));

        if (fs instanceof ProbabilitiesFork) {
            mapProbabilitiesForkToDto(fs, p, key, model, context);
        } else if (fs instanceof CombFork) {
            mapCombForkToDto(fs, p, key, model, context);
        } else if (fs instanceof ClassSwitchFork || fs instanceof MultiBranchClassSwitchFork) {
            mapClassSwitchForkToDto(fs, p, key, model, context);
        }
        return p;
    }

    private void mapProbabilitiesForkToDto(ForkStrategy fs, ParameterDescriptor p, Object key, CommonModel model, WritingContext context) {
        Map<Object, OutPath> outPaths = (Map<Object, OutPath>) fs.getOutDetails();
        if (outPaths == null) return;

        ParameterDescriptor arrayPar = new ParameterDescriptor();
        arrayPar.setName("EmpiricalEntryArray");
        arrayPar.setClassPath("jmt.engine.NetStrategies.ForkStrategies.OutPath");
        arrayPar.setArray(true);

        for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
            ParameterDescriptor entryPar = new ParameterDescriptor();
            entryPar.setName("OutPathEntry");
            entryPar.setClassPath("jmt.engine.NetStrategies.ForkStrategies.OutPath");

            // outUnitProbability
            ParameterDescriptor outUnitProb = new ParameterDescriptor();
            outUnitProb.setName("outUnitProbability");
            outUnitProb.setClassPath("jmt.gui.common.xml.EmpiricalEntry");

            ParameterDescriptor stationName = new ParameterDescriptor();
            stationName.setName("stationName");
            stationName.setClassPath(String.class.getName());
            stationName.setValue(model.getStationName(entry.getKey()));
            outUnitProb.addSubParameter(stationName);

            ParameterDescriptor prob = new ParameterDescriptor();
            prob.setName("probability");
            prob.setClassPath(Double.class.getName());
            prob.setValue(entry.getValue().getProbability().toString());
            outUnitProb.addSubParameter(prob);

            entryPar.addSubParameter(outUnitProb);

            // JobsPerLinkDis
            ParameterDescriptor jobsPerLinkDis = new ParameterDescriptor();
            jobsPerLinkDis.setName("JobsPerLinkDis");
            jobsPerLinkDis.setClassPath("jmt.gui.common.xml.EmpiricalEntry");
            jobsPerLinkDis.setArray(true);

            Map<Object, Object> map = entry.getValue().getOutParameters();
            for (Map.Entry<Object, Object> subEntry : map.entrySet()) {
                ParameterDescriptor numEntry = new ParameterDescriptor();
                numEntry.setName("EmpiricalEntry");
                numEntry.setClassPath("jmt.gui.common.xml.EmpiricalEntry");

                ParameterDescriptor numbers = new ParameterDescriptor();
                numbers.setName("numbers");
                numbers.setClassPath(String.class.getName());
                numbers.setValue(subEntry.getKey().toString());
                numEntry.addSubParameter(numbers);

                ParameterDescriptor numProb = new ParameterDescriptor();
                numProb.setName("probability");
                numProb.setClassPath(Double.class.getName());
                numProb.setValue(subEntry.getValue().toString());
                numEntry.addSubParameter(numProb);

                jobsPerLinkDis.addSubParameter(numEntry);
            }
            entryPar.addSubParameter(jobsPerLinkDis);
            arrayPar.addSubParameter(entryPar);
        }
        p.addSubParameter(arrayPar);
    }

    private void mapCombForkToDto(ForkStrategy fs, ParameterDescriptor p, Object key, CommonModel model, WritingContext context) {
        Map<Object, Double> probs = (Map<Object, Double>) fs.getOutDetails();
        if (probs == null) return;

        ParameterDescriptor entryArray = new ParameterDescriptor();
        entryArray.setName("EmpiricalEntry");
        entryArray.setClassPath("jmt.gui.common.xml.EmpiricalEntry");
        entryArray.setArray(true);

        for (int i = 0; i < probs.size(); i++) {
            String bKey = Integer.toString(i + 1);
            Double val = probs.get(bKey);
            if (val == null) continue;

            ParameterDescriptor outUnitProb = new ParameterDescriptor();
            outUnitProb.setName("outUnitProbability");
            outUnitProb.setClassPath("jmt.gui.common.xml.EmpiricalEntry");

            ParameterDescriptor branchNum = new ParameterDescriptor();
            branchNum.setName("Number of Branches");
            branchNum.setClassPath(String.class.getName());
            branchNum.setValue(bKey);
            outUnitProb.addSubParameter(branchNum);

            ParameterDescriptor prob = new ParameterDescriptor();
            prob.setName("probability");
            prob.setClassPath(Double.class.getName());
            prob.setValue(val.toString());
            outUnitProb.addSubParameter(prob);

            entryArray.addSubParameter(outUnitProb);
        }
        p.addSubParameter(entryArray);
    }

    private void mapClassSwitchForkToDto(ForkStrategy fs, ParameterDescriptor p, Object key, CommonModel model, WritingContext context) {
        Map<Object, OutPath> outPaths = (Map<Object, OutPath>) fs.getOutDetails();
        if (outPaths == null) return;

        ParameterDescriptor classJobNumArray = new ParameterDescriptor();
        classJobNumArray.setName("ClassJobNumArray");
        classJobNumArray.setClassPath("jmt.engine.NetStrategies.ForkStrategies.ClassJobNum");
        classJobNumArray.setArray(true);

        for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
            ParameterDescriptor entryPar = new ParameterDescriptor();
            entryPar.setName("OutPathEntry");
            entryPar.setClassPath("jmt.engine.NetStrategies.ForkStrategies.ClassJobNum");

            ParameterDescriptor stationName = new ParameterDescriptor();
            stationName.setName("stationName");
            stationName.setClassPath(String.class.getName());
            stationName.setValue(model.getStationName(entry.getKey()));
            entryPar.addSubParameter(stationName);

            ParameterDescriptor classesPar = new ParameterDescriptor();
            classesPar.setName("Classes");
            classesPar.setClassPath(String.class.getName());
            classesPar.setArray(true);

            ParameterDescriptor numbersPar = new ParameterDescriptor();
            numbersPar.setName("Numbers");
            numbersPar.setClassPath(String.class.getName());
            numbersPar.setArray(true);

            Map<Object, Object> map = entry.getValue().getOutParameters();
            for (Object cKey : model.getClassKeys()) {
                Integer count = (Integer) map.get(cKey);
                if (count != null) {
                    ParameterDescriptor jobClass = new ParameterDescriptor();
                    jobClass.setName("class");
                    jobClass.setClassPath(String.class.getName());
                    jobClass.setValue(model.getClassName(cKey));
                    classesPar.addSubParameter(jobClass);

                    ParameterDescriptor jobNum = new ParameterDescriptor();
                    jobNum.setName("numberOfJobs");
                    jobNum.setClassPath(String.class.getName());
                    jobNum.setValue(count.toString());
                    numbersPar.addSubParameter(jobNum);
                }
            }
            entryPar.addSubParameter(classesPar);
            entryPar.addSubParameter(numbersPar);
            classJobNumArray.addSubParameter(entryPar);
        }
        p.addSubParameter(classJobNumArray);
    }
}