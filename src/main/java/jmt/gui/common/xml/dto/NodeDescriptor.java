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
    private Double quantumSize;
    private Double quantumSwitchoverTime;

    // Getters and Setters
    public List<Double> getClassSoftDeadlines() {
        return classSoftDeadlines;
    }

    public void setClassSoftDeadlines(List<Double> classSoftDeadlines) {
        this.classSoftDeadlines = classSoftDeadlines;
    }

    public Double getQuantumSize() {
        return quantumSize;
    }

    public void setQuantumSize(Double quantumSize) {
        this.quantumSize = quantumSize;
    }

    public Double getQuantumSwitchoverTime() {
        return quantumSwitchoverTime;
    }

    public void setQuantumSwitchoverTime(Double quantumSwitchoverTime) {
        this.quantumSwitchoverTime = quantumSwitchoverTime;
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
