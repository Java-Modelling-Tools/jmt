package jmt.gui.common.xml;

import jmt.gui.common.definitions.CommonModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the refactored XMLWriter pipeline.
 * Performs a round-trip: XML -> Model -> XML and compares the content.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
class XMLWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void test_xmlRoundTrip_preservesContent() throws IOException {
        // 1. Setup paths
        File originalFile = new File("src/test/resources/jmt/gui/common/xml/parseImpatience_branches.xml");
        File outputFile = tempDir.resolve("output_roundtrip.xml").toFile();

        // 2. Load the model from the original XML (Parsing + Mapping)
        CommonModel model = new CommonModel();
        XMLArchiver.loadModel(originalFile, model);

        // 3. Save the model back to XML (New Writer pipeline: Model -> DTO -> XML)
        XMLArchiver.saveModel(outputFile, model);

        // 4. Compare the files
        // Note: Using ISO-8859-1 to match the legacy JMT encoding
        String originalXml = new String(Files.readAllBytes(originalFile.toPath()), Charset.forName("ISO-8859-1"));
        String producedXml = new String(Files.readAllBytes(outputFile.toPath()), Charset.forName("ISO-8859-1"));

        // Clean up whitespace differences that might occur due to indentation settings
        assertThat(normalizeXml(producedXml))
                .as("The produced XML should be structurally identical to the original")
                .isEqualTo(normalizeXml(originalXml));
    }

    /**
     * Helper to remove platform-specific line endings and leading/trailing whitespace
     * to ensure the comparison focuses on the XML structure.
     */
    private String normalizeXml(String xml) {
        return xml.replace("\r\n", "\n")
                .replaceAll(">\\s+<", "><") // Remove indentation whitespace between tags
                .trim();
    }
}