/**
 * Data Transfer Object representing the graphical shape of a directed edge between two
 * stations on the JSimGraph canvas ({@code JModelGUI.xsd}).
 * A shape is defined by the names of its source and target stations and an ordered list
 * of {@link ArcDescriptor} objects that form its Bezier path.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.gui.dto;

import java.util.ArrayList;
import java.util.List;

public class ConnectionShapeDescriptor {

    private String source;
    private String target;
    private final List<ArcDescriptor> arcs = new ArrayList<>();

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public List<ArcDescriptor> getArcs() { return arcs; }

    public void addArc(ArcDescriptor arc) {
        if (arc != null) arcs.add(arc);
    }
}