package common.controller;

import java.io.File;
import java.io.IOException;

/**
 * Interface for objects that can run a JSIM simulation.
 * Implemented by DispatcherThread in jmt-jsimg-gui.
 * Allows JMCH's AnimationPanel (in jmt-jmch-gui) to drive
 * a simulation run without a compile-time dep on DispatcherThread.
 */
public interface SimulationRunner {
	void setDaemon(boolean on);
	void startSimulation(File modelFile) throws IOException;
	boolean isAlive();
	void interrupt();
	void stopSimulation();
}
