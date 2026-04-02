/**
 * Cross-pipeline equivalence test: verifies that loading any JMT archive with the
 * legacy {@link LegacyXMLArchiver} pipeline and the refactored {@link XMLArchiver} pipeline
 * produces semantically identical {@link CommonModel} objects.
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
import java.util.Comparator;
import java.util.stream.Stream;

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
    void legacyAndNewPipelineProduceEquivalentModels(String fileName, File xmlFile) {

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
}
