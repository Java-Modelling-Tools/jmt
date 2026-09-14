package linetesting;

import java.io.File;

import org.w3c.dom.Document;

import analytical.ExactModel;
import common.controller.ParametricAnalysisRunner;
import common.controller.TimerThread;
import common.definitions.CommonModel;
import common.definitions.PAResultsModel;
import common.definitions.StoredResultsModel;
import common.xml.XMLArchiver;
import common.xml.XMLReader;
import common.xml.XMLResultsReader;
import common.xml.XMLWriter;
import jaba.JabaModel;
import jaba.JabaResults;
import jmt.framework.xml.XMLUtils;
import jsimgraph.definitions.JSimGraphModel;
import jsimwiz.definitions.JSIMModel;
import simDispatcher.DispatcherJSIMschema;

/**
 * Solves JMT models without constructing any Swing component.
 *
 * <p>
 * The tools' entry points ({@code JSIMGraphMain}, {@code JSIMWizMain},
 * {@code JMVAWizard}, {@code JabaWizard}) are JFrames, and the harness used to
 * build one per test purely to reach the model and the dispatcher behind it via
 * reflection. Everything underneath those frames is plain computation, so this
 * class drives it directly: the readers and writers in {@code common.xml}, the
 * simulation engine in {@code simDispatcher}, and the analytical solvers.
 * </p>
 *
 * <p>
 * The output artefacts are written in exactly the format the GUI produced -
 * {@code XMLArchiver} for JSIM models, {@code createDocument()} for JMVA and
 * JABA - so the stored oracles and the {@code jsim-diff-check} scripts keep
 * working unchanged.
 * </p>
 */
public final class HeadlessSolver {

	private static final XMLUtils XML_UTILS = new XMLUtils();

	private HeadlessSolver() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	// --- JSIM (JSIMgraph and JSIMwiz) -----------------------------------------

	/**
	 * Solves a JSIMgraph model and writes the solved model to outputFile.
	 *
	 * <p>
	 * A {@link JSimGraphModel} is used rather than a plain {@code CommonModel}
	 * because {@code GuiXMLReader} and {@code GuiXMLWriter} both guard their handling
	 * of station positions and connection shapes with an {@code instanceof} check on
	 * it - a plain model would silently drop the layout from the saved file.
	 * </p>
	 *
	 * <p>
	 * Its Mediator is null: the only members that dereference it are
	 * {@code setStationIcon}, {@code setConnectionEnd} and {@code refreshGraph},
	 * none of which the reader, the writer or the engine ever calls.
	 * </p>
	 */
	public static void solveJSimGraph(File testModel, File outputFile) throws Exception {
		// Mediator.openModel does not call manageJobs, so neither do we - it reallocates
		// preloaded jobs for closed classes, which would change what is simulated.
		solveJSim(new JSimGraphModel(null), testModel, outputFile, false);
	}

	/**
	 * Solves a JSIMwiz model and writes the solved model to outputFile.
	 */
	public static void solveJSimWiz(File testModel, File outputFile) throws Exception {
		// JSIMWizMain corrects job preloading after loading; JSIMgraph does not.
		solveJSim(new JSIMModel(), testModel, outputFile, true);
	}

	private static void solveJSim(CommonModel model, File testModel, File outputFile, boolean manageJobs)
			throws Exception {
		XMLArchiver.loadModel(testModel, model);

		// The archive carries the stored (oracle) results. Drop them, so that the file
		// we write back holds only what this run produced - otherwise a simulation that
		// yielded nothing would save the oracle straight back and the test would pass
		// without having computed anything.
		model.setSimulationResults(null);

		if (manageJobs) {
			// Corrects eventual problems on preloading for closed classes
			model.manageJobs();
		}

		if (model.isParametricAnalysisEnabled()) {
			PAResultsModel results = new ParametricAnalysisRunner(model, null).run();
			if (results != null) {
				model.setSimulationResults(results);
			}
		} else {
			StoredResultsModel results = runSingleSimulation(model);
			if (results != null) {
				model.setSimulationResults(results);
			}
		}

		XMLArchiver.saveModel(outputFile, model);
	}

	/**
	 * Runs one simulation of the model and parses the engine's result file.
	 *
	 * <p>
	 * This is the headless counterpart of {@code DispatcherThread}: that class runs
	 * the same dispatcher but polls it on a background thread to keep a live results
	 * table updated, which is pointless with nobody watching. Reading the result file
	 * once the run has finished yields the same measures.
	 * </p>
	 */
	private static StoredResultsModel runSingleSimulation(CommonModel model) throws Exception {
		File simulationFile = File.createTempFile("~JModelSimulation", ".xml");
		simulationFile.deleteOnExit();
		XMLWriter.writeXML(simulationFile, model);

		DispatcherJSIMschema simulator = new DispatcherJSIMschema(simulationFile);
		if (!model.getUseRandomSeed().booleanValue()) {
			simulator.setSimulationSeed(model.getSimulationSeed().longValue());
		}

		// The engine's own max-duration handling is not relied upon here, for the same
		// reason PADispatcherThread avoids it: a TimerThread stops the run predictably.
		TimerThread timer = null;
		if (model.getMaximumDuration().doubleValue() > 0) {
			timer = new TimerThread(simulator, model.getMaximumDuration().doubleValue());
			timer.start();
		}

		try {
			simulator.solveModel();
		} catch (OutOfMemoryError err) {
			simulator.abortAllMeasures();
			simulator.killSimulation();
			simulator.forceFinished();
			throw new RuntimeException("Out of memory while simulating", err);
		} catch (Exception ex) {
			simulator.abortAllMeasures();
			simulator.killSimulation();
			simulator.forceFinished();
			throw ex;
		} finally {
			if (timer != null) {
				timer.kill();
			}
		}

		File output = simulator.getSimulation().getOutputFile();
		if (output == null) {
			return null;
		}
		output.deleteOnExit();

		Document doc = XMLReader.loadXML(output.getAbsolutePath());
		StoredResultsModel results = new StoredResultsModel();
		XMLResultsReader.parseXML(doc, results);
		return results;
	}

	// --- JMVA -----------------------------------------------------------------

	/**
	 * Solves a JMVA model and writes the solved model to outputFile.
	 *
	 * <p>
	 * Mirrors {@code SolverClient.solve()} without its progress window: the model is
	 * written to a temp file, solved in place by the analytical dispatcher, then read
	 * back.
	 * </p>
	 */
	public static void solveJMVA(File testModel, File outputFile) throws Exception {
		ExactModel model = new ExactModel();
		if (!model.loadDocument(XML_UTILS.loadXML(testModel))) {
			throw new RuntimeException("Could not load JMVA model: " + testModel.getName());
		}

		ExactModel solved = new ExactModel(model);
		solved.resetResults();

		File temp = File.createTempFile("~jmt_headlessSolver", ".xml");
		temp.deleteOnExit();
		if (!XML_UTILS.saveXML(solved.createDocument(), temp)) {
			throw new RuntimeException("Could not write JMVA model to temp file");
		}

		new solvers.dispatchers.SolverDispatcher().solve(temp);

		if (!solved.loadDocument(XML_UTILS.loadXML(temp))) {
			throw new RuntimeException("Could not read back solved JMVA model");
		}

		if (!XML_UTILS.saveXML(solved.createDocument(), outputFile)) {
			throw new RuntimeException("Could not write solved JMVA model to " + outputFile);
		}
	}

	// --- JABA -----------------------------------------------------------------

	/**
	 * Solves a JABA model and returns its results.
	 *
	 * <p>
	 * {@code JabaWizard.solve()} is a thin wrapper over this: it copies the model,
	 * hands it to the dispatcher, and spends the rest of its body updating tabs.
	 * </p>
	 */
	public static JabaResults solveJABA(File testModel) throws Exception {
		JabaModel model = new JabaModel();
		if (!model.loadDocument(XML_UTILS.loadXML(testModel))) {
			throw new RuntimeException("Could not load JABA model: " + testModel.getName());
		}

		JabaModel solved = new JabaModel(model);
		solved.setResults(new jaba.link.SolverDispatcher().solve(solved));
		return solved.getResults();
	}

}
