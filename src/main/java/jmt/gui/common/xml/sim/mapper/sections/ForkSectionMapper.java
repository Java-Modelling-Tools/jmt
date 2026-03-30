package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.forkStrategies.*;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ForkSectionMapper extends AbstractSectionMapper {

    // =========================================================================================
    // --- TEMPORARY CONSTANTS (To be moved to XMLConstantNames) ---
    // =========================================================================================
    public static final String XML_E_EMPIRICAL_ENTRY_ARRAY = "EmpiricalEntryArray";
    public static final String XML_E_OUTPATH_ENTRY = "OutPathEntry";
    public static final String XML_E_OUT_UNIT_PROBABILITY = "outUnitProbability";
    public static final String XML_E_STATION_NAME = "stationName";
    public static final String XML_A_PROBABILITY = "probability";
    public static final String XML_E_JOBS_PER_LINK_DIS = "JobsPerLinkDis";
    public static final String XML_E_NUMBER_OF_BRANCHES = "Number of Branches";
    public static final String XML_E_CLASS_JOB_NUM_ARRAY = "ClassJobNumArray";
    public static final String XML_A_NUMBER_OF_JOBS = "numberOfJobs";
    // =========================================================================================

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        if (sec.getParameters() == null) return;

        mapSimpleParams(sec, model, key);
        mapForkStrategies(sec, model, key, ctx);
    }

    private void mapSimpleParams(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor jpl = getParam(sec, XMLConstantNames.PARAM_JOBS_PER_LINK);
        if (jpl != null) {
            Integer v = valI(jpl);
            if (v != null) model.setStationNumberOfServers(key, v);
        }

        ParameterDescriptor block = getParam(sec, XMLConstantNames.PARAM_BLOCK);
        if (block != null) {
            Integer v = valI(block);
            if (v != null) model.setForkBlock(key, v);
        }

        ParameterDescriptor isSimp = getParam(sec, XMLConstantNames.PARAM_IS_SIMPLIFIED_FORK);
        if (isSimp != null) {
            String bVal = val(isSimp);
            if (bVal != null) model.setIsSimplifiedFork(key, Boolean.parseBoolean(bVal));
        }
    }

    private void mapForkStrategies(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
        ParameterDescriptor forkStrat = getParam(sec, XMLConstantNames.PARAM_FORK_STRATEGY);
        if (forkStrat == null) return;

        getRefClassMap(forkStrat).forEach((className, fP) -> {
            Object classKey = ctx.classes.get(className);
            String cp = fP.getClassPath();

            // Set the concrete strategy from GUI template pool
            for (ForkStrategy strat : ctx.getEngineToGuiFork().values()) {
                if (cp != null && cp.equals(strat.getClassPath())) {
                    model.setForkStrategy(key, classKey, strat.clone());
                }
            }

            ForkStrategy fs = (ForkStrategy) model.getForkStrategy(key, classKey);
            if (fs instanceof ProbabilitiesFork) {
                parseProbabilitiesFork(fP, key, classKey, ctx);
            } else if (fs instanceof CombFork) {
                parseCombFork(fP, key, classKey, ctx);
            } else if (fs instanceof ClassSwitchFork || fs instanceof MultiBranchClassSwitchFork) {
                parseClassSwitchFork(fP, key, classKey, ctx, model);
            }
        });
    }

    private void parseProbabilitiesFork(ParameterDescriptor fP, Object key, Object classKey, MappingContext ctx) {
        Map<String, OutPath> map = new HashMap<>();
        ParameterDescriptor entryArray = getFirst(getSubParams(fP));
        if (entryArray == null) return;

        for (ParameterDescriptor entry : getSubParams(entryArray)) {
            parseOutPathEntry(entry, map);
        }
        ctx.empiricalFork.put(new Object[]{key, classKey}, map);
    }

    private void parseOutPathEntry(ParameterDescriptor entry, Map<String, OutPath> map) {
        OutPath op = new OutPath();
        List<ParameterDescriptor> entryParams = getSubParams(entry);

        ParameterDescriptor outUnitProb = getAt(entryParams, 0);
        List<ParameterDescriptor> outUnitSub = getSubParams(outUnitProb);
        String sName = val(getAt(outUnitSub, 0));
        Double pVal = valD(getAt(outUnitSub, 1));
        if (pVal != null) op.setProbability(pVal);

        ParameterDescriptor jobsPerLinkDis = getAt(entryParams, 1);
        op.setOutParameters(parseJobsPerLinkDistribution(jobsPerLinkDis));

        if (sName != null) {
            map.put(sName, op);
        }
    }

    private Map<Object, Object> parseJobsPerLinkDistribution(ParameterDescriptor jobsPerLinkDis) {
        Map<Object, Object> opMap = new HashMap<>();
        if (jobsPerLinkDis == null) return opMap;

        for (ParameterDescriptor res : getSubParams(jobsPerLinkDis)) {
            List<ParameterDescriptor> resParams = getSubParams(res);
            Integer rKey = valI(getAt(resParams, 0));
            Double rVal = valD(getAt(resParams, 1));
            if (rKey != null && rVal != null) {
                opMap.put(rKey, rVal);
            }
        }
        return opMap;
    }

    private void parseCombFork(ParameterDescriptor fP, Object key, Object classKey, MappingContext ctx) {
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

    private void parseClassSwitchFork(ParameterDescriptor fP, Object key, Object classKey, MappingContext ctx, CommonModel model) {
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

    // =========================================================================================
    // --- WRITING PHASE (Model -> DTO -> XML) ---
    // =========================================================================================

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        sec.addParameter(makeParam(XMLConstantNames.PARAM_JOBS_PER_LINK, XMLConstantNames.XML_INTEGER_CLASS, String.valueOf(model.getStationNumberOfServers(key))));
        sec.addParameter(makeParam(XMLConstantNames.PARAM_BLOCK, XMLConstantNames.XML_INTEGER_CLASS, String.valueOf(model.getForkBlock(key))));
        sec.addParameter(makeParam(XMLConstantNames.PARAM_IS_SIMPLIFIED_FORK, XMLConstantNames.XML_BOOLEAN_CLASS, String.valueOf(model.getIsSimplifiedFork(key))));

        ParameterDescriptor forkStrategy = makeContainer(XMLConstantNames.PARAM_FORK_STRATEGY, XMLConstantNames.CP_FORK_STRATEGY);
        forkStrategy.setArray(true);

        for (Object classKey : model.getClassKeys()) {
            ForkStrategy fs = (ForkStrategy) model.getForkStrategy(key, classKey);
            if (fs != null) {
                ParameterDescriptor p = makeContainer(fs.getName(), fs.getClassPath());
                p.setRefClass(model.getClassName(classKey));

                if (fs instanceof ProbabilitiesFork) {
                    writeProbabilitiesFork(p, (ProbabilitiesFork) fs, model);
                } else if (fs instanceof CombFork) {
                    writeCombFork(p, (CombFork) fs);
                } else if (fs instanceof ClassSwitchFork || fs instanceof MultiBranchClassSwitchFork) {
                    writeClassSwitchFork(p, fs, model);
                }
                forkStrategy.addSubParameter(p);
            }
        }
        sec.addParameter(forkStrategy);
    }

    private void writeProbabilitiesFork(ParameterDescriptor p, ProbabilitiesFork fs, CommonModel model) {
        Map<Object, OutPath> outPaths = fs.getOutDetails();
        if (outPaths == null) return;

        ParameterDescriptor arrayPar = makeContainer(XML_E_EMPIRICAL_ENTRY_ARRAY, XMLConstantNames.CP_OUTPATH);
        arrayPar.setArray(true);
        for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
            ParameterDescriptor entryPar = makeContainer(XML_E_OUTPATH_ENTRY, XMLConstantNames.CP_OUTPATH);

            ParameterDescriptor outUnitProb = makeContainer(XML_E_OUT_UNIT_PROBABILITY, XMLConstantNames.EMPIRICAL_ENTRY_CLASS);
            outUnitProb.addSubParameter(makeParam(XML_E_STATION_NAME, XMLConstantNames.XML_STRING_CLASS, model.getStationName(entry.getKey())));
            outUnitProb.addSubParameter(makeParam(XML_A_PROBABILITY, XMLConstantNames.XML_DOUBLE_CLASS, entry.getValue().getProbability().toString()));
            entryPar.addSubParameter(outUnitProb);

            ParameterDescriptor jobsPerLinkDis = makeContainer(XML_E_JOBS_PER_LINK_DIS, XMLConstantNames.EMPIRICAL_ENTRY_CLASS);
            jobsPerLinkDis.setArray(true);
            for (Map.Entry<Object, Object> subEntry : entry.getValue().getOutParameters().entrySet()) {
                ParameterDescriptor numEntry = makeContainer(XMLConstantNames.PARAM_EMPIRICAL_ENTRY, XMLConstantNames.EMPIRICAL_ENTRY_CLASS);
                numEntry.addSubParameter(makeParam(XMLConstantNames.PARAM_NUMBERS, XMLConstantNames.XML_STRING_CLASS, subEntry.getKey().toString()));
                numEntry.addSubParameter(makeParam(XML_A_PROBABILITY, XMLConstantNames.XML_DOUBLE_CLASS, subEntry.getValue().toString()));
                jobsPerLinkDis.addSubParameter(numEntry);
            }
            entryPar.addSubParameter(jobsPerLinkDis);
            arrayPar.addSubParameter(entryPar);
        }
        p.addSubParameter(arrayPar);
    }

    private void writeCombFork(ParameterDescriptor p, CombFork fs) {
        Map<Object, Double> probs = fs.getOutDetails();
        if (probs == null) return;

        ParameterDescriptor entryArray = makeContainer(XMLConstantNames.PARAM_EMPIRICAL_ENTRY, XMLConstantNames.EMPIRICAL_ENTRY_CLASS);
        entryArray.setArray(true);
        for (int i = 0; i < probs.size(); i++) {
            String bKey = Integer.toString(i + 1);
            Double val = probs.get(bKey);
            if (val != null) {
                ParameterDescriptor outUnitProb = makeContainer(XML_E_OUT_UNIT_PROBABILITY, XMLConstantNames.EMPIRICAL_ENTRY_CLASS);
                outUnitProb.addSubParameter(makeParam(XML_E_NUMBER_OF_BRANCHES, XMLConstantNames.XML_STRING_CLASS, bKey));
                outUnitProb.addSubParameter(makeParam(XML_A_PROBABILITY, XMLConstantNames.XML_DOUBLE_CLASS, val.toString()));
                entryArray.addSubParameter(outUnitProb);
            }
        }
        p.addSubParameter(entryArray);
    }

    private void writeClassSwitchFork(ParameterDescriptor p, ForkStrategy fs, CommonModel model) {
        Map<Object, OutPath> outPaths = (Map<Object, OutPath>) fs.getOutDetails();
        if (outPaths == null) return;

        ParameterDescriptor classJobNumArray = makeContainer(XML_E_CLASS_JOB_NUM_ARRAY, XMLConstantNames.CP_CLASS_JOB_NUM);
        classJobNumArray.setArray(true);
        for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
            ParameterDescriptor entryPar = makeContainer(XML_E_OUTPATH_ENTRY, XMLConstantNames.CP_CLASS_JOB_NUM);
            entryPar.addSubParameter(makeParam(XML_E_STATION_NAME, XMLConstantNames.XML_STRING_CLASS, model.getStationName(entry.getKey())));

            ParameterDescriptor classesPar = makeContainer(XMLConstantNames.PARAM_CLASSES, XMLConstantNames.XML_STRING_CLASS);
            classesPar.setArray(true);
            ParameterDescriptor numbersPar = makeContainer(XMLConstantNames.PARAM_NUMBERS, XMLConstantNames.XML_STRING_CLASS);
            numbersPar.setArray(true);

            Map<Object, Integer> map = (Map) entry.getValue().getOutParameters();
            for (Object cKey : model.getClassKeys()) {
                Integer count = map.get(cKey);
                if (count != null) {
                    classesPar.addSubParameter(makeParam(XMLConstantNames.XML_CLASS, XMLConstantNames.XML_STRING_CLASS, model.getClassName(cKey)));
                    numbersPar.addSubParameter(makeParam(XML_A_NUMBER_OF_JOBS, XMLConstantNames.XML_STRING_CLASS, count.toString()));
                }
            }
            entryPar.addSubParameter(classesPar);
            entryPar.addSubParameter(numbersPar);
            classJobNumArray.addSubParameter(entryPar);
        }
        p.addSubParameter(classJobNumArray);
    }
}