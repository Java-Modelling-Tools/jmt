/**
 * This class is a Data Transfer Object (DTO) that represents a connection between two nodes in the simulation model.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.sim.dto;

public class ConnectionDescriptor {

    private String source;
    private String target;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
