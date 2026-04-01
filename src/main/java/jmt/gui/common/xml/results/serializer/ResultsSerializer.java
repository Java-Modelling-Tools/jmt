/**
 * Serializes a {@link ResultsDescriptor} DTO into a DOM {@link Document} conforming
 * to {@code Results.xsd}. This class is the symmetric counterpart of
 * {@link jmt.gui.common.xml.parsers.ResultsParser} and has no knowledge of the live
 * results model classes.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.results.serializer;

import jmt.gui.common.xml.XMLResultsConstants;
import jmt.gui.common.xml.results.dto.ResultMeasureDescriptor;
import jmt.gui.common.xml.results.dto.ResultsDescriptor;
import jmt.gui.common.xml.results.dto.SampleDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ResultsSerializer implements XMLResultsConstants {

    /**
     * Serializes the given {@link ResultsDescriptor} into a fresh DOM {@link Document}.
     *
     * @param dto the fully-populated results descriptor
     * @return a DOM document ready to be piped through a {@link javax.xml.transform.Transformer}
     */
    public Document serialize(ResultsDescriptor dto) {
        Document doc = createDocument();
        Element root = doc.createElement(XML_DOCUMENT_ROOT);
        doc.appendChild(root);

        root.setAttribute("xsi:noNamespaceSchemaLocation", XML_DOCUMENT_XSD);
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute(XML_A_ROOT_POLLING, Double.toString(dto.getPollingInterval()));
        root.setAttribute(XML_A_ROOT_ELAPSED, Long.toString(dto.getElapsedTime()));
        root.setAttribute(XML_A_ROOT_LOG_DECIMAL_SEP, dto.getLogDecimalSeparator());
        root.setAttribute(XML_A_ROOT_LOG_DELIMITER, dto.getLogCsvDelimiter());
        if (dto.getLogPath() != null) {
            root.setAttribute(XML_A_ROOT_LOG_PATH, dto.getLogPath());
        }

        for (ResultMeasureDescriptor m : dto.getMeasures()) {
            root.appendChild(dto.isParametric()
                    ? serializeParametricMeasure(doc, m)
                    : serializeNormalMeasure(doc, m));
        }

        return doc;
    }

    // -----------------------------------------------------------------------
    // Normal measure
    // -----------------------------------------------------------------------

    private Element serializeNormalMeasure(Document doc, ResultMeasureDescriptor m) {
        Element el = doc.createElement(XML_E_MEASURE);
        el.setAttribute(XML_A_MEASURE_NAME,    m.getName());
        el.setAttribute(XML_A_MEASURE_TYPE,    m.getType());
        el.setAttribute(XML_A_MEASURE_CLASS,   m.getClassName());
        el.setAttribute(XML_A_MEASURE_STATION, m.getStationName());
        el.setAttribute(XML_A_MEASURE_FINALVALUE,          Double.toString(m.getFinalValue()));
        el.setAttribute(XML_A_MEASURE_SAMPLES,             Integer.toString(m.getAnalyzedSamples()));
        el.setAttribute(XML_A_MEASURE_STATE,               Integer.toString(m.getState()));
        el.setAttribute(XML_A_MEASURE_ALPHA,               Double.toString(m.getAlpha()));
        el.setAttribute(XML_A_MEASURE_PRECISION,           Double.toString(m.getPrecision()));
        el.setAttribute(XML_A_MEASURE_NODETYPE,            m.getNodeType());
        el.setAttribute(XML_A_MEASURE_DISCARDEDSAMPLES,    Integer.toString(m.getDiscardedSamples()));
        if (m.getLogFileName() != null) {
            el.setAttribute(XML_A_MEASURE_LOG, m.getLogFileName());
        }

        for (SampleDescriptor s : m.getSamples()) {
            el.appendChild(serializeNormalSample(doc, s));
        }
        return el;
    }

    private Element serializeNormalSample(Document doc, SampleDescriptor s) {
        Element el = doc.createElement(XML_E_SAMPLE);
        el.setAttribute(XML_A_SAMPLE_MEAN,              Double.toString(s.getMean()));
        el.setAttribute(XML_A_LAST_INTERVAL_AVG_VALUE,  Double.toString(s.getLastIntervalAvgValue()));
        el.setAttribute(XML_A_TIME,                     Double.toString(s.getSimulationTime()));
        // Lower/upper are omitted when zero or infinite (legacy writer behavior)
        if (s.getLowerBound() > 0 && !Double.isInfinite(s.getLowerBound())) {
            el.setAttribute(XML_A_SAMPLE_LOWERBOUND, Double.toString(s.getLowerBound()));
        }
        if (s.getUpperBound() > 0 && !Double.isInfinite(s.getUpperBound())) {
            el.setAttribute(XML_A_SAMPLE_UPPERBOUND, Double.toString(s.getUpperBound()));
        }
        return el;
    }

    // -----------------------------------------------------------------------
    // Parametric measure
    // -----------------------------------------------------------------------

    private Element serializeParametricMeasure(Document doc, ResultMeasureDescriptor m) {
        Element el = doc.createElement(XML_E_MEASURE);
        el.setAttribute(XML_A_MEASURE_NAME,    m.getName());
        el.setAttribute(XML_A_MEASURE_TYPE,    m.getType());
        el.setAttribute(XML_A_MEASURE_CLASS,   m.getClassName());
        el.setAttribute(XML_A_MEASURE_STATION, m.getStationName());
        el.setAttribute(XML_A_MEASURE_SAMPLES, Integer.toString(m.getAnalyzedSamples()));
        el.setAttribute(XML_A_MEASURE_ALPHA,   Double.toString(m.getAlpha()));
        el.setAttribute(XML_A_MEASURE_PRECISION, Double.toString(m.getPrecision()));
        el.setAttribute(XML_A_MEASURE_NODETYPE,  m.getNodeType());
        if (m.getLogFileName() != null) {
            el.setAttribute(XML_A_MEASURE_LOG, m.getLogFileName());
        }

        for (SampleDescriptor s : m.getSamples()) {
            el.appendChild(serializeParametricSample(doc, s));
        }
        return el;
    }

    private Element serializeParametricSample(Document doc, SampleDescriptor s) {
        Element el = doc.createElement(XML_E_SAMPLE);
        el.setAttribute(XML_A_SAMPLE_MEAN,       Double.toString(s.getMean()));
        el.setAttribute(XML_A_SAMPLE_LOWERBOUND, Double.toString(s.getLowerBound()));
        el.setAttribute(XML_A_SAMPLE_UPPERBOUND, Double.toString(s.getUpperBound()));
        el.setAttribute(XML_A_SAMPLE_VALIDITY,   Boolean.toString(s.isValid()));
        return el;
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private Document createDocument() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to create XML Document for results", e);
        }
    }
}