package jmt.gui.common.xml;

import jmt.gui.common.xml.gui.dto.GuiDescriptor;
import jmt.gui.common.xml.gui.mapper.GuiDescriptorMapper;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration round-trip tests for the refactored {@link jmt.gui.common.xml.gui.GuiXMLWriter}.
 *
 * <h3>Strategy</h3>
 * Each test loads an XML archive into a {@link JSimGraphModel}, extracts the expected
 * {@link GuiDescriptor} DTO via {@link GuiDescriptorMapper}, saves the model with
 * {@link XMLArchiver#saveModel} (which invokes the new {@code GuiXMLWriter} pipeline),
 * reloads the saved file, and compares the round-tripped DTO against the expected one
 * using AssertJ's deep recursive comparison.
 *
 * <p>No {@code equals()} implementation is required on the DTO classes because AssertJ's
 * {@code usingRecursiveComparison()} compares all fields by reflection.
 */
class GuiXMLWriterTest {

    @TempDir
    Path tempDir;

    // -----------------------------------------------------------------------
    // Round-trip tests — one per file that contains a <jmodel> section
    // -----------------------------------------------------------------------

    @Test
    void test_225_conBeta13fesc() {
        assertRoundTrip("225_conBeta13fesc.xml", "out_225.xml");
    }

    @Test
    void test_520_Sec5_3_FastTask_Quorum1_Wif_JSIMg() {
        assertRoundTrip("520_Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml", "out_520.xml");
    }

    @Test
    void test_641_2FJ_2tasks_nestedOK() {
        assertRoundTrip("641_2FJ_2tasks_nestedOK.xml", "out_641.xml");
    }

    @Test
    void test_792_Workcopy3_onoff() {
        assertRoundTrip("792_Workcopy3_onoff.xml", "out_792.xml");
    }

    @Test
    void test_4039_240415_FCFS_QD() {
        assertRoundTrip("4039_240415_FCFS_QD.xml", "out_4039.xml");
    }

    @Test
    void test_10089_ForkJoin_4Stat_2_SEED1() {
        assertRoundTrip("10089_ForkJoin_4Stat_2 - SEED1.xml", "out_10089.xml");
    }

    @Test
    void test_10190_220423_2RotNogen_2Q() {
        assertRoundTrip("10190_220423_2RotNogen_2Q.xml", "out_10190.xml");
    }

    @Test
    void test_23985_SjAS04Model_6AS_L5() {
        assertRoundTrip("23985_SjAS04Model_6AS-L5.xml", "out_23985.xml");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Executes a full jmodel round-trip for a single archive file:
     * <ol>
     *   <li>Load original model → map to expected {@link GuiDescriptor} DTO</li>
     *   <li>Save model with {@link XMLArchiver#saveModel} (uses the new {@code GuiXMLWriter})</li>
     *   <li>Reload saved file → map to actual {@link GuiDescriptor} DTO</li>
     *   <li>Assert deep equality</li>
     * </ol>
     */
    private void assertRoundTrip(String fileName, String outputName) {
        File xmlFile    = new File("src/test/resources/jmt/gui/common/xml/" + fileName);
        File outputFile = tempDir.resolve(outputName).toFile();

        // 1. Load original model
        JSimGraphModel originalModel = new JSimGraphModel(null);
        XMLArchiver.loadModel(xmlFile, originalModel);

        // 2. Extract expected DTO
        GuiDescriptorMapper mapper = new GuiDescriptorMapper();
        GuiDescriptor expectedDto  = mapper.mapModelToDto(originalModel);

        // 3. Save model — GuiXMLWriter.getDocument() is invoked internally
        XMLArchiver.saveModel(outputFile, originalModel);

        // 4. Reload round-tripped model
        JSimGraphModel roundTrippedModel = new JSimGraphModel(null);
        XMLArchiver.loadModel(outputFile, roundTrippedModel);

        // 5. Extract actual DTO
        GuiDescriptor actualDto = mapper.mapModelToDto(roundTrippedModel);

        // 6. Deep-compare — no equals() needed: AssertJ traverses fields recursively
        assertThat(actualDto)
                .as("Round-tripped jmodel DTO must match original for: " + fileName)
                // No equals() was needed on any DTO — AssertJ's usingRecursiveComparison() handles full field-by-field traversal including nested lists and maps.
                .usingRecursiveComparison()
                .isEqualTo(expectedDto);
    }
}
