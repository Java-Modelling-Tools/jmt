/**
 * This class is responsible for mapping {@link BlockingRegionDescriptor} objects from the DTO to the {@link CommonModel}.
 * It iterates through the blocking regions defined in the {@link SimDescriptor} and configures their properties and constraints.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.BlockingRegionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockingRegionsMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        if (dto.getBlockingRegions() == null || dto.getBlockingRegions().isEmpty()) {
            return;
        }

        for (BlockingRegionDescriptor region : dto.getBlockingRegions()) {
            mapBlockingRegion(region, context);
        }
    }

    private void mapBlockingRegion(BlockingRegionDescriptor region, MappingContext context) {
        CommonModel model = context.getModel();
        Object regionKey = model.addBlockingRegion(region.getName(), region.getType());
        context.regions.put(region.getName(), regionKey);

        applyGlobalConstraints(model, regionKey, region.getGlobalConstraints());
        applyRegionNodes(model, regionKey, region, context);
        applyClassConstraints(model, regionKey, region, context);
        applyClassWeights(model, regionKey, region, context);
        applyClassSizes(model, regionKey, region, context);
        applyDropRules(model, regionKey, region, context);
        applyGroups(model, regionKey, region, context);
    }

    private void applyGlobalConstraints(CommonModel model, Object regionKey, Map<String, String> glob) {
        if (glob == null) return;
        if (glob.get(XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS) != null) {
            model.setRegionCustomerConstraint(regionKey, Integer.parseInt(glob.get(XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS)));
        }
        if (glob.get(XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY) != null) {
            model.setRegionMemorySize(regionKey, Integer.parseInt(glob.get(XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY)));
        }
    }

    private void applyRegionNodes(CommonModel model, Object regionKey, BlockingRegionDescriptor region, MappingContext context) {
        if (region.getRegionNodes() == null) return;
        for (String nodeName : region.getRegionNodes()) {
            Object stationKey = context.stations.get(nodeName);
            if (stationKey != null) {
                model.addRegionStation(regionKey, stationKey);
            }
        }
    }

    private void applyClassConstraints(CommonModel model, Object regionKey, BlockingRegionDescriptor region, MappingContext context) {
        if (region.getClassConstraints() == null) return;
        region.getClassConstraints().forEach((className, constraintsMap) -> {
            Object classKey = context.classes.get(className);
            if (classKey == null) return;
            if (constraintsMap.get(XMLConstantNames.XML_A_CLASSCONSTRAINT_MAXJOBS) != null) {
                model.setRegionClassCustomerConstraint(regionKey, classKey, Integer.parseInt(constraintsMap.get(XMLConstantNames.XML_A_CLASSCONSTRAINT_MAXJOBS)));
            }
            if (constraintsMap.get(XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY) != null) {
                model.setRegionClassMemorySize(regionKey, classKey, Integer.parseInt(constraintsMap.get(XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY)));
            }
            if (constraintsMap.get(XMLConstantNames.XML_A_CLASS_SOFT_DEADLINE) != null) {
                model.setRegionClassSoftDeadline(regionKey, classKey, Double.parseDouble(constraintsMap.get(XMLConstantNames.XML_A_CLASS_SOFT_DEADLINE)));
            }
        });
    }

    private void applyClassWeights(CommonModel model, Object regionKey, BlockingRegionDescriptor region, MappingContext context) {
        if (region.getClassWeights() == null) return;
        region.getClassWeights().forEach((className, weight) -> {
            Object classKey = context.classes.get(className);
            if (classKey != null && weight != null) {
                model.setRegionClassWeight(regionKey, classKey, Integer.parseInt(weight));
            }
        });
    }

    private void applyClassSizes(CommonModel model, Object regionKey, BlockingRegionDescriptor region, MappingContext context) {
        if (region.getClassSizes() == null) return;
        region.getClassSizes().forEach((className, size) -> {
            Object classKey = context.classes.get(className);
            if (classKey != null && size != null) {
                model.setRegionClassSize(regionKey, classKey, Integer.parseInt(size));
            }
        });
    }

    private void applyDropRules(CommonModel model, Object regionKey, BlockingRegionDescriptor region, MappingContext context) {
        if (region.getDropRules() == null) return;
        region.getDropRules().forEach((className, dropRule) -> {
            Object classKey = context.classes.get(className);
            if (classKey != null && dropRule != null) {
                model.setRegionClassDropRule(regionKey, classKey, Boolean.parseBoolean(dropRule));
            }
        });
    }

    private void applyGroups(CommonModel model, Object regionKey, BlockingRegionDescriptor region, MappingContext context) {
        Set<String> groupNames = collectGroupNames(region);
        int groupIndex = 0;
        for (String groupName : groupNames) {
            model.addRegionGroup(regionKey, groupName);
            applyGroupConstraints(model, regionKey, groupIndex, groupName, region);
            applyGroupClasses(model, regionKey, groupIndex, groupName, region, context);
            groupIndex++;
        }
    }

    private Set<String> collectGroupNames(BlockingRegionDescriptor region) {
        Set<String> groupNames = new LinkedHashSet<>();
        if (region.getGroupConstraints() != null) groupNames.addAll(region.getGroupConstraints().keySet());
        if (region.getGroupMemoryConstraints() != null) groupNames.addAll(region.getGroupMemoryConstraints().keySet());
        if (region.getGroupClassLists() != null) groupNames.addAll(region.getGroupClassLists().keySet());
        return groupNames;
    }

    private void applyGroupConstraints(CommonModel model, Object regionKey, int groupIndex, String groupName, BlockingRegionDescriptor region) {
        String maxJobs = region.getGroupConstraints() != null ? region.getGroupConstraints().get(groupName) : null;
        if (maxJobs != null) {
            model.setRegionGroupCustomerConstraint(regionKey, groupIndex, Integer.parseInt(maxJobs));
        }
        String maxMemory = region.getGroupMemoryConstraints() != null ? region.getGroupMemoryConstraints().get(groupName) : null;
        if (maxMemory != null) {
            model.setRegionGroupMemorySize(regionKey, groupIndex, Integer.parseInt(maxMemory));
        }
    }

    private void applyGroupClasses(CommonModel model, Object regionKey, int groupIndex, String groupName, BlockingRegionDescriptor region, MappingContext context) {
        List<String> classes = region.getGroupClassLists() != null ? region.getGroupClassLists().get(groupName) : null;
        if (classes == null) return;
        for (String className : classes) {
            Object classKey = context.classes.get(className);
            if (classKey != null) {
                model.addClassIntoRegionGroup(regionKey, groupIndex, classKey);
            }
        }
    }
}