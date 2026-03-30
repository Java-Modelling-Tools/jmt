/**
 * Root Data Transfer Object for a simulation results document ({@code Results.xsd}).
 * Holds global metadata (polling interval, elapsed time, log formatting) and an
 * ordered list of {@link ResultMeasureDescriptor} objects.
 *
 * This DTO is populated by {@link jmt.gui.common.xml.parsers.ResultsParser} during
 * the reading phase and consumed by
 * {@link jmt.gui.common.xml.serializer.ResultsSerializer} during the writing phase.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.results.dto;

import java.util.ArrayList;
import java.util.List;

public class ResultsDescriptor {

    private double pollingInterval;
    private long elapsedTime;
    private String logDecimalSeparator = ".";
    private String logCsvDelimiter     = ";";
    private String logPath;

    /** True when all measures in this descriptor come from a parametric analysis. */
    private boolean parametric;

    private final List<ResultMeasureDescriptor> measures = new ArrayList<>();

    // --- Getters & Setters ---

    public double getPollingInterval() { return pollingInterval; }
    public void setPollingInterval(double pollingInterval) { this.pollingInterval = pollingInterval; }

    public long getElapsedTime() { return elapsedTime; }
    public void setElapsedTime(long elapsedTime) { this.elapsedTime = elapsedTime; }

    public String getLogDecimalSeparator() { return logDecimalSeparator; }
    public void setLogDecimalSeparator(String logDecimalSeparator) { this.logDecimalSeparator = logDecimalSeparator; }

    public String getLogCsvDelimiter() { return logCsvDelimiter; }
    public void setLogCsvDelimiter(String logCsvDelimiter) { this.logCsvDelimiter = logCsvDelimiter; }

    public String getLogPath() { return logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }

    public boolean isParametric() { return parametric; }
    public void setParametric(boolean parametric) { this.parametric = parametric; }

    public List<ResultMeasureDescriptor> getMeasures() { return measures; }

    public void addMeasure(ResultMeasureDescriptor measure) {
        if (measure != null) measures.add(measure);
    }
}