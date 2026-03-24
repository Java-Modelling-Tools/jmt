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
        if (routingParam == null) {
            return;
        }

        getRefClassMap(routingParam).forEach((className, p) -> {
            String cp = p.getClassPath();
            Object classKey = ctx.classes.get(className);

            if (ctx.engineToGuiRouting == null) {
                initGuiRoutingCache(ctx);
            }

            ctx.engineToGuiRouting.values().stream()
                    .filter(rs -> cp.equals(rs.getClassPath()) ||
                            (ENGINE_ROUTING_CLASS_SWITCH.equals(cp) && rs.getClassPath().equals(ENGINE_ROUTING_CLASS_SWITCH_GUI)))
                    .findFirst()
                    .ifPresent(rs -> model.setRoutingStrategy(key, classKey, rs.clone()));

            RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(key, classKey);

            if (rs instanceof ProbabilityRouting) {
                Map<String, Double> tmp = new TreeMap<>();
                List<ParameterDescriptor> probabilities = getSubParams(getFirst(getSubParams(p)));

                for (ParameterDescriptor entry : probabilities) {
                    List<ParameterDescriptor> subEntries = getSubParams(entry);
                    String sName = val(getFirst(subEntries));
                    Double dVal = valD(getAt(subEntries, 1));
                    if (sName != null && dVal != null) tmp.put(sName, dVal);
                }
                ctx.empiricalRouting.put(new Object[]{key, classKey}, tmp);

            } else if (rs instanceof LoadDependentRouting) {
                List<ParameterDescriptor> ldEntries = getSubParams(getFirst(getSubParams(p)));

                for (ParameterDescriptor fromEntry : ldEntries) {
                    List<ParameterDescriptor> subFrom = getSubParams(fromEntry);
                    Integer fromValue = valI(getFirst(subFrom));
                    if (fromValue == null) continue;

                    Map<String, Double> tmp = new TreeMap<>();
                    List<ParameterDescriptor> destinations = getSubParams(getAt(subFrom, 1));
                    for (ParameterDescriptor e : destinations) {
                        if (PARAM_EMPIRICAL_ENTRY.equalsIgnoreCase(e.getName())) {
                            List<ParameterDescriptor> eSub = getSubParams(e);
                            String sName = val(getFirst(eSub));
                            Double dVal = valD(getAt(eSub, 1));
                            if (sName != null && dVal != null) tmp.put(sName, dVal);
                        }
                    }
                    ctx.empiricalLDRouting.put(new Object[]{key, classKey, fromValue}, tmp);
                }

            } else if (rs instanceof PowerOfKRouting) {
                List<ParameterDescriptor> sps = getSubParams(p);
                Integer kVal = valI(getFirst(sps));
                String bVal = val(getAt(sps, 1));
                if (kVal != null) ((PowerOfKRouting) rs).setK(kVal);
                if (bVal != null) ((PowerOfKRouting) rs).setWithMemory(Boolean.valueOf(bVal));

            } else if (rs instanceof WeightedRoundRobinRouting) {
                Map<String, Integer> tmp = new TreeMap<>();
                List<ParameterDescriptor> wrrEntries = getSubParams(getFirst(getSubParams(p)));

                for (ParameterDescriptor entry : wrrEntries) {
                    List<ParameterDescriptor> subEntries = getSubParams(entry);
                    String sName = val(getFirst(subEntries));
                    Integer iVal = valI(getAt(subEntries, 1));
                    if (sName != null && iVal != null) tmp.put(sName, iVal);
                }
                ctx.wrrRouting.put(new Object[]{key, classKey}, tmp);

            } else if (rs instanceof ClassSwitchRouting) {
                Map<String, Double> branchTmp = new TreeMap<>();
                Map<String, Map<String, Double>> classTmp = new TreeMap<>();
                List<ParameterDescriptor> csEntries = getSubParams(getFirst(getSubParams(p)));

                for (ParameterDescriptor csp : csEntries) {
                    List<ParameterDescriptor> subCsp = getSubParams(csp);
                    String sName = val(getFirst(subCsp));
                    Double dVal = valD(getAt(subCsp, 1));
                    if (sName != null && dVal != null) branchTmp.put(sName, dVal);

                    Map<String, Double> innerMap = new TreeMap<>();
                    List<ParameterDescriptor> innerEntries = getSubParams(getAt(subCsp, 2));
                    for (ParameterDescriptor e : innerEntries) {
                        List<ParameterDescriptor> eSub = getSubParams(e);
                        String iName = val(getFirst(eSub));
                        Double iDVal = valD(getAt(eSub, 1));
                        if (iName != null && iDVal != null) innerMap.put(iName, iDVal);
                    }
                    if (sName != null) classTmp.put(sName, innerMap);
                }
                ctx.csBranchRouting.put(new Object[]{key, classKey}, branchTmp);
                ctx.csClassRouting.put(new Object[]{key, classKey}, classTmp);
            }
        });
    }
}