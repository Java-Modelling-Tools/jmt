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

package jmt.gui.common.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;

import jmt.engine.simDispatcher.DispatcherJSIMschema;
import jmt.framework.gui.graph.MeasureValue;
import jmt.gui.common.controller.TimerThread;
import jmt.gui.common.definitions.AbortMeasure;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.GuiInterface;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.definitions.PAResultsModel;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StoredResultsModel;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.common.panels.parametric.PAProgressWindow;
import jmt.gui.common.panels.parametric.PAResultsWindow;
import jmt.gui.common.xml.XMLReader;
import jmt.gui.common.xml.XMLResultsReader;
import jmt.gui.common.xml.XMLWriter;

import org.w3c.dom.Document;

/**
 * <p>
 * Title: PADispatcherThread
 * </p>
 * <p>
 * Description: This thread is responsible for dispatching several simulations.
 * If a simulation max time has been specified, an inner thread will be used to
 * stop simulation after that time. We do not rely on engine method as it does
 * not appear to work properly.
 * </p>
 *
 * @author Francesco D'Aquino Date: 29-dic-2005 Time: 9.58.49
 *
 * Fixed by Bertoli Marco
 */
public class PADispatcherThread extends Thread implements AbortMeasure {

	private SimulationDefinition simd;
	//private CommonModel model;
	private GuiInterface gui;
	private PAProgressWindow progressWindow;
	private boolean stopped;
	private int currentStep;

	private Map<Integer, PASimulation> active_simulations = new HashMap<>();
	private ExecutorService exec;
	private PAResultsModel parametricAnalysisResultsModel;
	private CountDownLatch results_latch;
	private CountDownLatch set_results_latch;

	private StoredResultsModel[] simulation_results;

	/**
	 * Construct a new Parametric Analysis Dispatcher Thread
	 * @param gui reference to current gui object (can be JSIMMain or Mediator)
	 * @param simd Reference to the simulation definition
	 */
	public PADispatcherThread(GuiInterface gui, SimulationDefinition simd, PAProgressWindow papw) {
		this.setName("PADispatcherThread");
		progressWindow = papw;
		this.simd = simd;
		this.gui = gui;
		// Avoid hanging the system during execution
		this.setPriority(Thread.MIN_PRIORITY);
	}

	/**
	 * Pauses current simulation only if it was already started
	 */
	public void pauseSimulation() {
		Set<Entry<Integer, PASimulation>> entrySet = active_simulations.entrySet();
		for (Entry<Integer, PASimulation> entry : entrySet) {
			PASimulation sim = entry.getValue();
			sim.pause();
		}
		gui.changeSimActionsState(true, false, true);
		progressWindow.pause();
	}

	/**
	 * Stops current simulation, forcing abort of all measures,
	 * only if it was already started
	 */
	public void stopSimulation() {
		// this.currentStep =
		// simd.getParametricAnalysisModel().getNumberOfSteps();
		exec.shutdownNow();

		Set<Entry<Integer, PASimulation>> entrySet = active_simulations.entrySet();
		for (Entry<Integer, PASimulation> entry : entrySet) {
			PASimulation sim = entry.getValue();
			sim.stopSimulation();
		}

		while (results_latch.getCount() > 0) {
			results_latch.countDown();
		}

		try {
			set_results_latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		stopped = true;
		progressWindow.stop();
	}

	/**
	 * Restarts current simulation, forcing abort of all measures, only if it
	 * was already started
	 */
	public void restartSimulation() {
		Set<Entry<Integer, PASimulation>> entrySet = active_simulations.entrySet();
		for (Entry<Integer, PASimulation> entry : entrySet) {
			PASimulation sim = entry.getValue();
			sim.restart();
		}

		gui.changeSimActionsState(false, true, true);
		progressWindow.restart();
	}

	/**
	 * Aborts a measure, given its index
	 * 
	 * @param index
	 *            index of the measure to be aborted
	 */
	public void abortMeasure(int index) {
		Set<Entry<Integer, PASimulation>> entrySet = active_simulations.entrySet();
		for (Entry<Integer, PASimulation> entry : entrySet) {
			PASimulation sim = entry.getValue();
			sim.abortMeasureAtRefresh(index);
		}

	}

	@Override
	public void run() {
		File simulationFile = null;
		ParametricAnalysisDefinition pad = simd.getParametricAnalysisModel();
		pad.createValuesSet();
		int steps = pad.getNumberOfSteps();
		int parallelism = simd.getWhatIfParallelism();

		results_latch = new CountDownLatch(steps);
		set_results_latch = new CountDownLatch(1);
		parametricAnalysisResultsModel = new PAResultsModel((CommonModel) simd);
		exec = Executors.newFixedThreadPool(parallelism);
		simulation_results = new StoredResultsModel[steps];

		for (currentStep = 0; currentStep < steps; currentStep++) {
			pad.changeModel(currentStep);
			try {
				simulationFile = File.createTempFile("~JModelSimulation-" + currentStep, ".xml");
				simulationFile.deleteOnExit();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "File exception", "Error", JOptionPane.ERROR_MESSAGE);
			}
			simulationFile.deleteOnExit();
			XMLWriter.writeXML(simulationFile, (CommonModel) simd);

			DispatcherJSIMschema simulator = (new DispatcherJSIMschema(simulationFile));
			SimSolver ss = new SimSolver(simulator, currentStep, results_latch);
			if (simd.getMaximumDuration().doubleValue() > 0) {
				ss.setTimer(new TimerThread(simulator, simd.getMaximumDuration().doubleValue()));
			}

			exec.execute(ss);
		}

		try {
			results_latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean hasResults = false;
		for (StoredResultsModel results : simulation_results) {
			if (results == null) {
				continue;
			}
			for (int i = 0; i < results.getMeasureNumber(); i++) {
				hasResults = true;
				Vector thisResult = results.getValues(i);
				int measureState = results.getMeasureState(i);
				Object value = thisResult.get(0);
				boolean valid = (measureState == MeasureDefinition.MEASURE_SUCCESS);
				MeasureValue tempValue = (MeasureValue) value;
				parametricAnalysisResultsModel.addSample(results.getName(i), tempValue.getLowerBound(),
						tempValue.getMeanValue(), tempValue.getUpperBound(), valid);
			}
		}

		set_results_latch.countDown();

		pad.restoreOriginalValues();
		if (!stopped) {
			progressWindow.finished();
			// progressWindow.hide();
			progressWindow.stopAnimation();
		} else {
			progressWindow.stop();
		}
		
		if (hasResults) {
			// set the parametric analysis model results
			simd.setSimulationResults(parametricAnalysisResultsModel);
			PAResultsWindow resWin = new PAResultsWindow(simd, gui.getFileName());
			gui.setResultsWindow(resWin);
			gui.showResultsWindow();
		}
		gui.changeSimActionsState(true, false, false);
		// timer = null;
		// simulator = null;
		exec.shutdown();

	}

	class SimSolver implements Runnable {

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
				PASimulation PASim = new PASimulation(simulator);
				if (timer != null) {
					PASim.setTimer(timer);
					timer.start();
				}
				active_simulations.put(step, PASim);
				simulator.solveModel();
				active_simulations.remove(step);

			} catch (OutOfMemoryError err) {
				simulator.abortAllMeasures();
				simulator.killSimulation();
				progressWindow.stop();
				progressWindow.dispose();
				gui.showErrorMessage(
						"Out of memory error. Try to run Java Virtual Machine with more heap size (-Xmx<num>m)");
			} catch (InterruptedException ex) {
				// interrupted by stop simulation
				return;
			} catch (Exception ex) {
				simulator.abortAllMeasures();
				simulator.killSimulation();
				gui.handleException(ex);
			}

			File output = simulator.getSimulation().getOutputFile();
			output.deleteOnExit();

			Document doc = XMLReader.loadXML(output.getAbsolutePath());
			StoredResultsModel results = new StoredResultsModel();
			XMLResultsReader.parseXML(doc, results);
			simulation_results[step] = results;

			progressWindow.incrementStepNumber();

			if (timer != null) {
				timer.kill();
				// simulator.killSimulation();
			}
			latch.countDown();
		}
	}

	class PASimulation {
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
			// gui.changeSimActionsState(true, false, false);
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
