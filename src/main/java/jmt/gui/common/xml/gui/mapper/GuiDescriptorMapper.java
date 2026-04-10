/**
 * Bidirectional mapper between a {@link GuiDescriptor} DTO and the live
 * {@link jmt.gui.jsimgraph.definitions.JSimGraphModel}.
 *
 * <p><b>Reading phase</b> ({@link #mapDtoToModel}): applies GUI metadata (colours,
 * positions, connection shapes, parametric analysis configuration) to an already
 * populated {@link jmt.gui.common.definitions.CommonModel}.
 * This method must be called <em>after</em> the simulation data has been loaded, since
 * it resolves station and class names against keys that the sim mapper already created.
 *
 * <p><b>Writing phase</b> ({@link #mapModelToDto}): extracts GUI metadata from the model
 * into a fresh {@link GuiDescriptor} ready for serialization.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.gui.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.parametric.ParametricAnalysisChecker;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.common.definitions.parametric.ParametricAnalysisModelFactory;
import jmt.gui.common.xml.GuiXMLConstants;
import jmt.gui.common.xml.gui.dto.*;
import jmt.gui.jsimgraph.definitions.JMTArc;
import jmt.gui.jsimgraph.definitions.JMTPath;
import jmt.gui.jsimgraph.definitions.JMTPoint;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiDescriptorMapper implements GuiXMLConstants {

    // -----------------------------------------------------------------------
    // Reading phase  (DTO → Model)
    // -----------------------------------------------------------------------

    /**
     * Applies all GUI metadata stored in the {@link GuiDescriptor} DTO to the given
     * {@link CommonModel}.  When the model is not a {@link JSimGraphModel} the graphical
     * sections are silently skipped; the parametric analysis block is always processed.
     *
     * @param dto   the DTO produced by {@link jmt.gui.common.xml.gui.parsers.GuiParser}
     * @param model the target model, already populated with simulation data
     */
    public void mapDtoToModel(GuiDescriptor dto, CommonModel model) {
        if (model instanceof JSimGraphModel) {
            JSimGraphModel graphModel = (JSimGraphModel) model;
            applyClassColors(dto, graphModel);
            applyStationPositions(dto, graphModel);
            applyConnectionShapes(dto, graphModel);
        }
        applyParametricAnalysis(dto, model);
    }

    private void applyClassColors(GuiDescriptor dto, JSimGraphModel model) {
        Map<String, Object> classKeysByName = buildClassNameMap(model);

        for (ClassColorDescriptor desc : dto.getClassColors()) {
            Object key = classKeysByName.get(desc.getName());
            if (key == null) {
                System.out.println("Warning - Found color info for class '"
                        + desc.getName() + "' which is not present in the model.");
                continue;
            }
            String hex = desc.getColor();
            if (hex == null) {
                hex = "";
            }
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            try {
                Color color = new Color((int) Long.parseLong(hex, 16), true);
                model.setClassColor(key, color);
            } catch (NumberFormatException e) {
                System.out.println("Warning - Bad color format for class '"
                        + desc.getName() + "'. Assigning a new color.");
                model.setClassColor(key, model.getNewColor());
            }
        }
    }

    private void applyStationPositions(GuiDescriptor dto, JSimGraphModel model) {
        Map<String, Object> stationKeysByName = buildStationNameMap(model);

        for (StationPositionDescriptor desc : dto.getStationPositions()) {
            Object key = stationKeysByName.get(desc.getName());
            if (key == null) {
                System.out.println("Error - Found position info for station '"
                        + desc.getName() + "' which is not present in the model.");
                continue;
            }
            model.setStationPosition(key,
                    new JMTPoint(desc.getX(), desc.getY(), desc.isRotate(), desc.getAngle()));
        }
    }

    private void applyConnectionShapes(GuiDescriptor dto, JSimGraphModel model) {
        Map<String, Object> stationKeysByName = buildStationNameMap(model);

        for (ConnectionShapeDescriptor shape : dto.getConnectionShapes()) {
            Object sourceKey = stationKeysByName.get(shape.getSource());
            Object targetKey = stationKeysByName.get(shape.getTarget());

            if (sourceKey == null || targetKey == null
                    || !model.areConnected(sourceKey, targetKey)) {
                System.out.println("Error - Found connection shape info for '"
                        + shape.getSource() + "' → '" + shape.getTarget()
                        + "' which is not present in the model.");
                continue;
            }

            ArrayList<JMTArc> arcList = new ArrayList<>();
            for (ArcDescriptor arc : shape.getArcs()) {
                ArrayList<Point2D> controlPoints = new ArrayList<>();
                controlPoints.add(new Point2D.Double(arc.getControlPoint1X(), arc.getControlPoint1Y()));
                controlPoints.add(new Point2D.Double(arc.getControlPoint2X(), arc.getControlPoint2Y()));
                arcList.add(new JMTArc(
                        new Point2D.Double(arc.getSourceX(), arc.getSourceY()),
                        controlPoints,
                        new Point2D.Double(arc.getTargetX(), arc.getTargetY())));
            }
            model.setConnectionShape(sourceKey, targetKey, new JMTPath(arcList));
        }
    }

    private void applyParametricAnalysis(GuiDescriptor dto, CommonModel model) {
        ParametricAnalysisDescriptor pad = dto.getParametricAnalysis();
        if (pad == null || !pad.isEnabled()) return;

        model.setParametricAnalysisEnabled(true);
        String classPath = pad.getClassPath();

        try {
            Class<?>[] paramTypes = {
                    jmt.gui.common.definitions.ClassDefinition.class,
                    jmt.gui.common.definitions.StationDefinition.class,
                    jmt.gui.common.definitions.SimulationDefinition.class
            };
            Object[] params = {model, model, model};
            ParametricAnalysisDefinition paModel =
                    (ParametricAnalysisDefinition) Class.forName(classPath)
                            .getConstructor(paramTypes)
                            .newInstance(params);

            for (Map.Entry<String, String> entry : pad.getProperties().entrySet()) {
                paModel.setProperty(entry.getKey(), entry.getValue());
            }
            paModel.searchForAvailableSteps();
            paModel.createValuesSet();
            model.setParametricAnalysisModel(paModel);

        } catch (InvocationTargetException e) {
            JOptionPane.showMessageDialog(null, "Invocation target exception: " + classPath);
        } catch (NoSuchMethodException e) {
            JOptionPane.showMessageDialog(null, "No such method: " + classPath);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Class not found: " + classPath);
        } catch (InstantiationException e) {
            JOptionPane.showMessageDialog(null, "Instantiation exception: " + classPath);
        } catch (IllegalAccessException e) {
            JOptionPane.showMessageDialog(null, "Illegal access exception: " + classPath);
        }
    }

    // -----------------------------------------------------------------------
    // Writing phase  (Model → DTO)
    // -----------------------------------------------------------------------

    /**
     * Extracts all GUI metadata from the given {@link CommonModel} into a fresh
     * {@link GuiDescriptor} DTO.
     * When the model is not a {@link JSimGraphModel} only the parametric analysis
     * block is written.
     *
     * @param model the source model
     * @return a populated {@link GuiDescriptor} ready for serialization
     */
    public GuiDescriptor mapModelToDto(CommonModel model) {
        GuiDescriptor dto = new GuiDescriptor();

        if (model instanceof JSimGraphModel) {
            JSimGraphModel graphModel = (JSimGraphModel) model;
            extractClassColors(graphModel, dto);
            extractStationPositions(graphModel, dto);
            extractConnectionShapes(graphModel, dto);
        }
        extractParametricAnalysis(model, dto);
        return dto;
    }

    private void extractClassColors(JSimGraphModel model, GuiDescriptor dto) {
        for (Object key : model.getClassKeys()) {
            ClassColorDescriptor desc = new ClassColorDescriptor();
            desc.setName(model.getClassName(key));
            // Convert Color to web-style hex string (matches GuiXMLWriter behavior)
            String hex = "#" + Integer.toHexString(model.getClassColor(key).getRGB()).toUpperCase();
            desc.setColor(hex);
            dto.addClassColor(desc);
        }
    }

    private void extractStationPositions(JSimGraphModel model, GuiDescriptor dto) {
        for (Object key : model.getStationKeys()) {
            JMTPoint pos = model.getStationPosition(key);
            StationPositionDescriptor desc = new StationPositionDescriptor();
            desc.setName(model.getStationName(key));
            desc.setX(pos.getX());
            desc.setY(pos.getY());
            desc.setRotate(pos.isRotate());
            desc.setAngle(pos.getAngle());
            dto.addStationPosition(desc);
        }
    }

    private void extractConnectionShapes(JSimGraphModel model, GuiDescriptor dto) {
        List<Object> keys = model.getStationKeys();
        for (int i = 0; i < keys.size(); i++) {
            for (int j = 0; j < keys.size(); j++) {
                Object src = keys.get(i);
                Object tgt = keys.get(j);
                if (!model.hasConnectionShape(src, tgt)) continue;

                JMTPath path = model.getConnectionShape(src, tgt);
                ConnectionShapeDescriptor shape = new ConnectionShapeDescriptor();
                shape.setSource(model.getStationName(src));
                shape.setTarget(model.getStationName(tgt));

                for (int k = 0; k < path.getArcsNb(); k++) {
                    JMTArc jmtArc = path.getArc(k);
                    ArcDescriptor arc = new ArcDescriptor();
                    arc.setSourceX(jmtArc.getSource().getX());
                    arc.setSourceY(jmtArc.getSource().getY());
                    arc.setControlPoint1X(jmtArc.getArcPoints().get(0).getX());
                    arc.setControlPoint1Y(jmtArc.getArcPoints().get(0).getY());
                    arc.setControlPoint2X(jmtArc.getArcPoints().get(1).getX());
                    arc.setControlPoint2Y(jmtArc.getArcPoints().get(1).getY());
                    arc.setTargetX(jmtArc.getTarget().getX());
                    arc.setTargetY(jmtArc.getTarget().getY());
                    shape.addArc(arc);
                }
                dto.addConnectionShape(shape);
            }
        }
    }

    private void extractParametricAnalysis(CommonModel model, GuiDescriptor dto) {
        if (!model.isParametricAnalysisEnabled()) {
            return;
        }

        ParametricAnalysisDefinition paModel = model.getParametricAnalysisModel();
        // Consistency check: ask the user to correct the PA model if stale
        int result = paModel.checkCorrectness(false);
        if (result != 0) {
            int choice = JOptionPane.showConfirmDialog(null,
                    "The parametric analysis model is not consistent with the simulation model, try to change it?",
                    "JMT - Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                if (result == 2) {
                    ParametricAnalysisChecker checker = new ParametricAnalysisChecker(model, model, model);
                    if (!checker.canBeEnabled()) {
                        return;
                    }
                    String available = checker.getRunnableParametricAnalysis()[0];
                    paModel = ParametricAnalysisModelFactory
                            .createParametricAnalysisModel(available, model, model, model);
                } else {
                    paModel.checkCorrectness(true);
                }
            }
        }

        ParametricAnalysisDescriptor pad = new ParametricAnalysisDescriptor();
        pad.setEnabled(true);
        pad.setClassPath(paModel.getClass().getName());
        paModel.getProperties().forEach(pad::addProperty);
        dto.setParametricAnalysis(pad);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Map<String, Object> buildClassNameMap(CommonModel model) {
        Map<String, Object> map = new HashMap<>();
        for (Object key : model.getClassKeys()) {
            map.put(model.getClassName(key), key);
        }
        return map;
    }

    private Map<String, Object> buildStationNameMap(CommonModel model) {
        Map<String, Object> map = new HashMap<>();
        for (Object key : model.getStationKeys()) {
            map.put(model.getStationName(key), key);
        }
        return map;
    }
}