package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.routingStrategies.*;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RouterSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor routingParam = getFirst(sec.getParameters());
        if (routingParam == null) return;

        getRefClassMap(routingParam).forEach((className, p) -> {
            String cp = p.getClassPath();
            Object classKey = ctx.classes.get(className);

            // Mappatura sicura indipendente dal classpath di default
            if (cp != null && (cp.contains("Empirical") || cp.contains("Probabilities"))) model.setRoutingStrategy(key, classKey, new ProbabilityRouting());
            else if (cp != null && cp.contains("LoadDependent")) model.setRoutingStrategy(key, classKey, new LoadDependentRouting());
            else if (cp != null && cp.contains("PowerOfK")) model.setRoutingStrategy(key, classKey, new PowerOfKRouting());
            else if (cp != null && cp.contains("WeightedRoundRobin")) model.setRoutingStrategy(key, classKey, new WeightedRoundRobinRouting());
            else if (cp != null && cp.contains("ClassSwitch")) model.setRoutingStrategy(key, classKey, new ClassSwitchRouting());
            else model.setRoutingStrategy(key, classKey, new RandomRouting());

            RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(key, classKey);

            if (rs instanceof ProbabilityRouting) {
                Map<String, Double> tmp = new TreeMap<>();
                ParameterDescriptor entryArray = getFirst(getSubParams(p));
                if (entryArray != null) {
                    for (ParameterDescriptor entry : getSubParams(entryArray)) {
                        List<ParameterDescriptor> subEntries = getSubParams(entry);
                        String sName = val(getAt(subEntries, 0));
                        Double dVal = valD(getAt(subEntries, 1));
                        if (sName != null && dVal != null) tmp.put(sName, dVal);
                    }
                }
                ctx.empiricalRouting.put(new Object[]{key, classKey}, tmp);

            } else if (rs instanceof LoadDependentRouting) {
                ParameterDescriptor entryArray = getFirst(getSubParams(p));
                if (entryArray != null) {
                    for (ParameterDescriptor fromEntry : getSubParams(entryArray)) {
                        List<ParameterDescriptor> subFrom = getSubParams(fromEntry);
                        Integer fromValue = valI(getAt(subFrom, 0));
                        if (fromValue == null) continue;

                        Map<String, Double> tmp = new TreeMap<>();
                        ParameterDescriptor destArray = getAt(subFrom, 1);
                        if (destArray != null) {
                            for (ParameterDescriptor e : getSubParams(destArray)) {
                                List<ParameterDescriptor> eSub = getSubParams(e);
                                String sName = val(getAt(eSub, 0));
                                Double dVal = valD(getAt(eSub, 1));
                                if (sName != null && dVal != null) tmp.put(sName, dVal);
                            }
                        }
                        ctx.empiricalLDRouting.put(new Object[]{key, classKey, fromValue}, tmp);
                    }
                }
            } else if (rs instanceof PowerOfKRouting) {
                List<ParameterDescriptor> sps = getSubParams(p);
                Integer kVal = valI(getAt(sps, 0));
                String bVal = val(getAt(sps, 1));
                if (kVal != null) ((PowerOfKRouting) rs).setK(kVal);
                if (bVal != null) ((PowerOfKRouting) rs).setWithMemory(Boolean.valueOf(bVal));

            } else if (rs instanceof WeightedRoundRobinRouting) {
                Map<String, Integer> tmp = new TreeMap<>();
                ParameterDescriptor entryArray = getFirst(getSubParams(p));
                if (entryArray != null) {
                    for (ParameterDescriptor entry : getSubParams(entryArray)) {
                        List<ParameterDescriptor> subEntries = getSubParams(entry);
                        String sName = val(getAt(subEntries, 0));
                        Integer iVal = valI(getAt(subEntries, 1));
                        if (sName != null && iVal != null) tmp.put(sName, iVal);
                    }
                }
                ctx.wrrRouting.put(new Object[]{key, classKey}, tmp);

            } else if (rs instanceof ClassSwitchRouting) {
                Map<String, Double> branchTmp = new TreeMap<>();
                Map<String, Map<String, Double>> classTmp = new TreeMap<>();
                ParameterDescriptor paramArray = getFirst(getSubParams(p));

                if (paramArray != null) {
                    for (ParameterDescriptor csp : getSubParams(paramArray)) {
                        List<ParameterDescriptor> subCsp = getSubParams(csp);
                        String sName = val(getAt(subCsp, 0));
                        Double dVal = valD(getAt(subCsp, 1));
                        if (sName != null && dVal != null) branchTmp.put(sName, dVal);

                        Map<String, Double> innerMap = new TreeMap<>();
                        ParameterDescriptor entryArray = getAt(subCsp, 2);
                        if (entryArray != null) {
                            for (ParameterDescriptor e : getSubParams(entryArray)) {
                                List<ParameterDescriptor> eSub = getSubParams(e);
                                String iName = val(getAt(eSub, 0));
                                Double iDVal = valD(getAt(eSub, 1));
                                if (iName != null && iDVal != null) innerMap.put(iName, iDVal);
                            }
                        }
                        if (sName != null) classTmp.put(sName, innerMap);
                    }
                }
                ctx.csBranchRouting.put(new Object[]{key, classKey}, branchTmp);
                ctx.csClassRouting.put(new Object[]{key, classKey}, classTmp);
            }
        });
    }
}