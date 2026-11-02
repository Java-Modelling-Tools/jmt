package solvers.utilities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import solvers.SolverMulti;

/** Utilisation law: an open load is sustainable iff sum_r lambda_r * V_r * S_r &lt; servers at every non-delay station. */
class ProcessingCapacityCheckerTest {

	private static double[][][] serv(double... s) {
		double[][][] t = new double[1][s.length][1];
		for (int r = 0; r < s.length; r++) {
			t[0][r][0] = s[r];
		}
		return t;
	}

	private static double[][] ones(int r) {
		double[][] v = new double[1][r];
		java.util.Arrays.fill(v[0], 1);
		return v;
	}

	private static final int[] LI = { SolverMulti.LI };

	@Test
	void singleServerMM1Boundary() {
		assertTrue(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(1), serv(1.0), new double[] { 0.5 }));
		assertTrue(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(1), serv(1.0), new double[] { 0.999 }));
		assertFalse(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(1), serv(1.0), new double[] { 1.0 }));
		assertFalse(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(1), serv(1.0), new double[] { 2.0 }));
		assertTrue(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(1), serv(1.0), new double[] { 0.0 }));
	}

	@Test
	void classesAddUp() {
		// 0.3*1 + 0.4*2 = 1.1 >= 1
		assertFalse(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(2), serv(1.0, 2.0), new double[] { 0.3, 0.4 }));
		// 0.3*1 + 0.2*2 = 0.7 < 1
		assertTrue(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(2), serv(1.0, 2.0), new double[] { 0.3, 0.2 }));
	}

	@Test
	void visitsScaleTheLoad() {
		double[][] v = { { 3 } };
		assertFalse(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, v, serv(1.0), new double[] { 0.4 }));
		assertTrue(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, v, serv(1.0), new double[] { 0.3 }));
	}

	@Test
	void multipleServersRaiseTheBound() {
		assertFalse(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(1), serv(1.0), new double[] { 1.5 }));
		assertTrue(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(1), serv(1.0), new double[] { 1.5 }, new int[] { 2 }));
		assertFalse(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(LI, ones(1), serv(1.0), new double[] { 2.0 }, new int[] { 2 }));
	}

	@Test
	void delayStationsNeverLimitCapacity() {
		assertTrue(ProcessingCapacityChecker.openHasSufficientProcessingCapacity(new int[] { SolverMulti.DELAY }, ones(1), serv(100.0), new double[] { 50 }));
	}

	@Test
	void closedOnlyModelsAreAlwaysSufficient() {
		assertTrue(ProcessingCapacityChecker.mixedHasSufficientProcessingCapacity(LI, ones(1), serv(1000.0), new double[] { 99 },
				new int[] { SolverMulti.CLOSED_CLASS }));
	}

	@Test
	void mixedModelCountsOnlyOpenClasses() {
		int[] types = { SolverMulti.CLOSED_CLASS, SolverMulti.OPEN_CLASS };
		// class 0 closed (its 'rate' is a population, ignored); open: 0.5 * 1.5 = 0.75 < 1
		assertTrue(ProcessingCapacityChecker.mixedHasSufficientProcessingCapacity(LI, ones(2), serv(9.0, 1.5), new double[] { 10, 0.5 }, types));
		assertFalse(ProcessingCapacityChecker.mixedHasSufficientProcessingCapacity(LI, ones(2), serv(9.0, 1.5), new double[] { 10, 0.7 }, types));
	}

	@Test
	void priorityStationOverloadIsDetected() {
		int[] hol = { SolverMulti.HOL };
		int[] types = { SolverMulti.OPEN_CLASS, SolverMulti.OPEN_CLASS };
		int[] prio = { 0, 1 };
		// utilisation 0.1*1 + 0.2*1 = 0.3 < 1 and the highest-priority arrival rate is small: sufficient
		assertTrue(ProcessingCapacityChecker.mixedHasSufficientProcessingCapacity(hol, ones(2), serv(1.0, 1.0), new double[] { 0.1, 0.2 }, types,
				new int[] { 1 }, prio));
		// utilisation 0.5 + 0.6 = 1.1 > 1: insufficient
		assertFalse(ProcessingCapacityChecker.mixedHasSufficientProcessingCapacity(hol, ones(2), serv(1.0, 1.0), new double[] { 0.5, 0.6 }, types,
				new int[] { 1 }, prio));
		// same through the PRS station type and the default-priority overload
		assertFalse(ProcessingCapacityChecker.mixedHasSufficientProcessingCapacity(new int[] { SolverMulti.PRS }, ones(2), serv(1.0, 1.0),
				new double[] { 0.5, 0.6 }, types, new int[] { 1 }));
	}
}
