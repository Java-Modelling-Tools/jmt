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

import jmt.engine.simDispatcher.DispatcherJSIMschema;

/**
 * <p>Title:</p>
 * <p>Description: An inner thread used to stop simulation after timeout elapsed
 *
 *
 * @author Francesco D'Aquino, Bertoli Marco
 *         Date: 7-feb-2006
 *         Time: 23.53.25
 *
 */
public class TimerThread extends Thread {

	protected DispatcherJSIMschema sim;
	protected long totalTime;
	protected long elapsedTime;
	protected long initialTime;
	protected boolean paused = false;
	protected boolean killed = false;
	protected boolean ended = false;

	public TimerThread(DispatcherJSIMschema simulator, double maxDuration) {
		this.setName("TimerThread");
		this.sim = simulator;
		this.totalTime = Math.round(maxDuration * 1000);
		this.elapsedTime = 0;
	}

	/**
	 * Thread run's method. It will wait until maxDuration has elapsed, then stops
	 * simulation
	 */
	@Override
	public void run() {
		while (!killed && !ended) {
			try {
				synchronized (this) {
					if (!paused) {
						initialTime = System.currentTimeMillis();
						wait(100);
						elapsedTime += System.currentTimeMillis() - initialTime;
						if (totalTime > 0 && elapsedTime >= totalTime) {
							ended = true;
						}
					} else {
						wait();
					}
				}
			} catch (InterruptedException e) {
				System.out.println("Error: Timer thread interrupted unexpectedly...");
			}
		}
		while (!sim.isFinished()) {
			sim.abortAllMeasures();
			try {
				sleep(500);
			} catch (InterruptedException e) {
				// Never thrown
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method have to be called when simulation is paused
	 */
	public synchronized void pause() {
		paused = true;
		notifyAll();
	}

	/**
	 * This method have to be called when simulation is restarted after pause
	 */
	public synchronized void restart() {
		paused = false;
		notifyAll();
	}

	/**
	 * This method have to be called when simulation is stopped before timeout
	 */
	public synchronized void kill() {
		killed = true;
		notifyAll();
	}

	/**
	 * Returns elapsed time
	 * @return elapsed time
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Returns elapsed percentage
	 * @return elapsed percentage
	 */
	public double getElapsedPercentage() {
		if (totalTime < 0) {
			return 0.0;
		}
		double percent = (double) elapsedTime / (double) totalTime;
		// Fix possible problem when this function is called after simulation ends
		if (percent > 1.0) {
			percent = 1.0;
		}
		return percent;
	}

}
