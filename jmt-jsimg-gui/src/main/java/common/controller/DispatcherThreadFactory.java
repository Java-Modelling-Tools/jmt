package common.controller;

import common.definitions.GuiInterface;
import common.definitions.SimulationDefinition;

/**
 * Registers DispatcherThread as the SimulationRunner implementation.
 * Called once from GraphStartScreen (the application entry point) so
 * that any module can create simulation runners via SimulationRunnerFactory.
 */
public class DispatcherThreadFactory {

	static {
		SimulationRunnerFactory.Registry.set(
			(gui, sd) -> new DispatcherThread(gui, sd)
		);
	}

	/** Trigger static initialisation from any module. */
	public static void register() { /* static block runs on class load */ }
}
