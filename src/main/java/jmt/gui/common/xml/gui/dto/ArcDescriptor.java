/**
 * Data Transfer Object representing a single arc within a connection shape path
 * ({@code JModelGUI.xsd}). Each arc is defined by a source endpoint, two Bezier
 * control points, and a target endpoint.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.gui.dto;

public class ArcDescriptor {

    private double sourceX, sourceY;
    private double controlPoint1X, controlPoint1Y;
    private double controlPoint2X, controlPoint2Y;
    private double targetX, targetY;

    public double getSourceX() { return sourceX; }
    public void setSourceX(double sourceX) { this.sourceX = sourceX; }

    public double getSourceY() { return sourceY; }
    public void setSourceY(double sourceY) { this.sourceY = sourceY; }

    public double getControlPoint1X() { return controlPoint1X; }
    public void setControlPoint1X(double controlPoint1X) { this.controlPoint1X = controlPoint1X; }

    public double getControlPoint1Y() { return controlPoint1Y; }
    public void setControlPoint1Y(double controlPoint1Y) { this.controlPoint1Y = controlPoint1Y; }

    public double getControlPoint2X() { return controlPoint2X; }
    public void setControlPoint2X(double controlPoint2X) { this.controlPoint2X = controlPoint2X; }

    public double getControlPoint2Y() { return controlPoint2Y; }
    public void setControlPoint2Y(double controlPoint2Y) { this.controlPoint2Y = controlPoint2Y; }

    public double getTargetX() { return targetX; }
    public void setTargetX(double targetX) { this.targetX = targetX; }

    public double getTargetY() { return targetY; }
    public void setTargetY(double targetY) { this.targetY = targetY; }
}