/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package common.controller;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.w3c.dom.Document;

import common.definitions.AbortMeasure;
import common.definitions.CommonModel;
import common.definitions.MeasureDefinition;
import common.definitions.PAResultsModel;
import common.definitions.SimulationDefinition;
import common.definitions.StoredResultsModel;
import common.definitions.parametric.ParametricAnalysisDefinition;
import common.xml.XMLReader;
import common.xml.XMLResultsReader;
import common.xml.XMLWriter;
import jmt.framework.gui.graph.MeasureValue;
import simDispatcher.DispatcherJSIMschema;

/**
 * <p>
 * Title: ParametricAnalysisRunner
 * </p>
 * <p>
 * Description: Runs every step of a What-If (parametric analysis) simulation and
 * aggregates the per-step results into a single {@link PAResultsModel}.
 * </p>
 * <p>
 * This class contains no Swing code and shows nothing on screen, so a What-If
 * analysis can be driven from a headless context - the command line, a test
 * harness, or a CI job. {@link PADispatcherThread} wraps it to supply the
 * progress window and the results window; everything those two share lives here,
 * so a fix to the step loop reaches both callers.
 * </p>
 *
 * @see PADispatcherThread
 */
public class ParametricAnalysisRunner implements AbortMeasure {

	/**
	 * Notified as the run progresses. Every method is optional - pass a null
	 * listener to {@link ParametricAnalysisRunner} for a completely silent run.
	 */
	public interface Listener {

		/** Called once per completed step, from the thread that ran that step. */
		void stepCompleted();

		/** Called when a step exhausts the heap. The run continues with the other steps. */
		void outOfMemory(String message);

		/** Called when a step fails for any other reason. The run continues with the other steps. */
		void exception(Exception e);

	}

	private final SimulationDefinition simd;
	private final Listener listener;

	// Written by the step threads, read by the control methods (pause/stop/abort),
	// so it must be a concurrent map rather than a plain HashMap.
	private final Map<Integer, PASimulation> activeSimulations = new ConcurrentHashMap<>();

	private ExecutorService exec;
	private StoredResultsModel[] simulationResults;
	private CountDownLatch resultsLatch;
	private CountDownLatch setResultsLatch;
	private volatile boolean stopped;

	/**
	 * @param simd the simulation definition carrying the parametric analysis model
	 * @param listener progress callback, or null for a silent run
	 */
	public ParametricAnalysisRunner(SimulationDefinition simd, Listener listener) {
		this.simd = simd;
		this.listener = listener;
	}

	/**
	 * Runs every step of the parametric analysis and blocks until all of them have
	 * finished. The model's original values are restored before returning.
	 *
	 * @return the aggregated results, or null if no step produced any measure
	 */
	public PAResultsModel run() {
		ParametricAnalysisDefinition pad = simd.getParametricAnalysisModel();
		pad.createValuesSet();
		int steps = pad.getNumberOfSteps();
		int parallelism = simd.getWhatIfParallelism();

		resultsLatch = new CountDownLatch(steps);
		setResultsLatch = new CountDownLatch(1);
		PAResultsModel parametricAnalysisResultsModel = new PAResultsModel((CommonModel) simd);
		exec = Executors.newFixedThreadPool(parallelism);
		simulationResults = new StoredResultsModel[steps];

		for (int currentStep = 0; currentStep < steps; currentStep++) {
			pad.changeModel(currentStep);
			File simulationFile = null;
			try {
				simulationFile = File.createTempFile("~JModelSimulation-" + currentStep, ".xml");
				simulationFile.deleteOnExit();
			} catch (IOException e) {
				e.printStackTrace();
			}
			XMLWriter.writeXML(simulationFile, (CommonModel) simd);

			DispatcherJSIMschema simulator = new DispatcherJSIMschema(simulationFile);
			simulator.setParametricStep(currentStep);
			// Propagate the fixed seed to each PA step so the run is reproducible.
			// Every step uses the same seed: a per-step offset (seed + currentStep) gave some steps
			// seeds under which the simulation converges very slowly (e.g. 11 min vs 41 min on Obj3).
			if (!simd.getUseRandomSeed().booleanValue()) {
				simulator.setSimulationSeed(simd.getSimulationSeed().longValue());
			}
			SimSolver ss = new SimSolver(simulator, currentStep, resultsLatch);
			if (simd.getMaximumDuration().doubleValue() > 0) {
				ss.setTimer(new TimerThread(simulator, simd.getMaximumDuration().doubleValue()));
			}

			exec.execute(ss);
		}

		try {
			resultsLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		boolean hasResults = false;
		for (StoredResultsModel results : simulationResults) {
			if (results == null) {
				continue;
			}
			for (int i = 0; i < results.getMeasureNumber(); i++) {
				hasResults = true;
				Vector<MeasureValue> thisResult = results.getValues(i);
				int measureState = results.getMeasureState(i);
				MeasureValue value = thisResult.get(0);
				boolean valid = (measureState == MeasureDefinition.MEASURE_SUCCESS);
				parametricAnalysisResultsModel.addSample(results.getName(i), value.getLowerBound(), value.getMeanValue(),
						value.getUpperBound(), valid);
			}
		}

		setResultsLatch.countDown();

		pad.restoreOriginalValues();
		exec.shutdown();

		return hasResults ? parametricAnalysisResultsModel : null;
	}

	/**
	 * @return true if the run was ended early by {@link #stopRun()}
	 */
	public boolean isStopped() {
		return stopped;
	}

	/**
	 * Pauses every step currently running.
	 */
	public void pause() {
		for (Entry<Integer, PASimulation> entry : entries()) {
			entry.getValue().pause();
		}
	}

	/**
	 * Stops the run, aborting the measures of every step still in flight, and
	 * blocks until the results of the steps that did finish have been collected.
	 */
	public void stopRun() {
		if (exec != null) {
			exec.shutdownNow();
		}

		for (Entry<Integer, PASimulation> entry : entries()) {
			entry.getValue().stopSimulation();
		}

		while (resultsLatch != null && resultsLatch.getCount() > 0) {
			resultsLatch.countDown();
		}

		try {
			if (setResultsLatch != null) {
				setResultsLatch.await();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		stopped = true;
	}

	/**
	 * Restarts every paused step.
	 */
	public void restart() {
		for (Entry<Integer, PASimulation> entry : entries()) {
			entry.getValue().restart();
		}
	}

	/**
	 * Aborts a measure across every running step.
	 *
	 * @param index index of the measure to be aborted
	 */
	@Override
	public void abortMeasure(int index) {
		for (Entry<Integer, PASimulation> entry : entries()) {
			entry.getValue().abortMeasureAtRefresh(index);
		}
	}

	private Set<Entry<Integer, PASimulation>> entries() {
		return activeSimulations.entrySet();
	}

	private void fireStepCompleted() {
		if (listener != null) {
			listener.stepCompleted();
		}
	}

	private void fireOutOfMemory(String message) {
		if (listener != null) {
			listener.outOfMemory(message);
		}
	}

	private void fireException(Exception e) {
		if (listener != null) {
			listener.exception(e);
		} else {
			e.printStackTrace();
		}
	}

	/**
	 * Runs one step of the analysis and parses its result file.
	 */
	private class SimSolver implements Runnable {

		private DispatcherJSIMschema simulator;
		private TimerThread timer;
		private int step;
		private CountDownLatch latch;

		public SimSolver(DispatcherJSIMschema simulator, int step, CountDownLatch latch) {
			this.simulator = simulator;
			this.step = step;
			this.latch = latch;
		}

		public void setTimer(TimerThread timer) {
			this.timer = timer;
		}

		@Override
		public void run() {
			try {
				PASimulation paSim = new PASimulation(simulator);
				if (timer != null) {
					paSim.setTimer(timer);
					timer.start();
				}
				activeSimulations.put(step, paSim);
				simulator.solveModel();
				activeSimulations.remove(step);
			} catch (OutOfMemoryError err) {
				simulator.abortAllMeasures();
				simulator.killSimulation();
				fireOutOfMemory("Out of memory error. Try to run Java Virtual Machine with more heap size (-Xmx<num>m)");
			} catch (InterruptedException ex) {
				// interrupted by stopRun()
				return;
			} catch (Exception ex) {
				simulator.abortAllMeasures();
				simulator.killSimulation();
				fireException(ex);
			}

			File output = simulator.getSimulation().getOutputFile();
			output.deleteOnExit();

			Document doc = XMLReader.loadXML(output.getAbsolutePath());
			StoredResultsModel results = new StoredResultsModel();
			XMLResultsReader.parseXML(doc, results);
			simulationResults[step] = results;

			fireStepCompleted();

			if (timer != null) {
				timer.kill();
			}
			latch.countDown();
		}

	}

	/**
	 * Control handle for one in-flight step.
	 */
	private static class PASimulation {

		private DispatcherJSIMschema simulator;
		private TimerThread timer;

		public PASimulation(DispatcherJSIMschema simulator) {
			this.simulator = simulator;
		}

		public void abortMeasureAtRefresh(int index) {
			simulator.abortMeasureAtRefresh(index);
		}

		public void stopSimulation() {
			if (timer != null) {
				timer.kill();
			}
			simulator.abortAllMeasures();
			simulator.killSimulation();
		}

		public void restart() {
			if (timer != null) {
				timer.restart();
			}
			simulator.restartSim();
		}

		public void setTimer(TimerThread timer) {
			this.timer = timer;
		}

		public void pause() {
			if (timer != null) {
				timer.pause();
			}
			simulator.pauseSim();
		}

	}

}
