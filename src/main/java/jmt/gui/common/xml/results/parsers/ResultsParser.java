/**
 * Parser for simulation results XML documents. Handles both formats:
 * <ul>
 *   <li>{@code Results.xsd} — produced by {@link jmt.gui.common.xml.results.XMLResultsWriter},
 *       covering both normal and parametric analysis results.</li>
 *   <li>{@code SIMmodeloutput.xsd} — produced directly by the simulation engine.</li>
 * </ul>
 * Implements the two-phase pattern: this class only converts DOM elements to
 * {@link ResultsDescriptor} DTOs and has no knowledge of any results model class.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.results.parsers;

import jmt.gui.common.xml.XMLResultsConstants;
import jmt.gui.common.xml.XmlEntityParser;
import jmt.gui.common.xml.results.dto.ResultMeasureDescriptor;
import jmt.gui.common.xml.results.dto.ResultsDescriptor;
import jmt.gui.common.xml.results.dto.SampleDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ResultsParser implements XmlEntityParser<ResultsDescriptor>, XMLResultsConstants {

    /**
     * Parses the root element of a results document into a {@link ResultsDescriptor}.
     * The root tag is inspected to determine which format is being parsed:
     * {@code <results>} (GUI format) or {@code <solutions>} (engine format).
     */
    @Override
    public ResultsDescriptor parse(Element root) {
        ResultsDescriptor descriptor = new ResultsDescriptor();
        String rootTag = root.getTagName();

        if (XML_DOCUMENT_ROOT.equals(rootTag)) {
            parseGuiResults(root, descriptor);
        } else if (XML_DOCUMENT_O_ROOT.equals(rootTag)) {
            parseEngineResults(root, descriptor);
        }

        return descriptor;
    }

    // -----------------------------------------------------------------------
    // GUI-format results  (<results> root)
    // -----------------------------------------------------------------------

    private void parseGuiResults(Element root, ResultsDescriptor descriptor) {
        // Global attributes
        String pollingStr = root.getAttribute(XML_A_ROOT_POLLING);
        if (pollingStr != null && !pollingStr.isEmpty()) {
            descriptor.setPollingInterval(Double.parseDouble(pollingStr));
        }
        String elapsedStr = root.getAttribute(XML_A_ROOT_ELAPSED);
        descriptor.setElapsedTime((elapsedStr == null || elapsedStr.isEmpty()) ? 0L : Long.parseLong(elapsedStr));

        String decSep = root.getAttribute(XML_A_ROOT_LOG_DECIMAL_SEP);
        descriptor.setLogDecimalSeparator((decSep == null || decSep.isEmpty()) ? "." : decSep);

        String delim = root.getAttribute(XML_A_ROOT_LOG_DELIMITER);
        descriptor.setLogCsvDelimiter((delim == null || delim.isEmpty()) ? ";" : delim);

        String logPath = root.getAttribute(XML_A_ROOT_LOG_PATH);
        if (logPath != null && !logPath.isEmpty()) {
            descriptor.setLogPath(logPath);
        }

        // Detect parametric vs normal by checking for <sample validity="...">
        boolean parametric = isParametricFormat(root);
        descriptor.setParametric(parametric);

        NodeList measures = root.getElementsByTagName(XML_E_MEASURE);
        for (int i = 0; i < measures.getLength(); i++) {
            Element el = (Element) measures.item(i);
            descriptor.addMeasure(parametric
                    ? parseParametricMeasure(el)
                    : parseNormalMeasure(el));
        }
    }

    /**
     * Heuristic: a results file is in parametric format when at least one
     * {@code <sample>} element has a {@code validity} attribute.
     */
    private boolean isParametricFormat(Element root) {
        NodeList samples = root.getElementsByTagName(XML_E_SAMPLE);
        for (int i = 0; i < samples.getLength(); i++) {
            if (((Element) samples.item(i)).hasAttribute(XML_A_SAMPLE_VALIDITY)) {
                return true;
            }
        }
        return false;
    }

    private ResultMeasureDescriptor parseNormalMeasure(Element el) {
        ResultMeasureDescriptor m = new ResultMeasureDescriptor();
        m.setName(el.getAttribute(XML_A_MEASURE_NAME));
        m.setType(deriveMeasureType(m.getName(), el.getAttribute(XML_A_MEASURE_TYPE), false));
        m.setStationName(el.getAttribute(XML_A_MEASURE_STATION));
        m.setClassName(el.getAttribute(XML_A_MEASURE_CLASS));
        m.setNodeType(el.getAttribute(XML_A_MEASURE_NODETYPE));
        m.setAlpha(parseDouble(el.getAttribute(XML_A_MEASURE_ALPHA), 0.0));
        m.setPrecision(parseDouble(el.getAttribute(XML_A_MEASURE_PRECISION), 0.0));
        m.setAnalyzedSamples(parseInt(el.getAttribute(XML_A_MEASURE_SAMPLES), 0));
        m.setDiscardedSamples(parseInt(el.getAttribute(XML_A_MEASURE_DISCARDEDSAMPLES), 0));
        m.setState(parseInt(el.getAttribute(XML_A_MEASURE_STATE), 0));
        m.setFinalValue(parseDouble(el.getAttribute(XML_A_MEASURE_FINALVALUE), 0.0));
        m.setParametric(false);

        String logFile = el.getAttribute(XML_A_MEASURE_LOG);
        if (logFile != null && !logFile.isEmpty()) m.setLogFileName(logFile);

        NodeList samples = el.getElementsByTagName(XML_E_SAMPLE);
        for (int i = 0; i < samples.getLength(); i++) {
            m.addSample(parseNormalSample((Element) samples.item(i)));
        }
        return m;
    }

    private ResultMeasureDescriptor parseParametricMeasure(Element el) {
        ResultMeasureDescriptor m = new ResultMeasureDescriptor();
        m.setName(el.getAttribute(XML_A_MEASURE_NAME));
        m.setType(deriveMeasureType(m.getName(), el.getAttribute(XML_A_MEASURE_TYPE), false));
        m.setStationName(el.getAttribute(XML_A_MEASURE_STATION));
        m.setClassName(el.getAttribute(XML_A_MEASURE_CLASS));
        m.setNodeType(el.getAttribute(XML_A_MEASURE_NODETYPE));
        m.setAlpha(parseDouble(el.getAttribute(XML_A_MEASURE_ALPHA), 0.0));
        m.setPrecision(parseDouble(el.getAttribute(XML_A_MEASURE_PRECISION), 0.0));
        m.setAnalyzedSamples(parseInt(el.getAttribute(XML_A_MEASURE_SAMPLES), 0));
        m.setParametric(true);

        String logFile = el.getAttribute(XML_A_MEASURE_LOG);
        if (logFile != null && !logFile.isEmpty()) m.setLogFileName(logFile);

        NodeList samples = el.getElementsByTagName(XML_E_SAMPLE);
        for (int i = 0; i < samples.getLength(); i++) {
            m.addSample(parseParametricSample((Element) samples.item(i)));
        }
        return m;
    }

    private SampleDescriptor parseNormalSample(Element el) {
        SampleDescriptor s = new SampleDescriptor();
        s.setMean(parseDouble(el.getAttribute(XML_A_SAMPLE_MEAN), 0.0));
        s.setLastIntervalAvgValue(parseDouble(el.getAttribute(XML_A_LAST_INTERVAL_AVG_VALUE), 0.0));
        s.setSimulationTime(parseDouble(el.getAttribute(XML_A_TIME), 0.0));

        String ub = el.getAttribute(XML_A_SAMPLE_UPPERBOUND);
        if (ub != null && !ub.isEmpty()) s.setUpperBound(parseDouble(ub, 0.0));

        String lb = el.getAttribute(XML_A_SAMPLE_LOWERBOUND);
        if (lb != null && !lb.isEmpty()) s.setLowerBound(parseDouble(lb, 0.0));

        return s;
    }

    private SampleDescriptor parseParametricSample(Element el) {
        SampleDescriptor s = new SampleDescriptor();
        s.setMean(parseDouble(el.getAttribute(XML_A_SAMPLE_MEAN), 0.0));
        s.setValid(Boolean.parseBoolean(el.getAttribute(XML_A_SAMPLE_VALIDITY)));

        if (s.isValid()) {
            s.setUpperBound(parseDouble(el.getAttribute(XML_A_SAMPLE_UPPERBOUND), 0.0));
            s.setLowerBound(parseDouble(el.getAttribute(XML_A_SAMPLE_LOWERBOUND), 0.0));
        } else {
            s.setUpperBound(parseInfinityAware(el.getAttribute(XML_A_SAMPLE_UPPERBOUND)));
            s.setLowerBound(parseInfinityAware(el.getAttribute(XML_A_SAMPLE_LOWERBOUND)));
        }
        return s;
    }

    // -----------------------------------------------------------------------
    // Engine-format results  (<solutions> root — SIMmodeloutput.xsd)
    // -----------------------------------------------------------------------

    private void parseEngineResults(Element root, ResultsDescriptor descriptor) {
        descriptor.setParametric(false);

        String decSep = root.getAttribute(XML_A_ROOT_LOG_DECIMAL_SEP);
        descriptor.setLogDecimalSeparator((decSep == null || decSep.isEmpty()) ? "." : decSep);

        String delim = root.getAttribute(XML_A_ROOT_LOG_DELIMITER);
        descriptor.setLogCsvDelimiter((delim == null || delim.isEmpty()) ? ";" : delim);

        NodeList measures = root.getElementsByTagName(XML_EO_MEASURE);
        for (int i = 0; i < measures.getLength(); i++) {
            descriptor.addMeasure(parseEngineMeasure((Element) measures.item(i)));
        }
    }

    private ResultMeasureDescriptor parseEngineMeasure(Element el) {
        ResultMeasureDescriptor m = new ResultMeasureDescriptor();
        m.setParametric(false);

        String stationName = el.getAttribute(XML_AO_MEASURE_STATION);
        m.setStationName(stationName.isEmpty() ? "Network" : stationName);

        String type = el.getAttribute(XML_AO_MEASURE_TYPE);
        m.setType(type);

        String className = el.getAttribute(XML_AO_MEASURE_CLASS);
        if (className.isEmpty()) {
            className = "FX".equals(type) ? "All modes" : "All classes";
        }
        m.setClassName(className);

        m.setNodeType(el.getAttribute(XML_AO_MEASURE_NODETYPE));
        m.setAlpha(1.0 - parseDouble(el.getAttribute(XML_AO_MEASURE_ALPHA), 0.0)); // engine stores complement
        m.setPrecision(parseDouble(el.getAttribute(XML_AO_MEASURE_PRECISION), 0.0));
        m.setAnalyzedSamples(parseInt(el.getAttribute(XML_AO_MEASURE_SAMPLES), 0));
        m.setDiscardedSamples(parseInt(el.getAttribute(XML_AO_MEASURE_DISCARDEDSAMPLES), 0));

        boolean successful = el.getAttribute(XML_AO_MEASURE_SUCCESSFUL).equalsIgnoreCase("true");
        int samples = m.getAnalyzedSamples();
        if (samples == 0) {
            m.setState(jmt.gui.common.definitions.MeasureDefinition.MEASURE_NO_SAMPLES);
        } else if (successful) {
            m.setState(jmt.gui.common.definitions.MeasureDefinition.MEASURE_SUCCESS);
        } else {
            m.setState(jmt.gui.common.definitions.MeasureDefinition.MEASURE_FAILED);
        }

        // Build the unique composite name used by the results model
        String serverType = el.getAttribute(XML_AO_MEASURE_SERVER_TYPE);
        String effectiveStation = (serverType != null && !serverType.isEmpty()) ? serverType : m.getStationName();
        m.setName(effectiveStation + "_" + m.getClassName() + "_" + type);

        String logFile = el.getAttribute(XML_A_MEASURE_LOG);
        if (logFile != null && !logFile.isEmpty()) m.setLogFileName(logFile);

        // Engine format has a single aggregate sample per measure
        SampleDescriptor s = new SampleDescriptor();
        s.setMean(parseDouble(el.getAttribute(XML_AO_MEASURE_MEAN), 0.0));
        s.setUpperBound(parseDouble(el.getAttribute(XML_AO_MEASURE_UPPER), 0.0));
        s.setLowerBound(parseDouble(el.getAttribute(XML_AO_MEASURE_LOWER), 0.0));
        m.addSample(s);

        return m;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Derives the measure-type string that {@link jmt.gui.common.definitions.MeasureDefinition}
     * expects as its {@code type} argument.
     *
     * <p>The {@code type} attribute on {@code <measure>} elements in {@code Results.xsd} is a
     * numeric code (e.g. {@code "2"}, {@code "8"}) used exclusively by the <em>writer</em>
     * ({@link jmt.gui.common.xml.results.serializer.ResultsSerializer}) via
     * {@link jmt.engine.simEngine.EngineUtils#decodeMeasureType}.
     * {@code MeasureDefinition.getMeasureType()} stores and returns the <em>textual</em> type
     * derived from the measure name suffix (e.g. {@code "Response Time"},
     * {@code "System Response Time"}), which is what the GUI rendering layer uses.
     * This method therefore always uses the name suffix and ignores the numeric attribute.
     *
     * <p>For engine-format ({@code SIMmodeloutput.xsd}) results the {@code measureType}
     * attribute already carries the textual string, so it is used directly.
     *
     * @param measureName the full measure name (e.g. {@code "Queue 4_All classes_Response Time"})
     * @param typeAttr    the raw value of the {@code type} attribute (may be numeric or textual)
     * @param engineFormat {@code true} when parsing a {@code <solutions>} engine-output document
     * @return the textual type string expected by {@code MeasureDefinition}
     */
    private String deriveMeasureType(String measureName, String typeAttr, boolean engineFormat) {
        // Engine format: the measureType attribute is already the textual type string
        if (engineFormat && typeAttr != null && !typeAttr.isEmpty()) return typeAttr;
        // GUI format (Results.xsd): always derive from name suffix regardless of type attr
        if (measureName == null) return null;
        int idx = measureName.lastIndexOf('_');
        return (idx >= 0) ? measureName.substring(idx + 1) : measureName;
    }

    private double parseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return defaultValue; }
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return defaultValue; }
    }

    private double parseInfinityAware(String value) {
        if ("Infinity".equals(value)) return Double.POSITIVE_INFINITY;
        return parseDouble(value, 0.0);
    }
}