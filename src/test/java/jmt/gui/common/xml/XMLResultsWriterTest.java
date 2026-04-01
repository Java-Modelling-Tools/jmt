/**
 * Integration round-trip tests for the refactored {@link jmt.gui.common.xml.results.XMLResultsWriter}.
 * Verifies that results data (standard and parametric) survives a full write-then-reload cycle
 * with no semantic loss, using AssertJ's deep recursive comparison on the DTO layer.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.PAResultsModel;
import jmt.gui.common.definitions.StoredResultsModel;
import jmt.gui.common.xml.results.XMLResultsReader;
import jmt.gui.common.xml.results.XMLResultsWriter;
import jmt.gui.common.xml.results.dto.ResultsDescriptor;
import jmt.gui.common.xml.results.mapper.ResultsDescriptorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class XMLResultsWriterTest {

    @TempDir
    Path tempDir;

    // -----------------------------------------------------------------------
    // Round-trip tests — standard (StoredResultsModel) results
    // -----------------------------------------------------------------------

    @Test
    void test_225_conBeta13fesc() {
        assertRoundTripStored("225_conBeta13fesc.xml", "out_225.xml");
    }

    @Test
    void test_4039_240415_FCFS_QD() {
        assertRoundTripStored("4039_240415_FCFS_QD.xml", "out_4039.xml");
    }

    @Test
    void test_641_2FJ_2tasks_nestedOK() {
        assertRoundTripStored("641_2FJ_2tasks_nestedOK.xml", "out_641.xml");
    }

    @Test
    void test_792_Workcopy3_onoff() {
        assertRoundTripStored("792_Workcopy3_onoff.xml", "out_792.xml");
    }

    @Test
    void test_10089_ForkJoin_4Stat_2_SEED1() {
        assertRoundTripStored("10089_ForkJoin_4Stat_2 - SEED1.xml", "out_10089.xml");
    }

    @Test
    void test_10190_220423_2RotNogen_2Q() {
        assertRoundTripStored("10190_220423_2RotNogen_2Q.xml", "out_10190.xml");
    }

    // -----------------------------------------------------------------------
    // Round-trip test — parametric analysis (PAResultsModel) results
    // -----------------------------------------------------------------------

    @Test
    void test_520_Sec5_3_FastTask_Quorum1_Wif_JSIMg_PA() {
        assertRoundTripPA("520_Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml", "out_520.xml");
    }

    // -----------------------------------------------------------------------
    // Infrastructure
    // -----------------------------------------------------------------------

    private Document parseDocument(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private Element getResultsElement(Document doc) {
        Element el = (Element) doc.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_ROOT).item(0);
        if (el != null) return el;
        el = (Element) doc.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_O_ROOT).item(0);
        if (el != null) return el;
        throw new IllegalArgumentException("No <results> or <solutions> element found in document");
    }

    // -----------------------------------------------------------------------
    // Round-trip helpers
    // -----------------------------------------------------------------------

    /**
     * Full results round-trip for a standard (non-parametric) simulation file:
     * <ol>
     *   <li>Parse results from archive → populate {@link StoredResultsModel} → map to expected DTO</li>
     *   <li>Write results with {@link XMLResultsWriter#writeXML}</li>
     *   <li>Reload from written file → populate new {@link StoredResultsModel} → map to actual DTO</li>
     *   <li>Assert deep equality</li>
     * </ol>
     */
    private void assertRoundTripStored(String fileName, String outputName) {
        try {
            File xmlFile    = new File("src/test/resources/jmt/gui/common/xml/" + fileName);
            File outputFile = tempDir.resolve(outputName).toFile();

            // 1. Parse original results
            Document originalDoc = parseDocument(xmlFile);
            StoredResultsModel originalModel = new StoredResultsModel();
            XMLResultsReader.parseXML(getResultsElement(originalDoc), originalModel);

            // 2. Expected DTO
            ResultsDescriptorMapper mapper = new ResultsDescriptorMapper();
            ResultsDescriptor expectedDto  = mapper.mapModelToDto(originalModel);

            // 3. Write results — ResultsSerializer is invoked internally
            XMLResultsWriter.writeXML(outputFile, originalModel);

            // 4. Reload round-tripped results
            Document roundTrippedDoc = parseDocument(outputFile);
            StoredResultsModel roundTrippedModel = new StoredResultsModel();
            XMLResultsReader.parseXML(getResultsElement(roundTrippedDoc), roundTrippedModel);

            // 5. Actual DTO
            ResultsDescriptor actualDto = mapper.mapModelToDto(roundTrippedModel);

            // 6. Deep-compare
            assertThat(actualDto)
                    .as("Round-tripped results DTO must match original for: " + fileName)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedDto);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Full results round-trip for a parametric analysis file:
     * <ol>
     *   <li>Load archive into {@link CommonModel} to obtain PA metadata, then parse
     *       results into a {@link PAResultsModel}</li>
     *   <li>Map to expected DTO</li>
     *   <li>Write with {@link XMLResultsWriter#writeXML}</li>
     *   <li>Reload into a fresh {@link PAResultsModel} backed by the same archive</li>
     *   <li>Assert deep equality</li>
     * </ol>
     */
    private void assertRoundTripPA(String fileName, String outputName) {
        try {
            File xmlFile    = new File("src/test/resources/jmt/gui/common/xml/" + fileName);
            File outputFile = tempDir.resolve(outputName).toFile();

            // 1. Load archive to build PAResultsModel
            CommonModel simModel = new CommonModel();
            XMLArchiver.loadModel(xmlFile, simModel);

            Document originalDoc = parseDocument(xmlFile);
            PAResultsModel originalModel = new PAResultsModel(simModel, true);
            XMLResultsReader.parseXML(getResultsElement(originalDoc), originalModel);

            // 2. Expected DTO
            ResultsDescriptorMapper mapper = new ResultsDescriptorMapper();
            ResultsDescriptor expectedDto  = mapper.mapModelToDto(originalModel);

            // 3. Write results
            XMLResultsWriter.writeXML(outputFile, originalModel);

            // 4. Reload round-tripped results (reuse same simModel for PA structure)
            Document roundTrippedDoc = parseDocument(outputFile);
            PAResultsModel roundTrippedModel = new PAResultsModel(simModel, true);
            XMLResultsReader.parseXML(getResultsElement(roundTrippedDoc), roundTrippedModel);

            // 5. Actual DTO
            ResultsDescriptor actualDto = mapper.mapModelToDto(roundTrippedModel);

            // 6. Deep-compare
            assertThat(actualDto)
                    .as("Round-tripped PA results DTO must match original for: " + fileName)
                    // No equals() was needed on any DTO — AssertJ's usingRecursiveComparison() handles full field-by-field traversal including nested lists and maps.
                    .usingRecursiveComparison()
                    .isEqualTo(expectedDto);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
