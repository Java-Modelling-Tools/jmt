/**
 * Data Transfer Object representing a single performance measure record
 * within a simulation results file ({@code Results.xsd} / {@code SIMmodeloutput.xsd}).
 * It holds all measure-level metadata and the ordered list of {@link SampleDescriptor}
 * objects that belong to it.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.results.dto;

import java.util.ArrayList;
import java.util.List;

public class ResultMeasureDescriptor {

    private String name;
    private String type;
    private String stationName;
    private String className;
    private String nodeType;
    private String logFileName;

    private double alpha;
    private double precision;
    private double finalValue;
    private double pollingInterval;

    private int analyzedSamples;
    private int discardedSamples;
    private int state;

    /** True when this measure belongs to a parametric analysis result set. */
    private boolean parametric;

    private List<SampleDescriptor> samples = new ArrayList<>();

    // --- Scalar getters / setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getLogFileName() { return logFileName; }
    public void setLogFileName(String logFileName) { this.logFileName = logFileName; }

    public double getAlpha() { return alpha; }
    public void setAlpha(double alpha) { this.alpha = alpha; }

    public double getPrecision() { return precision; }
    public void setPrecision(double precision) { this.precision = precision; }

    public double getFinalValue() { return finalValue; }
    public void setFinalValue(double finalValue) { this.finalValue = finalValue; }

    public double getPollingInterval() { return pollingInterval; }
    public void setPollingInterval(double pollingInterval) { this.pollingInterval = pollingInterval; }

    public int getAnalyzedSamples() { return analyzedSamples; }
    public void setAnalyzedSamples(int analyzedSamples) { this.analyzedSamples = analyzedSamples; }

    public int getDiscardedSamples() { return discardedSamples; }
    public void setDiscardedSamples(int discardedSamples) { this.discardedSamples = discardedSamples; }

    public int getState() { return state; }
    public void setState(int state) { this.state = state; }

    public boolean isParametric() { return parametric; }
    public void setParametric(boolean parametric) { this.parametric = parametric; }

    // --- Sample list ---

    public List<SampleDescriptor> getSamples() { return samples; }

    public void addSample(SampleDescriptor sample) {
        if (sample != null) samples.add(sample);
    }
}