/**
 * Recursive parity test for {@link XMLArchiver} and {@link LegacyXMLArchiver} against the
 * JSIM graph model corpus.
 *
 * <p>This test targets {@code .jsimg} archives (the native JSIM graphical format) stored in
 * {@code src/test/resources/linetesting/input/jsim/}. The corpus is discovered at runtime:
 * {@link #modelFileProvider()} lists every file that ends in {@code .jsimg} and turns each one
 * into an independent parameterised test case named after the file.
 *
 * <p>For every input file the single test method
 * {@link #newAndLegacyArchiver_readEquivalentModels_recursively(String, File)} exercises three
 * independent recursive assertions:
 * <ol>
 *   <li><b>CommonModel</b> — both models are compared field-by-field with AssertJ
 *       {@code usingRecursiveComparison()}, verifying that the simulation-model state
 *       (classes, stations, connections, measures, blocking regions, preloads, etc.) is
 *       identical between the two load paths.</li>
 *   <li><b>GUI model</b> — the full {@link jmt.gui.jsimgraph.definitions.JSimGraphModel}
 *       object graph (which extends {@code CommonModel} and additionally carries visual
 *       metadata: class colours, canvas positions, Bézier connection shapes) is compared
 *       recursively, confirming that the graphical layer is preserved faithfully.</li>
 *   <li><b>Simulation results</b> — the {@code containsSimulationResults()} flag is checked
 *       first; when both models carry embedded results the {@link jmt.gui.common.definitions.MeasureDefinition}
 *       objects are compared recursively, ignoring runtime-only log fields.</li>
 * </ol>
 *
 * <p>All three comparisons exclude fields whose names match the patterns
 * {@code .*[Ll]istener.*}, {@code .*[Pp]rogress.*}, and {@code .*[Tt]imestamp.*} (or
 * {@code .*[Ll]og.*} for the results layer). These fields carry volatile runtime state
 * (event-listener registrations, progress monitors, wall-clock timestamps) that is
 * legitimately absent or different between two independently created model instances
 * and has no bearing on semantic equivalence.
 *
 * <p>The key distinction from {@link XMLArchiverTest} is the comparison strategy:
 * {@code XMLArchiverTest} converts both models to {@link jmt.gui.common.xml.sim.dto.SimDescriptor}
 * DTOs before comparing; this class compares the live model objects directly, catching any
 * discrepancy that the DTO serialisation might silently absorb. Together the two suites
 * provide complementary coverage of the load pipeline.
 *
 * @see XMLArchiverTest
 * @see LegacyXMLArchiver
 * @author Borgna Daniele
 *         Date: 16-apr-2026
 */
package jmt.gui.common.xml;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class XMLArchiverModelRecursiveParityTest {

    private static final Path INPUT_FOLDER = Paths.get("src/test/resources/linetesting/input/jsim");
    private static final String INPUT_EXTENSION = ".jsimg";

    static Stream<Arguments> modelFileProvider() throws IOException {
        return Files.list(INPUT_FOLDER)
                .filter(p -> p.toString().endsWith(INPUT_EXTENSION))
                .sorted()
                .map(p -> Arguments.of(p.getFileName().toString(), p.toFile()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("modelFileProvider")
    @SuppressWarnings("deprecation")
    void newAndLegacyArchiver_readEquivalentModels_recursively(String fileName, File modelFile) {
        JSimGraphModel legacyModel = new JSimGraphModel(null);
        LegacyXMLArchiver.loadModel(modelFile, legacyModel);

        JSimGraphModel newModel = new JSimGraphModel(null);
        XMLArchiver.loadModel(modelFile, newModel);

        assertCommonModelRecursive(fileName, legacyModel, newModel);
        assertGuiModelRecursive(fileName, legacyModel, newModel);
        assertResultsModelRecursive(fileName, legacyModel, newModel);
    }

    private void assertCommonModelRecursive(String fileName, CommonModel legacyModel, CommonModel newModel) {
        assertThat(newModel)
                .as("[%s] CommonModel should match recursively between new and legacy archivers", fileName)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(
                        ".*[Ll]istener.*",
                        ".*[Pp]rogress.*",
                        ".*[Tt]imestamp.*")
                .isEqualTo(legacyModel);
    }

    private void assertGuiModelRecursive(String fileName, JSimGraphModel legacyModel, JSimGraphModel newModel) {
        assertThat(newModel)
                .as("[%s] GUI model state should match recursively between new and legacy archivers", fileName)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(
                        ".*[Ll]istener.*",
                        ".*[Pp]rogress.*",
                        ".*[Tt]imestamp.*")
                .isEqualTo(legacyModel);
    }

    private void assertResultsModelRecursive(String fileName, CommonModel legacyModel, CommonModel newModel) {
        assertThat(newModel.containsSimulationResults())
                .as("[%s] Results presence flag should match", fileName)
                .isEqualTo(legacyModel.containsSimulationResults());

        if (!legacyModel.containsSimulationResults()) {
            return;
        }

        MeasureDefinition legacyResults = legacyModel.getSimulationResults();
        MeasureDefinition newResults = newModel.getSimulationResults();

        assertThat(newResults)
                .as("[%s] Results model should match recursively between new and legacy archivers", fileName)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(
                        ".*[Ll]istener.*",
                        ".*[Pp]rogress.*",
                        ".*[Ll]og.*")
                .isEqualTo(legacyResults);
    }
}

