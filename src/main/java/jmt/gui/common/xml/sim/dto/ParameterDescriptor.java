/**
 * This class is a Data Transfer Object (DTO) that represents a parameter.
 * Parameters are used to configure various elements of the simulation model, such as distributions, strategies, and node properties.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ParameterDescriptor {

    private String name;
    private String classPath;
    private String value;
    private String refClass;
    private boolean isArray;
    private boolean map;
    private List<ParameterDescriptor> subParameters = new ArrayList<>();

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

    public boolean isMap() {
        return map;
    }

    public void setMap(boolean map) {
        this.map = map;
    }

    public List<ParameterDescriptor> getSubParameters() {
        return subParameters;
    }

    public void setSubParameters(List<ParameterDescriptor> subParameters) {
        this.subParameters = subParameters != null ? subParameters : new ArrayList<>();
    }

    public void addSubParameter(ParameterDescriptor subParameter) {
        if (subParameter != null) {
            subParameters.add(subParameter);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ParameterDescriptor that = (ParameterDescriptor) o;
        return isArray == that.isArray && map == that.map && Objects.equals(name, that.name) && Objects.equals(classPath, that.classPath) && Objects.equals(value, that.value) && Objects.equals(refClass, that.refClass) && Objects.equals(subParameters, that.subParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, classPath, value, refClass, isArray, map, subParameters);
    }
}
