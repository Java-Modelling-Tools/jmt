/**
 * <p>Title: XML Results Reader</p>
 * <p>Description: Refactored replacement for the legacy {@code XMLResultsReader}.
 * Reads stored simulation results from XML files using the two-phase
 * Parse → Map pipeline.</p>
 *
 * <p>Phase 1 — {@link jmt.gui.common.xml.parsers.ResultsParser}: DOM → {@link ResultsDescriptor}</p>
 * <p>Phase 2 — {@link ResultsDescriptorMapper}: {@link ResultsDescriptor} → {@link MeasureDefinition}</p>
 *
 * @author Borgna Daniele (Refactoring)
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.results;

import jmt.common.xml.XSDSchemaLoader;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.definitions.PAResultsModel;
import jmt.gui.common.definitions.StoredResultsModel;
import jmt.gui.common.xml.XMLLoader;
import jmt.gui.common.xml.XMLResultsConstants;
import jmt.gui.common.xml.results.dto.ResultsDescriptor;
import jmt.gui.common.xml.results.mapper.ResultsDescriptorMapper;
import jmt.gui.common.xml.results.parsers.ResultsParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

public class XMLResultsReader implements XMLResultsConstants {

    private XMLResultsReader() { /* utility class */ }

    // -----------------------------------------------------------------------
    // Public loading methods (mirror the legacy API)
    // -----------------------------------------------------------------------

    /**
     * Loads stored GUI results ({@code Results.xsd}) into a {@link StoredResultsModel}.
     */
    public static void loadGuiModel(StoredResultsModel model, File archiveName) {
        Document doc = XMLLoader.loadXML(
                archiveName.getAbsolutePath(),
                XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_GUI_RESULTS));
        parseXML(doc, model);
    }

    /**
     * Loads stored engine results ({@code SIMmodeloutput.xsd}) into a
     * {@link StoredResultsModel}.
     */
    public static void loadModel(StoredResultsModel model, File archiveName) {
        Document doc = XMLLoader.loadXML(
                archiveName.getAbsolutePath(),
                XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_MODEL_RESULTS));
        parseXML(doc, model);
    }

    /**
     * Parses a results {@link Document} (either format) into the given
     * {@link MeasureDefinition}.
     */
    public static void parseXML(Document xml, MeasureDefinition model) {
        if (xml != null) parseXML(xml.getDocumentElement(), model);
    }

    /**
     * Parses a results root {@link Element} into the given {@link MeasureDefinition}.
     * Dispatches to the appropriate model type ({@link PAResultsModel} or
     * {@link StoredResultsModel}) exactly as the legacy code did, but through the
     * two-phase DTO pipeline.
     */
    public static void parseXML(Element root, MeasureDefinition model) {
        if (root == null || model == null) return;

        String rootTag = root.getTagName();

        // Verify the root tag matches the expected model type before parsing
        boolean isGuiFormat    = XML_DOCUMENT_ROOT.equals(rootTag);
        boolean isEngineFormat = XML_DOCUMENT_O_ROOT.equals(rootTag);

        if (!isGuiFormat && !isEngineFormat) return;

        // Phase 1: parse DOM → DTO
        ResultsParser parser = new ResultsParser();
        ResultsDescriptor dto = parser.parse(root);

        // Phase 2: map DTO → model
        ResultsDescriptorMapper mapper = new ResultsDescriptorMapper();
        mapper.mapDtoToModel(dto, model);
    }
}