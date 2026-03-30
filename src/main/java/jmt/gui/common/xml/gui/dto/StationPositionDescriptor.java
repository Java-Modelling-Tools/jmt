/**
 * Data Transfer Object representing the graphical position and orientation of a
 * station node on the JSimGraph canvas ({@code JModelGUI.xsd}).
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.gui.dto;

public class StationPositionDescriptor {

    private String name;
    private double x;
    private double y;
    private boolean rotate;
    private double angle;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public boolean isRotate() { return rotate; }
    public void setRotate(boolean rotate) { this.rotate = rotate; }

    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }
}