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

import common.definitions.AbortMeasure;
import common.definitions.GuiInterface;
import common.definitions.PAResultsModel;
import common.definitions.SimulationDefinition;
import common.panels.parametric.PAProgressWindow;
import common.panels.parametric.PAResultsWindow;

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
 * <p>
 * The step loop itself lives in {@link ParametricAnalysisRunner}, which has no
 * Swing dependency; this class adds the progress window, the results window and
 * the toolbar state that only make sense with a GUI attached.
 * </p>
 *
 * @author Francesco D'Aquino Date: 29-dic-2005 Time: 9.58.49
 *
 * Fixed by Bertoli Marco
 */
public class PADispatcherThread extends Thread implements AbortMeasure {

	private SimulationDefinition simd;
	private GuiInterface gui;
	private PAProgressWindow progressWindow;
	private ParametricAnalysisRunner runner;

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
		this.runner = new ParametricAnalysisRunner(simd, new ProgressWindowListener());
		// Avoids hanging the system during execution
		this.setPriority(Thread.MIN_PRIORITY);
	}

	/**
	 * Pauses current simulation only if it was already started
	 */
	public void pauseSimulation() {
		runner.pause();
		gui.changeSimActionsState(true, false, true);
		progressWindow.pause();
	}

	/**
	 * Stops current simulation, forcing abort of all measures,
	 * only if it was already started
	 */
	public void stopSimulation() {
		runner.stopRun();
		progressWindow.stop();
	}

	/**
	 * Restarts current simulation, forcing abort of all measures, only if it
	 * was already started
	 */
	public void restartSimulation() {
		runner.restart();
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
		runner.abortMeasure(index);
	}

	@Override
	public void run() {
		PAResultsModel results = runner.run();

		if (!runner.isStopped()) {
			progressWindow.finished();
			progressWindow.stopAnimation();
		} else {
			progressWindow.stop();
		}

		if (results != null) {
			// Sets the parametric analysis model results
			simd.setSimulationResults(results);
			PAResultsWindow resWin = new PAResultsWindow(simd, gui.getFileName());
			gui.setResultsWindow(resWin);
			gui.showResultsWindow();
		}
		gui.changeSimActionsState(true, false, false);
	}

	/**
	 * Drives the progress window and surfaces per-step failures through the GUI.
	 */
	private class ProgressWindowListener implements ParametricAnalysisRunner.Listener {

		@Override
		public void stepCompleted() {
			progressWindow.incrementStepNumber();
		}

		@Override
		public void outOfMemory(String message) {
			progressWindow.stop();
			progressWindow.dispose();
			gui.showErrorMessage(message);
		}

		@Override
		public void exception(Exception e) {
			gui.handleException(e);
		}

	}

}
