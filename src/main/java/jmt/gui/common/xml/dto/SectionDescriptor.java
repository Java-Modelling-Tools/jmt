package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.List;

public class SectionDescriptor {

    private String className;
    private List<ParameterDescriptor> parameters = new ArrayList<ParameterDescriptor>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<ParameterDescriptor> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterDescriptor> parameters) {
        this.parameters = parameters != null ? parameters : new ArrayList<ParameterDescriptor>();
    }

    public void addParameter(ParameterDescriptor parameter) {
        if (parameter != null) {
            parameters.add(parameter);
        }
    }
}

