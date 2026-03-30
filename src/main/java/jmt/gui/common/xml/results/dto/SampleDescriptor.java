package jmt.gui.common.xml.results.dto;

/**
 * Data Transfer Object representing a single statistical sample point
 * within a simulation measure. Used by both normal and parametric analysis results.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */

public class SampleDescriptor {

    private double mean;
    private double upperBound;
    private double lowerBound;
    private double lastIntervalAvgValue;
    private double simulationTime;

    /** Only meaningful for parametric analysis samples. */
    private boolean valid;

    // --- Getters & Setters ---

    public double getMean() { return mean; }
    public void setMean(double mean) { this.mean = mean; }

    public double getUpperBound() { return upperBound; }
    public void setUpperBound(double upperBound) { this.upperBound = upperBound; }

    public double getLowerBound() { return lowerBound; }
    public void setLowerBound(double lowerBound) { this.lowerBound = lowerBound; }

    public double getLastIntervalAvgValue() { return lastIntervalAvgValue; }
    public void setLastIntervalAvgValue(double lastIntervalAvgValue) { this.lastIntervalAvgValue = lastIntervalAvgValue; }

    public double getSimulationTime() { return simulationTime; }
    public void setSimulationTime(double simulationTime) { this.simulationTime = simulationTime; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
}