package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class BlockingRegionDescriptor {

    private String name;
    private String type;
    private List<String> regionNodes = new ArrayList<String>();
    private LinkedHashMap<String, String> globalConstraints = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, LinkedHashMap<String, String>> classConstraints =
            new LinkedHashMap<String, LinkedHashMap<String, String>>();
    private LinkedHashMap<String, String> classWeights = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> classSizes = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> dropRules = new LinkedHashMap<String, String>();

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
        this.regionNodes = regionNodes != null ? regionNodes : new ArrayList<String>();
    }

    public void addRegionNode(String nodeName) {
        if (nodeName != null) {
            regionNodes.add(nodeName);
        }
    }

    public LinkedHashMap<String, String> getGlobalConstraints() {
        return globalConstraints;
    }

    public void setGlobalConstraints(LinkedHashMap<String, String> globalConstraints) {
        this.globalConstraints = globalConstraints != null
                ? globalConstraints
                : new LinkedHashMap<String, String>();
    }

    public void addGlobalConstraint(String key, String value) {
        if (key != null) {
            globalConstraints.put(key, value);
        }
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getClassConstraints() {
        return classConstraints;
    }

    public void setClassConstraints(
            LinkedHashMap<String, LinkedHashMap<String, String>> classConstraints) {
        this.classConstraints = classConstraints != null
                ? classConstraints
                : new LinkedHashMap<String, LinkedHashMap<String, String>>();
    }

    public void addClassConstraint(String className, String key, String value) {
        if (className == null || key == null) {
            return;
        }
        LinkedHashMap<String, String> constraints = classConstraints.get(className);
        if (constraints == null) {
            constraints = new LinkedHashMap<String, String>();
            classConstraints.put(className, constraints);
        }
        constraints.put(key, value);
    }

    public LinkedHashMap<String, String> getClassWeights() {
        return classWeights;
    }

    public void setClassWeights(LinkedHashMap<String, String> classWeights) {
        this.classWeights = classWeights != null ? classWeights : new LinkedHashMap<String, String>();
    }

    public void addClassWeight(String className, String weight) {
        if (className != null) {
            classWeights.put(className, weight);
        }
    }

    public LinkedHashMap<String, String> getClassSizes() {
        return classSizes;
    }

    public void setClassSizes(LinkedHashMap<String, String> classSizes) {
        this.classSizes = classSizes != null ? classSizes : new LinkedHashMap<String, String>();
    }

    public void addClassSize(String className, String size) {
        if (className != null) {
            classSizes.put(className, size);
        }
    }

    public LinkedHashMap<String, String> getDropRules() {
        return dropRules;
    }

    public void setDropRules(LinkedHashMap<String, String> dropRules) {
        this.dropRules = dropRules != null ? dropRules : new LinkedHashMap<String, String>();
    }

    public void addDropRule(String className, String dropRule) {
        if (className != null) {
            dropRules.put(className, dropRule);
        }
    }
}
