package jmt.gui.common.controller;

import jmt.engine.simDispatcher.Dispatcher_jSIMschema;

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

public class PATimerThread extends Thread {

	protected Dispatcher_jSIMschema sim;
	protected long totalTime;
	protected long elapsedTime;
	protected long initialTime;
	protected boolean paused = false;
	protected boolean killed = false;
	protected boolean ended = false;

	public PATimerThread(Dispatcher_jSIMschema simulator, double maxDuration) {
		this.setName("PATimerThread");
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

}
