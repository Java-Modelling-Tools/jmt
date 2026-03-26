/**
 * This class is a Data Transfer Object (DTO) that represents a node in the simulation model.
 * It contains all the properties of a node, such as its name, sections, and parameters.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.List;


public class NodeDescriptor {

    private String name;
    private List<SectionDescriptor> sections = new ArrayList<>();
    private List<ParameterDescriptor> parameters = new ArrayList<>();
    private List<Double> classSoftDeadlines = new ArrayList<>();
    private List<Double> quantumSize = new ArrayList<>();
    private List<Double> quantumSwitchoverTime = new ArrayList<>();

    // Getters and Setters
    public List<Double> getClassSoftDeadlines() {
        return classSoftDeadlines;
    }

    public void setClassSoftDeadlines(List<Double> classSoftDeadlines) {
        this.classSoftDeadlines = classSoftDeadlines;
    }

    public List<Double> getQuantumSize() {
        return quantumSize;
    }

    public void setQuantumSize(List<Double> quantumSize) {
        this.quantumSize = quantumSize != null ? quantumSize : new ArrayList<>();
    }

    public void addQuantumSize(Double size) {
        if (size != null) {
            quantumSize.add(size);
        }
    }

    public List<Double> getQuantumSwitchoverTime() {
        return quantumSwitchoverTime;
    }

    public void setQuantumSwitchoverTime(List<Double> quantumSwitchoverTime) {
        this.quantumSwitchoverTime = quantumSwitchoverTime != null ? quantumSwitchoverTime : new ArrayList<>();
    }

    public void addQuantumSwitchoverTime(Double time) {
        if (time != null) {
            quantumSwitchoverTime.add(time);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SectionDescriptor> getSections() {
        return sections;
    }

    public void setSections(List<SectionDescriptor> sections) {
        this.sections = sections != null ? sections : new ArrayList<>();
    }

    public void addSection(SectionDescriptor section) {
        if (section != null) {
            sections.add(section);
        }
    }

    public List<ParameterDescriptor> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterDescriptor> parameters) {
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }

    public void addParameter(ParameterDescriptor parameter) {
        if (parameter != null) {
            parameters.add(parameter);
        }
    }
}
