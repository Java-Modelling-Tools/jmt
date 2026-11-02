package solvers.dispatchers;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.dispatchers.DispatchFixtures.*;

import org.junit.jupiter.api.Test;

import analytical.ExactConstants;
import analytical.ExactModel;
import jmt.common.exception.InputDataException;
import jmt.common.exception.UnsupportedModelException;
import solvers.SolverAlgorithm;
import solvers.multiSolverAlgorithms.MultiFixtures;

class ModelSolverCompatibilityCheckerTest {

	private static ExactModel open() {
		return MultiFixtures.model(new int[] { LI }, new int[] { OPEN }, new double[] { 0.1 }, new double[][] { { 1 } });
	}

	private static ExactModel mixed() {
		return MultiFixtures.model(new int[] { LI }, new int[] { CLOSED, OPEN }, new double[] { 1, 0.1 }, new double[][] { { 1, 1 } });
	}

	@Test
	void compatibleCombinationsPass() throws Exception {
		ModelSolverCompatibilityChecker.checkCompatibility(repairman(2), new double[] { 2 }, SolverAlgorithm.EXACT);
		ModelSolverCompatibilityChecker.checkCompatibility(open(), new double[] { 0.1 }, SolverAlgorithm.EXACT);
		ModelSolverCompatibilityChecker.checkCompatibility(mixed(), new double[] { 1, 0.1 }, SolverAlgorithm.EXACT);
	}

	@Test
	void everyAlgorithmFlagIsEnforced() throws Exception {
		ExactModel ld = MultiFixtures.model(new int[] { ExactConstants.STATION_LD }, new int[] { CLOSED }, new double[] { 2 }, new double[][] { { 1 } });
		ExactModel prs = MultiFixtures.model(new int[] { ExactConstants.STATION_PRS }, new int[] { CLOSED, CLOSED }, new double[] { 1, 1 },
				new double[][] { { 1, 1 } });
		assertTrue(ld.isLd());
		assertTrue(prs.isPriority());
		for (SolverAlgorithm a : SolverAlgorithm.values()) {
			expect(!a.supportsLoadDependent(), "load-dependent", ld, a);
			expect(!a.supportsPriorities(), "priority", prs, a);
			expect(!a.supportsOpenClasses(), "open", open(), a);
			expect(!a.supportsOpenClasses(), "open", mixed(), a);
			expect(!a.supportsClosedClasses(), "closed", repairman(1), a);
			expect(!a.supportsClosedClasses(), "closed", mixed(), a);
		}
	}

	private static void expect(boolean refused, String what, ExactModel m, SolverAlgorithm a) throws Exception {
		if (refused) {
			UnsupportedModelException e = assertThrows(UnsupportedModelException.class,
					() -> ModelSolverCompatibilityChecker.checkModelAlgorithmCompatability(m, a), a + " must refuse " + what);
			assertTrue(e.getMessage().contains(what), e.getMessage());
		}
		// when not refused for this reason other flags may still refuse, so no positive assertion here
	}

	@Test
	void nonPositivePopulationOrRateIsRejectedPerModelType() {
		InputDataException c = assertThrows(InputDataException.class,
				() -> ModelSolverCompatibilityChecker.checkClassPopulationsAndArrivalRates(repairman(1), new double[] { 0 }));
		assertTrue(c.getMessage().contains("Populations"), c.getMessage());
		InputDataException o = assertThrows(InputDataException.class,
				() -> ModelSolverCompatibilityChecker.checkClassPopulationsAndArrivalRates(open(), new double[] { 0 }));
		assertTrue(o.getMessage().contains("Arrival rates"), o.getMessage());
		InputDataException m = assertThrows(InputDataException.class,
				() -> ModelSolverCompatibilityChecker.checkClassPopulationsAndArrivalRates(mixed(), new double[] { 1, -0.5 }));
		assertTrue(m.getMessage().contains("Populations and arrival rates"), m.getMessage());
	}

	@Test
	void compatibilityIsCheckedBeforeValues() {
		// an LD model with an unsupporting algorithm and a zero population: the model/algorithm error comes first
		ExactModel ld = MultiFixtures.model(new int[] { ExactConstants.STATION_LD }, new int[] { CLOSED }, new double[] { 0 }, new double[][] { { 1 } });
		assertThrows(UnsupportedModelException.class,
				() -> ModelSolverCompatibilityChecker.checkCompatibility(ld, new double[] { 0 }, SolverAlgorithm.RECAL));
	}
}
