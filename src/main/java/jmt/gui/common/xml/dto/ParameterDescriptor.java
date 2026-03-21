package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.List;

public class ParameterDescriptor {

    private String name;
    private String classPath;
    private String value;
    private String refClass;
    private boolean isArray;
    private List<ParameterDescriptor> subParameters = new ArrayList<ParameterDescriptor>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRefClass() {
        return refClass;
    }

    public void setRefClass(String refClass) {
        this.refClass = refClass;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public List<ParameterDescriptor> getSubParameters() {
        return subParameters;
    }

    public void setSubParameters(List<ParameterDescriptor> subParameters) {
        this.subParameters = subParameters != null ? subParameters : new ArrayList<ParameterDescriptor>();
    }

    public void addSubParameter(ParameterDescriptor subParameter) {
        if (subParameter != null) {
            subParameters.add(subParameter);
        }
    }
}

