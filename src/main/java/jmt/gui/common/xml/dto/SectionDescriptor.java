/**
 * This class is a Data Transfer Object (DTO) that represents a section within a node.
 * A section typically defines a specific behavior or configuration, like a routing strategy or a server's properties.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.List;

public class SectionDescriptor {

    private String className;
    private List<ParameterDescriptor> parameters = new ArrayList<>();

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
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }

    public void addParameter(ParameterDescriptor parameter) {
        if (parameter != null) {
            parameters.add(parameter);
        }
    }
}
