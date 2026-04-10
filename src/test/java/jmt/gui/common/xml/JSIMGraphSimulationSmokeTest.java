package jmt.gui.common.xml;

import jmt.engine.log.LoggerParameters;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.mainGui.JSIMGraphMain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GUI smoke test: for each XML model in the same folder used by XMLArchiverTest,
 * open the model in JSIMGraph and start the simulation as the Play action does.
 */
class JSIMGraphSimulationSmokeTest {

//    private static final Path RESOURCES_DIR = Paths.get("src/test/resources/jmt/gui/common/xml");
    private static final Path RESOURCES_DIR = Paths.get("src/test/resources/linetesting/input/jsim");
    private static final String RESOURCES_EXTENSION = ".jsimg";
    private static final long THREAD_INIT_TIMEOUT_MS = 10_000;
    private static final long SIMULATION_TIMEOUT_MS = 120_000;
    private static final long RESULTS_READY_TIMEOUT_MS = 15_000;
    private static final long RESULTS_POLL_MS = 50;
    private static final long MAX_GUI_MODEL_BYTES = 8L * 1024L * 1024L;

    static Stream<Arguments> xmlFileProvider() throws IOException {
        try (Stream<Path> paths = Files.list(RESOURCES_DIR)) {
            List<Path> files = paths
                    .filter(p -> p.toString().endsWith(RESOURCES_EXTENSION))
                    .sorted()
                    .collect(Collectors.toList());
            return files.stream().map(p -> Arguments.of(p.getFileName().toString(), p.toFile()));
        }
    }

    @AfterEach
    void cleanupWindows() {
        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                window.dispose();
            }
        }
    }

    @Tag("gui-integration")
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("xmlFileProvider")
    void openModelAndStartSimulationLikePlayAction(String fileName, File xmlFile) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "GUI test skipped in headless environment");
        Assumptions.assumeTrue(xmlFile.length() <= MAX_GUI_MODEL_BYTES,
                "GUI smoke test skipped for very large model: " + fileName + " (" + xmlFile.length() + " bytes)");

        File simulationInput = prepareSimulationInput(xmlFile);

        AtomicReference<JSIMGraphMain> mainRef = new AtomicReference<>();
        Mediator mediator = null;
        try {
            runOnEdt(() -> mainRef.set(new JSIMGraphMain()));
            JSIMGraphMain mainWindow = mainRef.get();
            mediator = getMediator(mainWindow);
            final Mediator mediatorRef = mediator;

            runOnEdt(() -> mediatorRef.openModel(simulationInput));
            // Avoid heavy repaint cycles during long simulations in smoke mode.
            runOnEdt(() -> {
                mainWindow.removeGraph();
                mainWindow.setVisible(false);
            });
            CommonModel model = getModel(mediatorRef);

            // Avoid confirmation dialogs that would block unattended GUI tests.
            model.setSimulationResults(null);
            model.setLoggingGlbParameter("autoAppend", String.valueOf(LoggerParameters.LOGGER_AR_REPLACE));

            boolean isParametric = model.isParametricAnalysisEnabled();

            runOnEdt(mediatorRef::startSimulation);

            Thread simulationThread;
            if (isParametric) {
                simulationThread = (Thread) waitForFieldInitialization(mediatorRef, "batchThread", "batchThread");
            } else {
                simulationThread = (Thread) waitForFieldInitialization(mediatorRef, "dispatcher", "dispatcherThread");
            }

            awaitSimulationEnd(simulationThread, fileName);

            MeasureDefinition results = awaitResultsReady(model, fileName);
            int measureCount = results.getMeasureNumber();
            assertThat(measureCount)
                    .as("Simulation should expose at least one measure for %s", fileName)
                    .isGreaterThan(0);
            int analyzedSamples = totalAnalyzedSamples(results, measureCount);
            if (analyzedSamples == 0) {
                assertThat(hasNoInProgressMeasures(results, measureCount))
                        .as("Simulation ended with zero analyzed samples, but measures are still in progress for %s", fileName)
                        .isTrue();
            } else {
                assertThat(analyzedSamples)
                        .as("Simulation should analyze at least one sample for %s", fileName)
                        .isGreaterThan(0);
            }
        } finally {
            if (mediator != null) {
                Mediator mediatorRef = mediator;
                runOnEdt(() -> {
                    mediatorRef.stopSimulation();
                    if (mediatorRef.getResultsWindow() != null) {
                        mediatorRef.getResultsWindow().dispose();
                    }
                    if (mediatorRef.getPAProgressWindow() != null) {
                        mediatorRef.getPAProgressWindow().stopAnimation();
                        mediatorRef.getPAProgressWindow().dispose();
                    }
                });
            }
            if (mainRef.get() != null) {
                JSIMGraphMain mainWindow = mainRef.get();
                runOnEdt(mainWindow::dispose);
            }
            if (!xmlFile.equals(simulationInput)) {
                Files.deleteIfExists(simulationInput.toPath());
            }
        }
    }

    private File prepareSimulationInput(File xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            Element root = doc.getDocumentElement();
            if (root == null || !"archive".equals(root.getTagName())) {
                return xmlFile;
            }

            NodeList resultsNodes = root.getElementsByTagName("results");
            if (resultsNodes.getLength() == 0) {
                return xmlFile;
            }

            List<Node> toRemove = new ArrayList<>();
            for (int i = 0; i < resultsNodes.getLength(); i++) {
                toRemove.add(resultsNodes.item(i));
            }
            for (Node node : toRemove) {
                root.removeChild(node);
            }

            File temp = File.createTempFile("jsimg-smoke-no-results-", ".xml");
            temp.deleteOnExit();

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(temp));
            return temp;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to prepare simulation input for " + xmlFile.getName(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare simulation input for " + xmlFile.getName(), e);
        }
    }

    private Mediator getMediator(JSIMGraphMain mainWindow) throws ReflectiveOperationException {
        Field mediatorField = JSIMGraphMain.class.getDeclaredField("mediator");
        mediatorField.setAccessible(true);
        return (Mediator) mediatorField.get(mainWindow);
    }

    private CommonModel getModel(Mediator mediator) throws ReflectiveOperationException {
        Field modelField = Mediator.class.getDeclaredField("model");
        modelField.setAccessible(true);
        return (CommonModel) modelField.get(mediator);
    }

    private Object waitForFieldInitialization(Object target, String fieldName, String displayName)
            throws ReflectiveOperationException, InterruptedException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        long start = System.currentTimeMillis();
        Object value;
        do {
            value = field.get(target);
            if (value != null) {
                return value;
            }
            Thread.sleep(50);
        } while (System.currentTimeMillis() - start < THREAD_INIT_TIMEOUT_MS);

        throw new IllegalStateException("Simulation aborted: " + displayName + " was not initialized");
    }

    private void awaitSimulationEnd(Thread simulationThread, String fileName) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (simulationThread.getState() == Thread.State.NEW
                && System.currentTimeMillis() - start < THREAD_INIT_TIMEOUT_MS) {
            Thread.sleep(20);
        }

        long remaining = SIMULATION_TIMEOUT_MS - (System.currentTimeMillis() - start);
        if (remaining <= 0) {
            throw new IllegalStateException("Timeout while waiting simulation start for " + fileName);
        }

        simulationThread.join(remaining);
        if (simulationThread.isAlive()) {
            throw new IllegalStateException("Simulation timeout for " + fileName);
        }
    }

    private MeasureDefinition awaitResultsReady(CommonModel model, String fileName) throws InterruptedException {
        long start = System.currentTimeMillis();
        boolean containsResults = false;
        String resultsClass = "null";
        Integer measureCount = null;
        while (System.currentTimeMillis() - start < RESULTS_READY_TIMEOUT_MS) {
            containsResults = model.containsSimulationResults();
            MeasureDefinition results = model.getSimulationResults();
            if (results != null) {
                resultsClass = results.getClass().getName();
                try {
                    measureCount = results.getMeasureNumber();
                    if (measureCount != null && measureCount > 0) {
                        return results;
                    }
                } catch (NullPointerException ignored) {
                    // ResultsModel can be visible before internal measures initialization.
                }
            }
            Thread.sleep(RESULTS_POLL_MS);
        }

        throw new IllegalStateException("Results not ready for " + fileName
                + " after " + RESULTS_READY_TIMEOUT_MS + "ms"
                + " (containsResults=" + containsResults
                + ", resultsClass=" + resultsClass
                + ", measureCount=" + measureCount + ")");
    }

    private int totalAnalyzedSamples(MeasureDefinition results, int measureCount) {
        int total = 0;
        for (int i = 0; i < measureCount; i++) {
            total += Math.max(0, results.getAnalyzedSamples(i));
        }
        return total;
    }

    private boolean hasNoInProgressMeasures(MeasureDefinition results, int measureCount) {
        for (int i = 0; i < measureCount; i++) {
            if (results.getMeasureState(i) == MeasureDefinition.MEASURE_IN_PROGRESS) {
                return false;
            }
        }
        return true;
    }

    private void runOnEdt(ThrowingRunnable task) throws InvocationTargetException, InterruptedException {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                task.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                task.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}


