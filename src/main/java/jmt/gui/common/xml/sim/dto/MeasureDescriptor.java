/**
 * This class is a Data Transfer Object (DTO) that represents a performance measure to be collected during the simulation.
 * It holds all the properties of a measure, such as its name, type, and the node it refers to.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.dto;

public class MeasureDescriptor {

    private String name;
    private String nodeType;
    private String referenceNode;
    private String referenceUserClass;
    private String type;
    private String alpha;
    private String precision;
    private String verbose;
    private String serverType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getReferenceNode() {
        return referenceNode;
    }

    public void setReferenceNode(String referenceNode) {
        this.referenceNode = referenceNode;
    }

    public String getReferenceUserClass() {
        return referenceUserClass;
    }

    public void setReferenceUserClass(String referenceUserClass) {
        this.referenceUserClass = referenceUserClass;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlpha() {
        return alpha;
    }

    public void setAlpha(String alpha) {
        this.alpha = alpha;
    }

    public String getPrecision() {
        return precision;
    }

    public void setPrecision(String precision) {
        this.precision = precision;
    }

    public String getVerbose() {
        return verbose;
    }

    public void setVerbose(String verbose) {
        this.verbose = verbose;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }
}
