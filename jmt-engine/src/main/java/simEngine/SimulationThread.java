package simEngine;

/**
 * Marker interface for threads that run a simulation.
 * Allows engine classes (e.g. ReplayerPar) to detect simulation context
 * without importing the GUI-layer DispatcherThread.
 */
public interface SimulationThread {
	void notifyDetectedMalformedReplayerFile(String msg);
}
