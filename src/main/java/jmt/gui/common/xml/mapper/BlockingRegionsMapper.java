/**
 * This class is responsible for mapping {@link BlockingRegionDescriptor} objects from the DTO to the {@link CommonModel}
 * and vice-versa. It iterates through the blocking regions defined in the {@link SimDescriptor} and configures
 * their properties and constraints, or extracts them from the model to build the DTO.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.BlockingRegionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class BlockingRegionsMapper implements ModelElementMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
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

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model;
        Vector<Object> regionKeys = model.getRegionKeys();

        if (regionKeys == null || regionKeys.isEmpty()) {
            return;
        }

        for (Object regionKey : regionKeys) {
            BlockingRegionDescriptor regionDto = new BlockingRegionDescriptor();
            mapModelBlockingRegionToDto(model, regionKey, regionDto);
            dto.addBlockingRegion(regionDto);
        }
    }

    private void mapModelBlockingRegionToDto(CommonModel model, Object regionKey, BlockingRegionDescriptor regionDto) {
        // Name and Type
        regionDto.setName(model.getRegionName(regionKey));
        regionDto.setType(model.getRegionType(regionKey));

        // Global Constraints
        Integer maxJobs = model.getRegionCustomerConstraint(regionKey);
        if (maxJobs != null) {
            regionDto.addGlobalConstraint(XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS, maxJobs.toString());
        }

        Integer maxMemory = model.getRegionMemorySize(regionKey);
        if (maxMemory != null) {
            regionDto.addGlobalConstraint(XMLConstantNames.XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY, maxMemory.toString());
        }

        // Region Nodes
        Set<Object> stations = model.getBlockingRegionStations(regionKey);
        if (stations != null) {
            for (Object stationKey : stations) {
                regionDto.addRegionNode(model.getStationName(stationKey));
            }
        }

        // Constraints, Weights, Sizes, and Drop Rules mapped per User Class
        extractClassSpecificDataToDto(model, regionKey, regionDto);

        // Group Constraints and Class Lists
        extractGroupsToDto(model, regionKey, regionDto);
    }

    private void extractClassSpecificDataToDto(CommonModel model, Object regionKey, BlockingRegionDescriptor regionDto) {
        Vector<Object> classKeys = model.getClassKeys();
        if (classKeys == null) return;

        for (Object classKey : classKeys) {
            String className = model.getClassName(classKey);
            if (className == null) continue;

            // Class Constraints
            Integer classMaxJobs = model.getRegionClassCustomerConstraint(regionKey, classKey);
            if (classMaxJobs != null) {
                regionDto.addClassConstraint(className, XMLConstantNames.XML_A_CLASSCONSTRAINT_MAXJOBS, classMaxJobs.toString());
            }

            Integer classMaxMem = model.getRegionClassMemorySize(regionKey, classKey);
            if (classMaxMem != null) {
                regionDto.addClassConstraint(className, XMLConstantNames.XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY, classMaxMem.toString());
            }

            Double classSoftDeadline = model.getRegionClassSoftDeadline(regionKey, classKey);
            if (classSoftDeadline != null) {
                regionDto.addClassConstraint(className, XMLConstantNames.XML_A_CLASS_SOFT_DEADLINE, classSoftDeadline.toString());
            }

            // Class Weight
            Integer classWeight = model.getRegionClassWeight(regionKey, classKey);
            if (classWeight != null) {
                regionDto.addClassWeight(className, classWeight.toString());
            }

            // Class Size
            Integer classSize = model.getRegionClassSize(regionKey, classKey);
            if (classSize != null) {
                regionDto.addClassSize(className, classSize.toString());
            }

            // Class Drop Rule
            Boolean dropRule = model.getRegionClassDropRule(regionKey, classKey);
            if (dropRule != null) {
                regionDto.addDropRule(className, dropRule.toString());
            }
        }
    }

    private void extractGroupsToDto(CommonModel model, Object regionKey, BlockingRegionDescriptor regionDto) {
        int index = 0;
        String groupName;

        // Loop through all groups by index until we run out
        while ((groupName = model.getRegionGroupName(regionKey, index)) != null) {

            // Group Constraints
            Integer groupMaxJobs = model.getRegionGroupCustomerConstraint(regionKey, index);
            if (groupMaxJobs != null) {
                regionDto.addGroupConstraint(groupName, groupMaxJobs.toString());
            }

            Integer groupMaxMem = model.getRegionGroupMemorySize(regionKey, index);
            if (groupMaxMem != null) {
                regionDto.addGroupMemoryConstraint(groupName, groupMaxMem.toString());
            }

            // Group Class Lists
            List<Object> groupClassKeys = model.getRegionGroupClassList(regionKey, index);
            if (groupClassKeys != null) {
                for (Object classKey : groupClassKeys) {
                    String className = model.getClassName(classKey);
                    if (className != null) {
                        regionDto.addGroupClass(groupName, className);
                    }
                }
            }
            index++;
        }
    }
}