package common.controller;

import common.definitions.GuiInterface;
import common.definitions.SimulationDefinition;

/**
 * Factory interface for creating SimulationRunner instances.
 * Registered at runtime by jmt-jsimg-gui so that jmt-jmch-gui panels
 * can create DispatcherThread objects without a compile-time dependency.
 */
public interface SimulationRunnerFactory {
	SimulationRunner create(GuiInterface gui, SimulationDefinition sd);

	// ── static registry ───────────────────────────────────────────────

	final class Registry {
		private static SimulationRunnerFactory instance;

		public static void set(SimulationRunnerFactory f) { instance = f; }

		public static SimulationRunner create(GuiInterface gui, SimulationDefinition sd) {
			if (instance == null) throw new IllegalStateException(
				"SimulationRunnerFactory not registered — jmt-jsimg-gui not on classpath?");
			return instance.create(gui, sd);
		}
	}
}
