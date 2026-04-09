/**
 * Cross-pipeline equivalence test: verifies that loading any JMT archive with the
 * legacy {@link LegacyXMLArchiver} pipeline and the refactored {@link XMLArchiver} pipeline
 * produces semantically identical {@link CommonModel} objects. Also validates the round-trip
 * process (load → save → load) for both pipelines.
 *
 * <p>Three aspects are checked for every input file in
 * {@code src/test/resources/jmt/gui/common/xml/}:
 * <ol>
 *   <li><b>Sim model</b> — recursive field-by-field comparison of the {@link SimDescriptor} DTOs
 *       produced by {@link SimDescriptorMapper} (same technique used in {@link XMLSimWriterTest}).</li>
 *   <li><b>GUI metadata</b> — recursive comparison of the {@link GuiDescriptor} DTOs produced by
 *       {@link GuiDescriptorMapper} (parametric-analysis config; positional/colour data only for
 *       {@link jmt.gui.jsimgraph.definitions.JSimGraphModel} instances).</li>
 *   <li><b>Simulation results</b> — presence check; when both models carry embedded results,
 *       a recursive field comparison is performed on the
 *       {@link jmt.gui.common.definitions.SimulationResultsModel}.</li>
 * </ol>
 *
 * <p>Round-trip tests verify that each pipeline can save and reload its own output without loss:
 * <ul>
 *   <li>Legacy pipeline: load → save → load → compare (must be identical)</li>
 *   <li>New pipeline: load → save → load → compare (must be identical)</li>
 * </ul>
 *
 * @author Borgna Daniele
 *         Date: 02-apr-2026
 */
package jmt.gui.common.xml;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.gui.dto.GuiDescriptor;
import jmt.gui.common.xml.gui.mapper.GuiDescriptorMapper;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.dto.SimDescriptor;
import jmt.gui.common.xml.sim.mapper.SimDescriptorMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.assertj.core.api.Assertions.assertThat;

class XMLArchiverTest {

    private static final Path RESOURCES_DIR =
            Paths.get("src/test/resources/jmt/gui/common/xml");

    // -------------------------------------------------------------------------
    // Test-case provider: one Arguments per .xml file, sorted for stable order
    // -------------------------------------------------------------------------

    static Stream<Arguments> xmlFileProvider() throws IOException {
        return Files.list(RESOURCES_DIR)
                .filter(p -> p.toString().endsWith(".xml"))
                .sorted()
                .map(p -> Arguments.of(p.getFileName().toString(), p.toFile()));
    }

    // -------------------------------------------------------------------------
    // Parameterised test — one case per file
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("xmlFileProvider")
    @SuppressWarnings("deprecation")
    void legacyAndNewPipelineLoadXmlEquivalently(String fileName, File xmlFile) {

        // ── Load with legacy pipeline ────────────────────────────────────────
        CommonModel legacyModel = new CommonModel();
        LegacyXMLArchiver.loadModel(xmlFile, legacyModel);

        // ── Load with new pipeline ────────────────────────────────────────────
        CommonModel newModel = new CommonModel();
        XMLArchiver.loadModel(xmlFile, newModel);

        // ── 1. Sim model (SimDescriptor recursive comparison) ─────────────────
        assertSimModelsEqual(legacyModel, newModel, fileName);

        // ── 2. GUI metadata (GuiDescriptor recursive comparison) ──────────────
        assertGuiDataEqual(legacyModel, newModel, fileName);

        // ── 3. Simulation results ─────────────────────────────────────────────
        assertResultsEqual(legacyModel, newModel, fileName);
    }

    // -------------------------------------------------------------------------
    // Writing phase tests: verify CommonModel -> XML preservation
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("xmlFileProvider")
    @SuppressWarnings("deprecation")
    void legacyPipelineSaveAndLoadPreservesModel(String fileName, File xmlFile) throws IOException {

        // ── Load with legacy pipeline ────────────────────────────────────────
        CommonModel originalModel = new CommonModel();
        LegacyXMLArchiver.loadModel(xmlFile, originalModel);

        // ── Save and reload ──────────────────────────────────────────────────
        File tempFile = File.createTempFile("legacy-roundtrip-", ".xml");
        try {
            LegacyXMLArchiver.saveModel(tempFile, originalModel);
            CommonModel reloadedModel = new CommonModel();
            LegacyXMLArchiver.loadModel(tempFile, reloadedModel);

            // ── Verify round-trip preserves model identity ────────────────────
            assertSimModelsEqual(originalModel, reloadedModel, fileName + " (legacy round-trip)");
            assertGuiDataEqual(originalModel, reloadedModel, fileName + " (legacy round-trip)");
            assertResultsEqual(originalModel, reloadedModel, fileName + " (legacy round-trip)");
        } finally {
            assertTrue(tempFile.delete(), "Failed to clean up temporary file: " + tempFile);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("xmlFileProvider")
    void newPipelineSaveAndLoadPreservesModel(String fileName, File xmlFile) throws IOException {

        // ── Load with new pipeline ───────────────────────────────────────────
        CommonModel originalModel = new CommonModel();
        XMLArchiver.loadModel(xmlFile, originalModel);

        // ── Save and reload ──────────────────────────────────────────────────
        File tempFile = File.createTempFile("new-roundtrip-", ".xml");
        try {
            XMLArchiver.saveModel(tempFile, originalModel);
            CommonModel reloadedModel = new CommonModel();
            XMLArchiver.loadModel(tempFile, reloadedModel);

            // ── Verify round-trip preserves model identity ────────────────────
            assertSimModelsEqual(originalModel, reloadedModel, fileName + " (new round-trip)");
            assertGuiDataEqual(originalModel, reloadedModel, fileName + " (new round-trip)");
            assertResultsEqual(originalModel, reloadedModel, fileName + " (new round-trip)");
        } finally {
            assertTrue(tempFile.delete(), "Failed to clean up temporary file: " + tempFile);
        }
    }

    // -------------------------------------------------------------------------
    // XML content comparison tests: detect structural changes in serialization
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("xmlFileProvider")
    void legacyPipelineSavePreservesXmlStructure(String fileName, File xmlFile) throws IOException {

        // ── Load and save with legacy pipeline ───────────────────────────────
        CommonModel originalModel = new CommonModel();
        LegacyXMLArchiver.loadModel(xmlFile, originalModel);

        File tempFile = File.createTempFile("legacy-xml-compare-", ".xml");
        try {
            LegacyXMLArchiver.saveModel(tempFile, originalModel);

            // ── Compare original and saved XML content ────────────────────────
            assertXmlContentPreserved(xmlFile, tempFile, fileName, "legacy");
        } finally {
            assertTrue(tempFile.delete(), "Failed to clean up temporary file: " + tempFile);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("xmlFileProvider")
    void newPipelineSavePreservesXmlStructure(String fileName, File xmlFile) throws IOException {

        // ── Load and save with new pipeline ──────────────────────────────────
        CommonModel originalModel = new CommonModel();
        XMLArchiver.loadModel(xmlFile, originalModel);

        File tempFile = File.createTempFile("new-xml-compare-", ".xml");
        try {
            XMLArchiver.saveModel(tempFile, originalModel);

            // ── Compare original and saved XML content ────────────────────────
            assertXmlContentPreserved(xmlFile, tempFile, fileName, "new");
        } finally {
            assertTrue(tempFile.delete(), "Failed to clean up temporary file: " + tempFile);
        }
    }

    // -------------------------------------------------------------------------
    // Refactoring delta test: compare legacy vs new pipeline serialization
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("xmlFileProvider")
    @SuppressWarnings("deprecation")
    void refactoringChangesXmlSerialization(String fileName, File xmlFile) throws IOException {

        // ── Load with both pipelines ─────────────────────────────────────────
        CommonModel legacyModel = new CommonModel();
        LegacyXMLArchiver.loadModel(xmlFile, legacyModel);

        CommonModel newModel = new CommonModel();
        XMLArchiver.loadModel(xmlFile, newModel);

        // ── Save with both pipelines ─────────────────────────────────────────
        File legacyFile = File.createTempFile("legacy-refactor-compare-", ".xml");
        File newFile = File.createTempFile("new-refactor-compare-", ".xml");
        try {
            LegacyXMLArchiver.saveModel(legacyFile, legacyModel);
            XMLArchiver.saveModel(newFile, newModel);

            // ── Compare what changed between legacy and new serialization ─────
            assertXmlChangesDueToRefactoring(legacyFile, newFile, fileName);
        } finally {
            assertTrue(legacyFile.delete(), "Failed to clean up temporary file: " + legacyFile);
            assertTrue(newFile.delete(), "Failed to clean up temporary file: " + newFile);
        }
    }

    // -------------------------------------------------------------------------
    // Comparison helpers
    // -------------------------------------------------------------------------

    /**
     * Maps both models to {@link SimDescriptor} DTOs and compares them recursively,
     * using the same zero-strategy alignment as {@link XMLSimWriterTest}.
     */
    private void assertSimModelsEqual(CommonModel legacy, CommonModel newM, String file) {
        SimDescriptor legacyDto = new SimDescriptorMapper(legacy).mapToDto();
        SimDescriptor newDto    = new SimDescriptorMapper(newM).mapToDto();

        // Align "ghost" zero/disabled strategies: wherever the new pipeline has
        // emitted a Zero/DisabledServiceTimeStrategy, force the legacy DTO to match
        // so that orphaned exponential sub-parameters don't cause spurious diffs.
        alignZeroStrategies(legacyDto, newDto);

        assertThat(newDto)
                .as("[%s] Sim model must be identical between legacy and new pipeline", file)
                .usingRecursiveComparison()
                .ignoringFields(
                        "archiveName",
                        "globalAttributes.timestamp",
                        "globalAttributes.name")
                .withComparatorForType(trailingSlashComparator(), String.class)
                .isEqualTo(legacyDto);
    }

    /**
     * Maps both models to {@link GuiDescriptor} DTOs (parametric-analysis config
     * for plain {@link CommonModel}; additionally positions/colours for
     * {@link jmt.gui.jsimgraph.definitions.JSimGraphModel}) and compares them
     * recursively.
     */
    private void assertGuiDataEqual(CommonModel legacy, CommonModel newM, String file) {
        GuiDescriptorMapper mapper  = new GuiDescriptorMapper();
        GuiDescriptor legacyGui = mapper.mapModelToDto(legacy);
        GuiDescriptor newGui    = mapper.mapModelToDto(newM);

        assertThat(newGui)
                .as("[%s] GUI metadata must be identical between legacy and new pipeline", file)
                .usingRecursiveComparison()
                .isEqualTo(legacyGui);
    }

    /**
     * Checks that both models agree on whether simulation results are present.
     * When both carry results, performs a recursive field comparison on the
     * {@link jmt.gui.common.definitions.SimulationResultsModel}, ignoring
     * runtime-only listener/progress hooks.
     */
    private void assertResultsEqual(CommonModel legacy, CommonModel newM, String file) {
        assertThat(newM.containsSimulationResults())
                .as("[%s] Results-presence flag must match between pipelines", file)
                .isEqualTo(legacy.containsSimulationResults());

        if (!legacy.containsSimulationResults()) {
            return;
        }

        assertThat(newM.getSimulationResults())
                .as("[%s] Simulation results must be identical between legacy and new pipeline", file)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(
                        ".*[Ll]istener.*",
                        ".*[Pp]rogress.*",
                        ".*[Ll]og.*")
                .isEqualTo(legacy.getSimulationResults());
    }

    // -------------------------------------------------------------------------
    // ZeroStrategy alignment (mirrors XMLSimWriterTest)
    // -------------------------------------------------------------------------

    /**
     * Walks both DTO trees in parallel. Wherever the new pipeline has emitted a
     * {@code ZeroServiceTimeStrategy} or {@code DisabledServiceTimeStrategy},
     * forces the legacy DTO node to match, removing orphaned sub-parameters.
     */
    private void alignZeroStrategies(SimDescriptor legacy, SimDescriptor newM) {
        if (legacy.getNodes() == null || newM.getNodes() == null) return;
        for (int i = 0; i < legacy.getNodes().size(); i++) {
            if (i >= newM.getNodes().size()) break;
            NodeDescriptor legacyNode = legacy.getNodes().get(i);
            NodeDescriptor newNode    = newM.getNodes().get(i);

            if (legacyNode.getSections() == null || newNode.getSections() == null) continue;
            for (int j = 0; j < legacyNode.getSections().size(); j++) {
                if (j >= newNode.getSections().size()) break;
                SectionDescriptor legacySec = legacyNode.getSections().get(j);
                SectionDescriptor newSec    = newNode.getSections().get(j);

                if (legacySec.getParameters() == null || newSec.getParameters() == null) continue;
                for (int k = 0; k < legacySec.getParameters().size(); k++) {
                    if (k >= newSec.getParameters().size()) break;
                    alignParams(legacySec.getParameters().get(k), newSec.getParameters().get(k));
                }
            }
        }
    }

    private void alignParams(ParameterDescriptor legacy, ParameterDescriptor newP) {
        if (legacy == null || newP == null) return;

        boolean newIsZero = newP.getClassPath() != null
                && (newP.getClassPath().contains("ZeroServiceTimeStrategy")
                    || newP.getClassPath().contains("DisabledServiceTimeStrategy"));

        if (newIsZero) {
            legacy.setName(newP.getName());
            legacy.setClassPath(newP.getClassPath());
            legacy.setValue(newP.getValue());
            if (legacy.getSubParameters() != null) {
                legacy.getSubParameters().clear();
            }
            return;
        }

        if (legacy.getSubParameters() != null && newP.getSubParameters() != null) {
            for (int i = 0; i < legacy.getSubParameters().size(); i++) {
                if (i >= newP.getSubParameters().size()) break;
                alignParams(legacy.getSubParameters().get(i), newP.getSubParameters().get(i));
            }
        }
    }

    // -------------------------------------------------------------------------
    // String comparator (ignores trailing slashes on path-like values)
    // -------------------------------------------------------------------------

    private Comparator<String> trailingSlashComparator() {
        return (s1, s2) -> {
            if (s1 == null && s2 == null) return 0;
            if (s1 == null || s2 == null) return -1;
            return s1.replaceAll("/$", "").compareTo(s2.replaceAll("/$", ""));
        };
    }

    // -------------------------------------------------------------------------
    // XML content comparison (detects structural changes in serialization)
    // -------------------------------------------------------------------------

    /**
     * Compares original and saved XML files line by line, normalizing dynamic
     * attributes (timestamp, archive name) that change on each save. Produces
     * a unified diff output on mismatch to show structural changes.
     */
    private void assertXmlContentPreserved(File original, File saved, String fileName, String pipelineType)
            throws IOException {
        List<String> originalLines = normalizeXmlLines(Files.readAllLines(original.toPath()));
        List<String> savedLines = normalizeXmlLines(Files.readAllLines(saved.toPath()));

        if (originalLines.equals(savedLines)) {
            return; // XML structure preserved
        }

        // Produce unified diff output showing what changed
        StringBuilder diff = new StringBuilder();
        diff.append("\n[").append(fileName).append(" (").append(pipelineType).append(" pipeline)]")
            .append(" XML structure differs:\n\n");

        int maxLines = Math.max(originalLines.size(), savedLines.size());
        int contextBefore = 2;
        int lastDiffLine = -10;

        for (int i = 0; i < maxLines; i++) {
            String origLine = i < originalLines.size() ? originalLines.get(i) : null;
            String savedLine = i < savedLines.size() ? savedLines.get(i) : null;

            boolean linesDiffer = !java.util.Objects.equals(origLine, savedLine);

            if (linesDiffer) {
                // Show context before first difference in a range
                if (i - lastDiffLine > contextBefore + 1) {
                    for (int ctx = Math.max(0, i - contextBefore); ctx < i; ctx++) {
                        if (ctx >= originalLines.size() && ctx >= savedLines.size()) break;
                        String ctxLine = ctx < originalLines.size() ? originalLines.get(ctx) : null;
                        diff.append(" ").append(ctxLine).append("\n");
                    }
                }

                // Show the difference
                if (origLine != null) {
                    diff.append("-").append(origLine).append("\n");
                }
                if (savedLine != null) {
                    diff.append("+").append(savedLine).append("\n");
                }
                lastDiffLine = i;
            }
        }

        // Assert with the diff output
        assertThat(savedLines)
            .as(diff.toString())
            .isEqualTo(originalLines);
    }

    /**
     * Compares XML produced by legacy pipeline vs new (refactored) pipeline.
     * This isolates the changes introduced by the refactoring itself, ignoring
     * common serialization behavior that both pipelines share.
     */
    private void assertXmlChangesDueToRefactoring(File legacyXml, File newXml, String fileName)
            throws IOException {
        List<String> legacyLines = normalizeXmlLines(Files.readAllLines(legacyXml.toPath()));
        List<String> newLines = normalizeXmlLines(Files.readAllLines(newXml.toPath()));

        if (legacyLines.equals(newLines)) {
            return; // Both pipelines produce identical XML (no refactoring changes)
        }

        // Produce unified diff showing what the refactoring changed
        StringBuilder diff = new StringBuilder();
        diff.append("\n[").append(fileName).append("] Refactoring changes to XML serialization:\n\n");

        int maxLines = Math.max(legacyLines.size(), newLines.size());
        int contextLines = 1;
        int lastDiffLine = -10;

        for (int i = 0; i < maxLines; i++) {
            String legacyLine = i < legacyLines.size() ? legacyLines.get(i) : null;
            String newLine = i < newLines.size() ? newLines.get(i) : null;

            boolean linesDiffer = !java.util.Objects.equals(legacyLine, newLine);

            if (linesDiffer) {
                // Show context lines before first difference in a range
                if (i - lastDiffLine > contextLines + 1) {
                    for (int ctx = Math.max(0, i - contextLines); ctx < i; ctx++) {
                        String ctxLine = ctx < legacyLines.size() ? legacyLines.get(ctx) : null;
                        if (ctxLine != null) {
                            diff.append(" ").append(ctxLine).append("\n");
                        }
                    }
                }

                // Show the differences: legacy (old) vs new (refactored)
                if (legacyLine != null) {
                    diff.append("-").append(legacyLine).append("\n");
                }
                if (newLine != null) {
                    diff.append("+").append(newLine).append("\n");
                }
                lastDiffLine = i;
            }
        }

        // Assert with the diff output
        assertThat(newLines)
            .as(diff.toString())
            .isEqualTo(legacyLines);
    }

    /**
     * Normalizes XML lines by:
     * - Trimming leading whitespace (for indentation differences)
     * - Removing/masking dynamic attributes (timestamp, paths, filenames)
     * - Normalizing path separators across platforms
     */
    private List<String> normalizeXmlLines(List<String> lines) {
        List<String> normalized = new ArrayList<>();
        for (String line : lines) {
            // Trim leading whitespace to ignore indentation differences (Java 8 compatible)
            line = line.replaceAll("^\\s+", "");

            // Normalize all name attributes (filenames can vary)
            // Match: name="<anything>.jsimg" or name="<anything>.xml"
            line = line.replaceAll("\\sname=\"[^\"]+\\.(jsimg|xml)\"", " name=\"NORMALIZED\"");

            // Remove timestamp attribute (varies on each save)
            line = line.replaceAll("\\stimestamp=\"[^\"]*\"", " timestamp=\"NORMALIZED\"");

            // Normalize file paths (Windows backslash → Unix forward slash)
            line = line.replaceAll("\\\\", "/");

            // Normalize absolute paths that include username/home directory
            // (e.g., C:/Users/alice/JMT/ vs /home/bob/JMT/)
            line = line.replaceAll("logPath=\"[^\"]*JMT/\"", "logPath=\"/normalized/JMT/\"");

            normalized.add(line);
        }
        return normalized;
    }
}
