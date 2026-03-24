package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
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

        CommonModel model = context.getModel();

        for (BlockingRegionDescriptor region : dto.getBlockingRegions()) {

            // 1. Region creation
            Object regionKey = model.addBlockingRegion(region.getName(), region.getType());
            context.regions.put(region.getName(), regionKey);

            // 2. Global parameters of the region
            Map<String, String> glob = region.getGlobalConstraints();
            if (glob != null) {
                if (glob.get("maxJobs") != null) {
                    model.setRegionCustomerConstraint(regionKey, Integer.parseInt(glob.get("maxJobs")));
                }
                if (glob.get("maxMemory") != null) {
                    model.setRegionMemorySize(regionKey, Integer.parseInt(glob.get("maxMemory")));
                }
            }

            // 3. Nodes (Stations) belonging to the region
            if (region.getRegionNodes() != null) {
                for (String nodeName : region.getRegionNodes()) {
                    Object stationKey = context.stations.get(nodeName);
                    if (stationKey != null) {
                        model.addRegionStation(regionKey, stationKey);
                    }
                }
            }

            // 4. Multiple limits per Class (maxJobs, maxMemory, softDeadline)
            if (region.getClassConstraints() != null) {
                region.getClassConstraints().forEach((className, constraintsMap) -> {
                    Object classKey = context.classes.get(className);
                    if (classKey != null) {
                        // FIX: Using the exact keys injected by SimParser
                        if (constraintsMap.get("maxJobsPerClass") != null) {
                            model.setRegionClassCustomerConstraint(regionKey, classKey, Integer.parseInt(constraintsMap.get("maxJobsPerClass")));
                        }
                        if (constraintsMap.get("maxMemoryPerClass") != null) {
                            model.setRegionClassMemorySize(regionKey, classKey, Integer.parseInt(constraintsMap.get("maxMemoryPerClass")));
                        }
                        if (constraintsMap.get("softDeadline") != null) {
                            model.setRegionClassSoftDeadline(regionKey, classKey, Double.parseDouble(constraintsMap.get("softDeadline")));
                        }
                    }
                });
            }

            // 5. Weights per Class
            if (region.getClassWeights() != null) {
                region.getClassWeights().forEach((className, weight) -> {
                    Object classKey = context.classes.get(className);
                    if (classKey != null && weight != null) {
                        model.setRegionClassWeight(regionKey, classKey, Integer.parseInt(weight));
                    }
                });
            }

            // 6. Sizes per Class
            if (region.getClassSizes() != null) {
                region.getClassSizes().forEach((className, size) -> {
                    Object classKey = context.classes.get(className);
                    if (classKey != null && size != null) {
                        model.setRegionClassSize(regionKey, classKey, Integer.parseInt(size));
                    }
                });
            }

            // 7. Drop Rules per Class
            if (region.getDropRules() != null) {
                region.getDropRules().forEach((className, dropRule) -> {
                    Object classKey = context.classes.get(className);
                    if (classKey != null && dropRule != null) {
                        model.setRegionClassDropRule(regionKey, classKey, Boolean.parseBoolean(dropRule));
                    }
                });
            }

            // 8. Blocking groups and related classes
            Set<String> groupNames = new LinkedHashSet<>();
            if (region.getGroupConstraints() != null) groupNames.addAll(region.getGroupConstraints().keySet());
            if (region.getGroupMemoryConstraints() != null) groupNames.addAll(region.getGroupMemoryConstraints().keySet());
            if (region.getGroupClassLists() != null) groupNames.addAll(region.getGroupClassLists().keySet());

            int groupIndex = 0;
            for (String groupName : groupNames) {
                model.addRegionGroup(regionKey, groupName);

                String maxJobs = region.getGroupConstraints() != null ? region.getGroupConstraints().get(groupName) : null;
                if (maxJobs != null) {
                    model.setRegionGroupCustomerConstraint(regionKey, groupIndex, Integer.parseInt(maxJobs));
                }

                String maxMemory = region.getGroupMemoryConstraints() != null ? region.getGroupMemoryConstraints().get(groupName) : null;
                if (maxMemory != null) {
                    model.setRegionGroupMemorySize(regionKey, groupIndex, Integer.parseInt(maxMemory));
                }

                List<String> classes = region.getGroupClassLists() != null ? region.getGroupClassLists().get(groupName) : null;
                if (classes != null) {
                    for (String className : classes) {
                        Object classKey = context.classes.get(className);
                        if (classKey != null) {
                            model.addClassIntoRegionGroup(regionKey, groupIndex, classKey);
                        }
                    }
                }

                groupIndex++;
            }
        }
    }
}