package jmt.gui.jsimgraph.controller;

import jmt.gui.jsimgraph.JGraphMod.CellComponent;
import jmt.gui.jsimgraph.JGraphMod.InputPort;
import jmt.gui.jsimgraph.JGraphMod.JmtCell;
import jmt.gui.jsimgraph.JGraphMod.OutputPort;
import jmt.gui.jsimgraph.UtilPoint;
import jmt.gui.jsimgraph.definitions.JMTArc;
import jmt.gui.jsimgraph.definitions.JMTPath;
import jmt.gui.jsimgraph.definitions.JMTPoint;
import org.jgraph.graph.PortView;
import org.jgraph.graph.VertexView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
/**
 * <p>Title:  Handles all the events when the user draws a Bezier connection </p>
 * <p>Description: This class handles all the steps involved in the drawing of a Bezier connection</p>
 *
 * @author Emma Bortone
 *         Date: 2020
 *
 *
 */

public class BezierConnectState extends UIStateDefault {



    protected Point2D start;
    protected Point2D current;
    protected PortView port;
    protected PortView firstPort;
    protected JMTPath path;
    protected GraphMouseListener ml;

    protected boolean pressed = false;

    protected Graphics2D dashedGraphics =null;

    protected Point2D sourceArc = new Point2D.Double(0, 0);
    protected Point2D targetArc = new Point2D.Double(0, 0);
    protected Point2D tangentSourceArc = new Point2D.Double(0, 0);
    protected Point2D tangentTargetArc = new Point2D.Double(0, 0);
    protected  ArrayList<JMTArc> arcs =new ArrayList<JMTArc>();
    protected String state = "FIRST_POINT";
    protected String previous_state ="FIRST_POINT";
    protected Point2D last;

    protected Point2D lastMousePosition = new Point2D.Double(0, 0);

    public BezierConnectState(Mediator mediator, GraphMouseListener ml) {
        super(mediator);
        this.ml = ml;
    }

    protected void setState(String new_state){
        previous_state=state;
        state = new_state;
    }

    @Override
    public void handlePress(MouseEvent e) {
        switch (state) {
            case "FIRST_POINT": {
                if (!e.isConsumed()) {
                    start = mediator.snap(e.getPoint());
                    firstPort = port = getOutPortViewAt(e.getX(), e.getY());
                    if (firstPort != null) {
                        start = mediator.toScreen(firstPort.getLocation(null));
                        last = start;

                        if (!e.isShiftDown()) {
                            setState("INTERMEDIARY_POINT");
                        } else {
                            setState("FIRST_TANGENT");
                        }
                    } else {
                        setState("FIRST_POINT");
                    }
                    e.consume();
                }
                pressed = true;
            }
            case "INTERMEDIARY_POINT": {
                if (!e.isShiftDown()) {
                    if (!e.isConsumed()) {
                        PortView end = getInPortViewAt(e.getX(), e.getY());
                        if (end != null) {
                            last = mediator.snap(e.getPoint());
                            if (!isAutomaticLoopRequired()){
                                ArrayList pointsList = new ArrayList<JMTPoint>();
                                ArrayList<JMTArc> arc = new ArrayList<JMTArc>();
                                pointsList.add(tangentSourceArc);
                                pointsList.add(tangentTargetArc);
                                arcs.add(new JMTArc(sourceArc, pointsList, new Point2D.Double(0, 0)));
                                path = new JMTPath(arcs);
                                mediator.connectBezier(start, current, path, end, firstPort);
                                initializeState();
                            }
                            else{
                                buildSelfLoop();
                                path = new JMTPath(arcs);
                                mediator.connectBezier(start, current, path, end, firstPort);
                                initializeState();
                                System.out.println("BezierConnectState self loop");

                            }
                        } else {
                            Graphics2D g = mediator.getGraphGraphics();
                            drawOverlay(g, state);
                            last = mediator.snap(e.getPoint());
                            targetArc = new Point2D.Double(mediator.snap(e.getPoint()).getX() - start.getX(), mediator.snap(e.getPoint()).getY() - start.getY());
                            ArrayList pointsList = new ArrayList<JMTPoint>();
                            pointsList.add(tangentSourceArc);
                            pointsList.add(tangentTargetArc);
                            arcs.add(new JMTArc(sourceArc, pointsList, targetArc));
                            g.setPaintMode();
                            drawCurve(g);

                            sourceArc = targetArc;
                            tangentSourceArc = new Point2D.Double(0, 0);
                            tangentTargetArc = new Point2D.Double(0, 0);
                            drawOverlay(g, state);
                            setState("INTERMEDIARY_POINT");
                        }

                        e.consume();
                    }
                    pressed = true;
                } else {
                    if (!e.isConsumed()) {
                        port = getInPortViewAt(e.getX(), e.getY());
                        if (port != null) {
                            last = mediator.toScreen(port.getLocation(null));
                        }
                        else {
                            last = mediator.snap(e.getPoint());
                        }
                        targetArc = new Point2D.Double(last.getX() - start.getX(), last.getY() - start.getY());
                        setState("DOUBLE_TANGENT");
                        e.consume();
                    }
                    pressed = true;
                }

            }
        }
    }

    @Override
    public void handleExit(MouseEvent e) {
        lastMousePosition=current;
        if (state.equals("INTERMEDIARY_POINT")){
            Graphics2D g = mediator.getGraphGraphics();
            drawOverlay(g, state,lastMousePosition);
        }
        mediator.setCursor(mediator.getOldCursor());
    }

    @Override
    public void handleEnter(MouseEvent e) {
        Graphics2D g = mediator.getGraphGraphics();
        if ((state.equals("INTERMEDIARY_POINT")) && (previous_state.equals("INTERMEDIARY_POINT"))){
            drawOverlay(g, state, lastMousePosition);
        }

        mediator.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        g.setPaintMode();
        mediator.getGraph().getGraphLayoutCache().reload();
        drawCurve(g);
    }

    @Override
    public void handleMove(MouseEvent e) {
        if (start != null && (state.equals("FIRST_POINT") || state.equals("INTERMEDIARY_POINT"))) {
            if (!e.isConsumed()) {
                Graphics2D g = mediator.getGraphGraphics();
                drawOverlay(g,state);


                current = mediator.snap(e.getPoint());
                port = getInPortViewAt(e.getX(), e.getY());
                if (port != null) {
                    current = mediator.toScreen(port.getLocation(null));
                }
                drawOverlay(g,state);
                setState(state);
                e.consume();
            }
        }
    }


    @Override
    public void handleDrag(MouseEvent e) {
        if (firstPort != null && (state.equals("FIRST_TANGENT") || state.equals("DOUBLE_TANGENT"))) {
            if (!e.isConsumed()) {
                Graphics2D g = mediator.getGraphGraphics();
                drawOverlay(g, state);

                current = mediator.snap(e.getPoint());

                drawOverlay(g, state);
                setState(state);
                e.consume();
            }
        }
        else if (start != null && (state.equals("FIRST_POINT") || state.equals("INTERMEDIARY_POINT"))) {
            if (!e.isConsumed()) {
                Graphics2D g = mediator.getGraphGraphics();
                drawOverlay(g,state);


                current = mediator.snap(e.getPoint());
                port = getInPortViewAt(e.getX(), e.getY());
                if (port != null) {
                    current = mediator.toScreen(port.getLocation(null));
                }
                drawOverlay(g,state);
                setState(state);
                e.consume();
            }
        }

    }


    @Override
    public void handleRelease(MouseEvent e){
        switch (state){
            case "FIRST_TANGENT" :
                if (!e.isConsumed()) {
                    Graphics2D g = mediator.getGraphGraphics();
                    drawOverlay(g,state);

                    g.setPaintMode();
                    tangentSourceArc = new Point2D.Double(mediator.snap(e.getPoint()).getX() - last.getX(), mediator.snap(e.getPoint()).getY() - last.getY());

                    setState("INTERMEDIARY_POINT");

                    drawOverlay(g,state);

                    e.consume();
                }
                break;
            case "DOUBLE_TANGENT" :
                if (!e.isConsumed()) {
                    PortView end = getInPortViewAt((int) last.getX(), (int) last.getY());
                    if (end != null) {
                        tangentTargetArc = UtilPoint.inversePoint(UtilPoint.subtractPoints(mediator.snap(e.getPoint()),last));

                        ArrayList pointsList = new ArrayList<JMTPoint>();
                        pointsList.add(tangentSourceArc);
                        pointsList.add(tangentTargetArc);
                        arcs.add(new JMTArc(sourceArc, pointsList, new Point2D.Double(0.0, 0.0)));

                        path = new JMTPath(arcs);

                        mediator.connectBezier(start, last, path, end, firstPort);
                        initializeState();

                    }
                    else {
                        // select tgt position
                        tangentTargetArc = UtilPoint.inversePoint(UtilPoint.subtractPoints(mediator.snap(e.getPoint()),last));

                        ArrayList pointsList = new ArrayList<JMTPoint>();
                        pointsList.add(tangentSourceArc);
                        pointsList.add(tangentTargetArc);
                        arcs.add(new JMTArc(sourceArc, pointsList, targetArc));

                        Graphics2D g = mediator.getGraphGraphics();
                        drawOverlay(g,state);

                        g.setPaintMode();

                        mediator.getGraph().getGraphLayoutCache().reload();
                        drawCurve(g);
                        setState("INTERMEDIARY_POINT");

                        tangentSourceArc= UtilPoint.inversePoint(tangentTargetArc);
                        tangentTargetArc = new Point2D.Double(0, 0);
                        sourceArc = targetArc;
                        drawOverlay(g,state);
                    }
                    e.consume();
                }
                break;
            case "INTERMEDIARY_POINT": //allows to do simple straight arcs by dragging the mouse
                if  (!e.isConsumed() && arcs.size()==0 ){
                    PortView end = getInPortViewAt(e.getX(), e.getY());
                    if (end != null) {
                        last = mediator.snap(e.getPoint());
                        if (!isAutomaticLoopRequired()){
                            ArrayList pointsList = new ArrayList<JMTPoint>();
                            ArrayList<JMTArc> arc = new ArrayList<JMTArc>();
                            pointsList.add(tangentSourceArc);
                            pointsList.add(tangentTargetArc);
                            arcs.add(new JMTArc(sourceArc, pointsList, new Point2D.Double(0, 0)));
                            path = new JMTPath(arcs);
                            mediator.connectBezier(start, current, path, end, firstPort);
                            initializeState();
                        }
                        else {
                            last=start;
                        }
                    }
                }
                break;
        }
    }


    /**
     * Gets the first portView of the output port of the cell at position
     * @param x
     * @param y
     * @return portView of the output port
     */
    protected PortView getOutPortViewAt(int x, int y) {
        return mediator.getOutPortViewAt(x, y);
    }

    /**
     * Gets the first portView of the input port of the cell at position
     * @param x
     * @param y
     * @return portView of the input port
     */
    protected PortView getInPortViewAt(int x, int y) {
        return mediator.getInPortViewAt(x, y);
    }

    /**
     * Called when an arc is finished.
     * Draws all the completed arcs
     *
     * @param g Graphics2D
     */
    private void drawCurve(Graphics2D g) {
        Color fg = Color.BLACK;
        g.setColor(fg);
        int n = arcs.size();
        GeneralPath shape = new GeneralPath();
        Point2D  controlPointSource, controlPointTarget, sourceOfArc, targetOfArc;
        if (start != null) {
            shape.moveTo((float) start.getX(), (float) start.getY());
            for (int i = 0; i < n; i++) {

                sourceOfArc = UtilPoint.addPoints(arcs.get(i).getSource(),start);
                targetOfArc = UtilPoint.addPoints(arcs.get(i).getTarget(),start);
                controlPointSource = UtilPoint.addPoints(arcs.get(i).getArcPoints().get(0), sourceOfArc);
                controlPointTarget = UtilPoint.addPoints(arcs.get(i).getArcPoints().get(1), targetOfArc);

                shape.curveTo((float) controlPointSource.getX(), (float) controlPointSource.getY()
                        , (float) controlPointTarget.getX(), (float) controlPointTarget.getY()
                        , (float) targetOfArc.getX(), (float) targetOfArc.getY());
                shape.moveTo((float) targetOfArc.getX(), (float) targetOfArc.getY());
            }
            g.draw(shape);
        }
    }

    /**
     * Called the user is currently drawing an arc (selecting the control points)
     * Draws the arc that is currently being drawn
     *
     * @param g Graphics2D
     * @param my_state current state
     */
    private void drawOverlay(Graphics2D g,String my_state) {
        drawOverlay(g,my_state, current);
    }

    /**
     * Called the user is currently drawing an arc (selecting the control points)
     * Draws the arc that is currently being drawn and its tangents
     *
     * @param g Graphics2D
     * @param my_state current state
     * @param mousePosition last known position of the mouse
     */
    private void drawOverlay(Graphics2D g,String my_state, Point2D mousePosition) {
        //defining variables
        double theta;
        double xOffset, yOffset;

        //setting environment
        Color bg = mediator.getGraphBackground();
        Color fg = Color.BLUE;
        g.setColor(bg);
        g.setXORMode(fg);
        createDashedGraphics(g);
        if (last != null && mousePosition != null && start != null) {
            switch (my_state) {
                case "FIRST_TANGENT":
                    dashedGraphics.draw(new Line2D.Double(last.getX(), last.getY(), mousePosition.getX(), mousePosition.getY()));
                    break;
                case "DOUBLE_TANGENT":
                    //draw double tangent
                    xOffset = mousePosition.getX() - last.getX();
                    yOffset = mousePosition.getY() - last.getY();
                    dashedGraphics.draw(new Line2D.Double(last.getX() - xOffset, last.getY() - yOffset, last.getX() + xOffset, last.getY() + yOffset));
                    Point2D targetTangent = UtilPoint.subtractPoints(last, new Point2D.Double(xOffset,yOffset));
                    //draw curve
                    if (mousePosition != last) {
                        GeneralPath currentArc = new GeneralPath();
                        currentArc.moveTo((float) start.getX() + sourceArc.getX(), (float) start.getY() + sourceArc.getY());
                        currentArc.curveTo(start.getX() + sourceArc.getX() + tangentSourceArc.getX()
                                , start.getY() + sourceArc.getY() + tangentSourceArc.getY()
                                , (float) targetTangent.getX(), (float) targetTangent.getY()
                                , (float) last.getX(), (float) last.getY());
                        g.draw(currentArc);

                        Point2D currentRightTangent= new Point2D.Double(-mousePosition.getX()+last.getX(),-mousePosition.getY()+last.getY());
                        Point2D source= new Point2D.Double(sourceArc.getX()+start.getX(), sourceArc.getY()+start.getY());
                        theta =  rotationAngle(source, last, currentRightTangent);
                        drawArrowHead(g, last, theta);

                        mediator.getGraph().getGraphLayoutCache().reload();
                    }
                    break;
                case "INTERMEDIARY_POINT":
                    //draw curve
                    GeneralPath shape = new GeneralPath();
                    shape.moveTo((float) last.getX(), (float) last.getY());
                    shape.curveTo((float) tangentSourceArc.getX() + last.getX(), (float) tangentSourceArc.getY() + last.getY()
                            , (float) mousePosition.getX(), (float) mousePosition.getY()
                            , (float) mousePosition.getX(), (float) mousePosition.getY());

                    g.draw(shape);
                    theta =  rotationAngle(last, mousePosition, tangentTargetArc);
                    drawArrowHead(g, mousePosition, theta );
                    mediator.getGraph().getGraphLayoutCache().reload();
                    break;
            }
        }
    }

    /**
     * Draws the triangular shape at the end on the arc being drawn
     *  @param g Graphics2D
     *  @param target position where the shape needs to be drawn
     *  @param theta angle of rotation of the arrow head
     */
    private void drawArrowHead(Graphics2D g, Point2D target, double theta){
        Point2D target_clone = (Point2D) target.clone();
        Shape EndShape;;
        EndShape = createLineEnd(10, 1, new Point2D.Double(target_clone.getX()  - 10*Math.cos(theta),
                target_clone.getY() - 10*Math.sin(theta)), target_clone);
        g.draw(EndShape);
        g.fill(EndShape);
    }

    /**
     * Initialize the state
     */
    void initializeState(){
        mediator.getGraph().getGraphLayoutCache().reload();
        setState("FIRST_POINT");
        setState("FIRST_POINT");
        arcs = new ArrayList<JMTArc>();
        sourceArc = new Point2D.Double(0, 0);
        targetArc = new Point2D.Double(0, 0);
        tangentSourceArc = new Point2D.Double(0, 0);
        tangentTargetArc =  new Point2D.Double(0, 0);

        firstPort = null;
        port = null;
        start = null;
        current = null;
        mediator.graphRepaint();

    }

    /**
     *  @return the rotation angle of the last arc (necessay to orient the arrow shape at the end of the arc)
     *  @param source
     *  @param target
     *  @param tangentTarget
     */
    private double rotationAngle(Point2D source, Point2D target, Point2D tangentTarget) {
        double theta, x, y;

        //If the control point at the  target is different than 0, we use that for calculating the rotation angle
        if (tangentTarget.getX() != 0 || tangentTarget.getY() != 0) {
            y = -tangentTarget.getY();
            x = -tangentTarget.getX();
        }
        //If the control point at the target is equal to 0, then we check if we can use the control point at the source
        //of the arc
        else if (tangentSourceArc.getX() != 0 || tangentSourceArc.getY() != 0) {
            //If the control point at the source of the arc is different than 0, we use that for calculating the
            //rotation angle
            y = target.getY() - (start.getY() + sourceArc.getY() + tangentSourceArc.getY());
            x = target.getX() - (start.getX() + sourceArc.getX() + tangentSourceArc.getX());
        } else {
            //If the control point at the source of the arc is equal to 0, the rotation angle is calculated using the
            //position of the source and target of the arc.
            y =(target.getY()) - (source.getY());
            x =(target.getX()) - (source.getX());
        }
        theta = Math.atan2(y, x);
        return theta;
    }

    /**
     * Create the triangular shape at the end on the arc being drawn
     *  @param var1
     *  @param var2
     *  @param var3
     *  @param var4
     */
    private Shape createLineEnd(int var1, int var2, Point2D var3, Point2D var4) {
        if (var3 != null && var4 != null) {
            int var5 = (int)Math.max(1.0D, var4.distance(var3));
            int var6 = (int)(-((double)var1 * (var4.getX() - var3.getX()) / (double)var5));
            int var7 = (int)(-((double)var1 * (var4.getY() - var3.getY()) / (double)var5));
            Polygon var8;
            Point2D var9;

            var8 = new Polygon();
            var8.addPoint((int)var4.getX(), (int)var4.getY());
            var8.addPoint((int)(var4.getX() + (double)var6 + (double)(var7 / 2)), (int)(var4.getY() + (double)var7 - (double)(var6 / 2)));
            var9 = (Point2D)var4.clone();
            if (var2 == 1) {
                var4.setLocation((double) ((int) (var4.getX() + (double) (var6 * 2 / 3))), (double) ((int) (var4.getY() + (double) (var7 * 2 / 3))));
                var8.addPoint((int) var4.getX(), (int) var4.getY());
            }
            var8.addPoint((int)(var9.getX() + (double)var6 - (double)(var7 / 2)), (int)(var9.getY() + (double)var7 + (double)(var6 / 2)));
            return var8;

        } else {
            return null;
        }
    }

    private void createDashedGraphics(Graphics2D g){
        dashedGraphics = (Graphics2D) g.create();
        BasicStroke dashed;
        dashed =new BasicStroke(1.0f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, new float[]{10.0f}, 0.0f);
        dashedGraphics.setStroke(dashed);
    }

    /**
    @return true if the station as a self loop and if the user didn't draw any shape
     */
    private boolean isAutomaticLoopRequired(){
        Object targetKey =  ((CellComponent) ((JmtCell) ((InputPort) (getInPortViewAt((int) last.getX(), (int) last.getY()).getCell())).getUserObject()).getUserObject()).getKey();
        Object sourceKey =  ((CellComponent) ((JmtCell) ((OutputPort) (firstPort.getCell())).getUserObject()).getUserObject()).getKey();
        return ((sourceKey==targetKey) && (arcs.size()==0));
    }

    /**
     * Build the list of arcs in order to have a nice self loop
     */
    private void buildSelfLoop(){
        JmtCell sourceCell =  ((JmtCell) ((OutputPort) (firstPort.getCell())).getUserObject());
        ArrayList pointsList = new ArrayList<JMTPoint>();
        pointsList.add( new Point2D.Double(0, 0));
        pointsList.add( new Point2D.Double(0, 0));
        double xOffset, yOffset, rotationAngle;
        Point2D.Double point1,  point2, point3, point4, inputPort_location, outputPort_location;
        inputPort_location = (Point2D.Double) getInPortViewAt((int) last.getX(), (int) last.getY()).getLocation();
        outputPort_location = (Point2D.Double) firstPort.getLocation();

        double height =inputPort_location.getY()-outputPort_location.getY();
        double width = inputPort_location.getX()-outputPort_location.getX();
        rotationAngle = ((!(sourceCell.isLeftInputCell())) ? (sourceCell.getRotationAngle()+180.0) : sourceCell.getRotationAngle());
        xOffset = ((width >= 0) ? -20 : 20);
        yOffset = ((height >= 0) ? 20 : -20);
        if (Math.abs(height) <= 1.0){
            yOffset = 40.0;
        }
        if (Math.abs(width) <= 1.0){
            xOffset = 40.0;
        }
        //If station is in vertical position
        if (rotationAngle==-90.0 || rotationAngle==90.0 ||rotationAngle==270.0 || rotationAngle==-270.0){
            point1 = new Point2D.Double(0.0, -yOffset);
            point2= new Point2D.Double(xOffset, point1.getY());
            point3= new Point2D.Double(point2.getX(),height+yOffset);
            point4= new Point2D.Double(0.0,point3.getY());
        }
        else {
            point1 = new Point2D.Double(xOffset, 0.0);
            point2= new Point2D.Double(point1.getX(), height+yOffset);
            point3= new Point2D.Double(width-xOffset, point2.getY());
            point4= new Point2D.Double(point3.getX(), height);
        }
        arcs.add(new JMTArc(sourceArc, pointsList, point1));
        arcs.add(new JMTArc(point1, pointsList, point2));
        arcs.add(new JMTArc(point2, pointsList, point3));
        arcs.add(new JMTArc(point3, pointsList, point4));
        arcs.add(new JMTArc(point4, pointsList,  new Point2D.Double(0, 0)));
    }
}



