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

