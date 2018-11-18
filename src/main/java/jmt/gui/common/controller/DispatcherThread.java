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

import java.awt.Cursor;
import java.io.File;

import jmt.engine.simDispatcher.DispatcherJSIMschema;
import jmt.gui.common.definitions.AbortMeasure;
import jmt.gui.common.definitions.GuiInterface;
import jmt.gui.common.definitions.ResultsModel;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.panels.ResultsWindow;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.controller.ModelSnapshot;
import jmt.gui.jsimgraph.controller.SimulationStateChecker;

/**
 * <p>Title: Dispatcher Thread</p>
 * <p>Description: This thread is responsible for dispatching simulation. If simulation max time has
 * been specified, an inner thread will be used to stop simulation after that time. We do not rely
 * on engine method as it does not appear to work properly.</p>
 * 
 * @author Bertoli Marco
 *         Date: 8-set-2005
 *         Time: 11.40.46
 *
 * Modified by Francesco D'Aquino 9/11/2005
 */
public class DispatcherThread extends Thread implements AbortMeasure {

	private SimulationStateChecker simStateChecker;
	private DispatcherJSIMschema simulator;
	private GuiInterface gui;
	private SimulationDefinition sd;
	private TimerThread timer = null;
	private PollerThread poller = null;
	private ResultsModel results;

	/**
	 * Construct a new Dispatcher Thread
	 * @param gui reference to current gui object (can be JSIMMain or Mediator)
	 * @param sd Reference to Simulation Definition data structure
	 */
	public DispatcherThread(GuiInterface gui, SimulationDefinition sd) {
		this.setName("DispatcherThread");
		this.gui = gui;
		this.sd = sd;
		this.results = (ResultsModel) sd.getSimulationResults();
		poller = new PollerThread(sd.getPollingInterval().doubleValue(), this);
		// Avoid hanging the system during execution
		this.setPriority(Thread.MIN_PRIORITY);
	}

	/**
	 * Run method. This method will simply start simulation, it is designed to be called only
	 * after startSimulation has been called.
	 */
	@Override
	public void run() {
		timer.start();
		poller.start();
		try {
			simulator.solveModel();
		} catch (OutOfMemoryError err) {
			simulator.abortAllMeasures();
			simulator.killSimulation();
			gui.showErrorMessage("Out of memory error. Try to run Java Virtual Machine with more heap size (-Xmx<num>m)");
		} catch (Exception ex) {
			simulator.abortAllMeasures();
			simulator.killSimulation();
			gui.handleException(ex);
		}
		timer.kill();
		gui.changeSimActionsState(true, false, false);
		results.refresh(1.0, timer.getElapsedTime());

		// Removes output file, if it was created.
		if (simulator.getOutputFile() != null) {
			if (!simulator.getOutputFile().delete()) {
				simulator.getOutputFile().deleteOnExit();
			}
		}
	}

	/**
	 * This method starts simulation, given simulation XML file
	 * @param simulationFile file where simulation model is stored
	 */
	public void startSimulation(File simulationFile) {
		simulator = new DispatcherJSIMschema(simulationFile);
		// ----------- Francesco D'Aquino ---------------
		// If animation is enabled creates a simStateChecker
		if (sd.isAnimationEnabled()) {
			simStateChecker = new SimulationStateChecker((Mediator) gui, simulator);
		}
		// ----------- end Francesco D'Aquino -----------
		// If needed sets simulation seed
		if (!sd.getUseRandomSeed().booleanValue()) {
			simulator.setSimulationSeed(sd.getSimulationSeed().longValue());
		}
		timer = new TimerThread(simulator, sd.getMaximumDuration().doubleValue());
		gui.changeSimActionsState(false, true, true);
		start();
	}

	/**
	 * Pauses current simulation only if it was already started
	 */
	public void pauseSimulation() {
		if (simulator != null) {
			timer.pause();
			gui.changeSimActionsState(true, false, true);
			simulator.pauseSim();
		}
	}

	/**
	 * Stops current simulation, forcing abort of all measures,
	 * only if it was already started
	 */
	public void stopSimulation() {
		if (simulator != null) {
			timer.kill();
			//gui.changeSimActionsState(true, false, false);
			simulator.abortAllMeasures();
		}
	}

	/**
	 * Restarts current simulation, forcing abort of all measures,
	 * only if it was already started
	 */
	public void restartSimulation() {
		if (simulator != null) {
			timer.restart();
			gui.changeSimActionsState(false, true, true);
			simulator.restartSim();
		}
	}

	/**
	 * Aborts a measure, given its index
	 * @param index index of the measure to be aborted
	 */
	public void abortMeasure(int index) {
		simulator.abortMeasureAtRefresh(index);
	}

	/**
	 * Inner thread used to poll
	 */
	protected class PollerThread extends Thread {
		protected long interval;
		protected DispatcherThread dispatcher;
		protected boolean initialized = false;

		public PollerThread(double seconds, DispatcherThread dispatcher) {
			this.setName("PollerThread");
			interval = Math.round(seconds * 1000);
			this.dispatcher = dispatcher;
		}

		@Override
		public void run() {
			if (gui instanceof Mediator) {
				((Mediator) gui).setGraphCursor(new Cursor(Cursor.WAIT_CURSOR));
			}
			// While simulation is not finished, polls at given intervals
			while (!dispatcher.simulator.isFinished()) {
				// Wait to collect results until simulation is really started
				while (!dispatcher.simulator.isStarted() && !dispatcher.simulator.isFinished()) {
					try {
						sleep(500);
					} catch (InterruptedException ex) {
						System.err.println("Unexpected InterruptException in PollerThread");
					}
				}
				// If it is first poll, initialize Results data structure
				if (!initialized) {
					if (gui instanceof Mediator) {
						((Mediator) gui).setGraphCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					}
					dispatcher.simulator.refreshTempMeasures();
					dispatcher.results.setTempMeasures(dispatcher.simulator.getTempMeasures(),
							dispatcher.simulator.checkSimProgress(), timer.getElapsedTime());
					initialized = true;
					// Sets ResultsWindow
					ResultsWindow rw = new ResultsWindow(sd, dispatcher, gui.getFileName());
					gui.setResultsWindow(rw);
					// Moves it to lower right corner if animation is enabled
					if (sd.isAnimationEnabled()) {
						rw.moveToLowerRightCorner();
					}
					// Shows it
					gui.showResultsWindow();
				}
				// If it is next one, refresh stored tempMeasures (if simulation is started and not paused)
				else if (!dispatcher.simulator.isPaused()) {
					dispatcher.simulator.refreshTempMeasures();
					double progress = dispatcher.simulator.checkSimProgress();
					// Progress is the biggest between simulation progress and timer progress
					if (timer.getElapsedPercentage() > progress) {
						progress = timer.getElapsedPercentage();
					}
					dispatcher.results.refresh(progress, timer.getElapsedTime());
					// ------------ Francesco D'Aquino -------------------
					if (sd.isAnimationEnabled()) {
						if (!simStateChecker.isInitialized()) {
							simStateChecker.initialize();
						}
						ModelSnapshot tmpServersContent = (ModelSnapshot) simStateChecker.getServersContent().clone();
						ModelSnapshot tmpServersUtilization = (ModelSnapshot) simStateChecker.getServersUtilization().clone();
						if (!dispatcher.simulator.isFinished()) {//SPIC
							simStateChecker.getModelState();
						}//SPIC
						if (dispatcher.simulator.isFinished()) {
							simStateChecker.setServersContent(tmpServersContent);
							simStateChecker.setServersUtilization(tmpServersUtilization);
						}
						//simStateChecker.print();
						try {
							simStateChecker.forceDraw();
						} catch (Exception e) {
							//Ignore
						}
					}
					// -------------end Francesco D'Aquino ---------------
				}

				// Wait for polling interval
				try {
					sleep(interval);
				} catch (InterruptedException ex) {
					System.out.println("Error: Poller thread interrupted unexpectedly...");
				}
			}
			// Simulation is finished
		}
	}

	public void notifyDetectedMalformedReplayerFile(String msg) {
		results.detectedMalformedReplayerFile(msg);
	}

}
