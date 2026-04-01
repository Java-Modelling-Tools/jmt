/**
 * <p>Title: XML Results Writer</p>
 * <p>Description: Refactored replacement for the legacy {@code XMLResultsWriter}.
 * Writes simulation results to XML using the two-phase Map → Serialize pipeline.</p>
 *
 * <p>Phase 1 — {@link ResultsDescriptorMapper}: {@link MeasureDefinition} → {@link ResultsDescriptor}</p>
 * <p>Phase 2 — {@link ResultsSerializer}: {@link ResultsDescriptor} → DOM {@link Document}</p>
 *
 * @author Borgna Daniele (Refactoring)
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.results;

import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.xml.XMLResultsConstants;
import jmt.gui.common.xml.results.dto.ResultsDescriptor;
import jmt.gui.common.xml.results.mapper.*;
import jmt.gui.common.xml.results.serializer.ResultsSerializer;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;

public class XMLResultsWriter implements XMLResultsConstants {

    private XMLResultsWriter() { /* utility class */ }

    // -----------------------------------------------------------------------
    // Public write methods (mirror the legacy API)
    // -----------------------------------------------------------------------

    public static void writeXML(String fileName, MeasureDefinition measure) {
        writeToResult(new StreamResult(new File(fileName)), measure);
    }

    public static void writeXML(File xmlFile, MeasureDefinition measure) {
        writeToResult(new StreamResult(xmlFile), measure);
    }

    public static void writeXML(OutputStream out, MeasureDefinition measure) {
        writeToResult(new StreamResult(out), measure);
    }

    /**
     * Builds the DOM {@link Document} from the given {@link MeasureDefinition}.
     * Called by {@link jmt.gui.common.xml.XMLArchiver} when assembling the archive root.
     */
    public static Document getDocument(MeasureDefinition measure) {
        // Phase 1: map model → DTO
        ResultsDescriptorMapper mapper = new ResultsDescriptorMapper();
        ResultsDescriptor dto = mapper.mapModelToDto(measure);

        // Phase 2: serialize DTO → DOM Document
        ResultsSerializer serializer = new ResultsSerializer();
        return serializer.serialize(dto);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static void writeToResult(Result result, MeasureDefinition measure) {
        Document doc = getDocument(measure);
        if (doc == null) return;
        try {
            Transformer transformer = createSecureTransformer();
            transformer.transform(new DOMSource(doc), result);
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            e.printStackTrace();
        }
    }

    private static Transformer createSecureTransformer() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        return transformer;
    }
}