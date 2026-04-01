/**
 * Data Transfer Object representing the GUI color metadata for a user class
 * ({@code JModelGUI.xsd}).
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.gui.dto;

public class ClassColorDescriptor {

    private String name;
    /** Web-style hex color string, e.g. {@code #FF8000FF}. */
    private String color;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}