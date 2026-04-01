/**
 * Root Data Transfer Object for the GUI XML document ({@code JModelGUI.xsd}).
 * Aggregates all graphical metadata associated with a simulation model: class colours,
 * station positions, connection shapes, and the optional parametric analysis configuration.
 *
 * This DTO is populated by {@link jmt.gui.common.xml.parsers.GuiParser} during the
 * reading phase and consumed by
 * {@link jmt.gui.common.xml.serializer.GuiSerializer} during the writing phase.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.gui.dto;

import java.util.ArrayList;
import java.util.List;

public class GuiDescriptor {

    private final List<ClassColorDescriptor> classColors             = new ArrayList<>();
    private final List<StationPositionDescriptor> stationPositions   = new ArrayList<>();
    private final List<ConnectionShapeDescriptor> connectionShapes   = new ArrayList<>();

    /** Null when parametric analysis is absent or disabled. */
    private ParametricAnalysisDescriptor parametricAnalysis;

    // --- Class colors ---

    public List<ClassColorDescriptor> getClassColors() { return classColors; }

    public void addClassColor(ClassColorDescriptor desc) {
        if (desc != null) classColors.add(desc);
    }

    // --- Station positions ---

    public List<StationPositionDescriptor> getStationPositions() { return stationPositions; }

    public void addStationPosition(StationPositionDescriptor desc) {
        if (desc != null) stationPositions.add(desc);
    }

    // --- Connection shapes ---

    public List<ConnectionShapeDescriptor> getConnectionShapes() { return connectionShapes; }

    public void addConnectionShape(ConnectionShapeDescriptor desc) {
        if (desc != null) connectionShapes.add(desc);
    }

    // --- Parametric analysis ---

    public ParametricAnalysisDescriptor getParametricAnalysis() { return parametricAnalysis; }

    public void setParametricAnalysis(ParametricAnalysisDescriptor parametricAnalysis) {
        this.parametricAnalysis = parametricAnalysis;
    }
}