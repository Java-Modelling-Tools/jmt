package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlockingRegionDescriptor {

    private String name;
    private String type;
    private List<String> regionNodes = new ArrayList<>();
    private Map<String, String> globalConstraints = new LinkedHashMap<>();
    private Map<String, Map<String, String>> classConstraints = new LinkedHashMap<>();
    private Map<String, String> classWeights = new LinkedHashMap<>();
    private Map<String, String> classSizes = new LinkedHashMap<>();
    private Map<String, String> dropRules = new LinkedHashMap<>();
    private final Map<String, String> groupConstraints = new LinkedHashMap<>();
    private final Map<String, String> groupMemoryConstraints = new LinkedHashMap<>();
    private final Map<String, List<String>> groupClassLists = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getRegionNodes() {
        return regionNodes;
    }

    public void setRegionNodes(List<String> regionNodes) {
        this.regionNodes = regionNodes != null ? regionNodes : new ArrayList<>();
    }

    public void addRegionNode(String nodeName) {
        if (nodeName != null) {
            regionNodes.add(nodeName);
        }
    }

    public Map<String, String> getGlobalConstraints() {
        return globalConstraints;
    }

    public void setGlobalConstraints(Map<String, String> globalConstraints) {
        this.globalConstraints = globalConstraints != null
                ? globalConstraints
                : new LinkedHashMap<>();
    }

    public void addGlobalConstraint(String key, String value) {
        if (key != null) {
            globalConstraints.put(key, value);
        }
    }

    public Map<String, Map<String, String>> getClassConstraints() {
        return classConstraints;
    }

    public void setClassConstraints(
            Map<String, Map<String, String>> classConstraints) {
        this.classConstraints = classConstraints != null
                ? classConstraints
                : new LinkedHashMap<>();
    }

    public void addClassConstraint(String className, String key, String value) {
        if (className == null || key == null) {
            return;
        }
        classConstraints.computeIfAbsent(className, k -> new LinkedHashMap<>()).put(key, value);
    }

    public Map<String, String> getClassWeights() {
        return classWeights;
    }

    public void setClassWeights(Map<String, String> classWeights) {
        this.classWeights = classWeights != null ? classWeights : new LinkedHashMap<>();
    }

    public void addClassWeight(String className, String weight) {
        if (className != null) {
            classWeights.put(className, weight);
        }
    }

    public Map<String, String> getClassSizes() {
        return classSizes;
    }

    public void setClassSizes(Map<String, String> classSizes) {
        this.classSizes = classSizes != null ? classSizes : new LinkedHashMap<>();
    }

    public void addClassSize(String className, String size) {
        if (className != null) {
            classSizes.put(className, size);
        }
    }

    public Map<String, String> getDropRules() {
        return dropRules;
    }

    public void setDropRules(Map<String, String> dropRules) {
        this.dropRules = dropRules != null ? dropRules : new LinkedHashMap<>();
    }

    public void addDropRule(String className, String dropRule) {
        if (className != null) {
            dropRules.put(className, dropRule);
        }
    }

    public Map<String, String> getGroupConstraints() {
        return groupConstraints;
    }

    public void addGroupConstraint(String groupName, String maxJobs) {
        if (groupName != null) {
            groupConstraints.put(groupName, maxJobs);
        }
    }

    public Map<String, String> getGroupMemoryConstraints() {
        return groupMemoryConstraints;
    }

    public void addGroupMemoryConstraint(String groupName, String maxMemory) {
        if (groupName != null) {
            groupMemoryConstraints.put(groupName, maxMemory);
        }
    }

    public Map<String, List<String>> getGroupClassLists() {
        return groupClassLists;
    }

    public void addGroupClass(String groupName, String className) {
        if (groupName == null || className == null) {
            return;
        }
        groupClassLists.computeIfAbsent(groupName, k -> new ArrayList<>()).add(className);
    }
}
