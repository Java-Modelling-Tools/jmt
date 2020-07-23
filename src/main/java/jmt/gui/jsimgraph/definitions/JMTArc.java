package jmt.gui.jsimgraph.definitions;

import java.awt.geom.Point2D;
import java.util.ArrayList;


/**
 * <p>Title: JmtArc connection structure</p>
 * <p>Description: This class is used to defined the arcs composing the Bezier curves</p>
 *
 * @author Emma Bortone
 *         Date: 13-feb-2020
 *         Time: 13.33.00
 *
 */

public class JMTArc {

    private static final long serialVersionUID = 1L;
    protected ArrayList<Point2D> arcPoints ;
    protected Point2D target ;
    protected Point2D source ;

    public JMTArc(Point2D source, ArrayList<Point2D> arcPoints, Point2D target) {
        this.source = source;
        this.arcPoints = arcPoints;
        this.target = target;
    }

    public JMTArc(JMTArc original) {
        this.source = (Point2D) original.source.clone();
        this.arcPoints = new ArrayList<Point2D>();
        for(int i=0;i<original.arcPoints.size();i++){
            arcPoints.add((Point2D) original.arcPoints.get(i).clone());
        }
        this.target = (Point2D) original.target.clone();
    }

    public ArrayList<Point2D> getArcPoints() {
        return arcPoints;
    }

    public Point2D getTarget() {
        return target;
    }
    public Point2D getSource() {
        return source;
    }

    public void setSource(Point2D source){
        this.source=source;
    }

    public void setTarget(Point2D target){
        this.target=target;
    }

    public void setFirstControlPoint(Point2D tangentSource){
        this.arcPoints.set(0,tangentSource);
    }

    public void setSecondControlPoint(Point2D tangentTarget){
        this.arcPoints.set(1,tangentTarget);
    }

}
