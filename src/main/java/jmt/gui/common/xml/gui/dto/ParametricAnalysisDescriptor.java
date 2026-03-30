/**
 * Data Transfer Object representing the parametric analysis configuration block
 * stored in the GUI XML ({@code JModelGUI.xsd}).
 * It holds the enabled flag, the fully-qualified class name of the
 * {@link jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition}
 * implementation, and the key/value property map that configures that implementation.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.gui.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParametricAnalysisDescriptor {

    private boolean enabled;
    private String classPath;
    private final Map<String, String> properties = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getClassPath() { return classPath; }
    public void setClassPath(String classPath) { this.classPath = classPath; }

    public Map<String, String> getProperties() { return properties; }

    public void addProperty(String name, String value) {
        if (name != null) properties.put(name, value);
    }
}