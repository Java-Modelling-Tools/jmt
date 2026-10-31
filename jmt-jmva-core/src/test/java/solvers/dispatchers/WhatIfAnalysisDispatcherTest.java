package solvers.dispatchers;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.dispatchers.DispatchFixtures.*;

import org.junit.jupiter.api.Test;

import analytical.ExactConstants;
import analytical.ExactModel;
import jmt.common.exception.InputDataException;
import solvers.SolverAlgorithm;
import solvers.multiSolverAlgorithms.MultiFixtures;

/** The sweep must equal single solves of the modified model, and be monotone where theory says so. */
class WhatIfAnalysisDispatcherTest {

	private static final SolverAlgorithm EXACT = SolverAlgorithm.EXACT;

	private static void whatIf(ExactModel m, String type, double[] values, int cl, int st) {
		m.setWhatIfType(type);
		m.setWhatIfClass(cl);
		m.setWhatIfStation(st);
		m.setWhatIfValues(values);
	}

	@Test
	void customersSweepEqualsSingleSolvesAndGrowsMonotonically() throws Exception {
		// single class, repairman: the single-class path
		ExactModel m = repairman(1);
		double[] ns = { 1, 2, 3, 5, 8 };
		whatIf(m, ExactConstants.WHAT_IF_CUSTOMERS, ns, 0, -1);
		new SolverDispatcher().solve(m);
		assertTrue(m.areResultsOK());
		double prevX = 0, prevQ = 0;
		for (int i = 0; i < ns.length; i++) {
			ExactModel single = repairman((int) ns[i]);
			new SolverDispatcher().solve(single);
			assertEquals(x(single, EXACT, 0, 0, 0), x(m, EXACT, 0, 0, i), 1e-12, "X at " + ns[i]);
			assertEquals(q(single, EXACT, 0, 0, 0), q(m, EXACT, 0, 0, i), 1e-12, "Q at " + ns[i]);
			assertEquals(r(single, EXACT, 0, 0, 0), r(m, EXACT, 0, 0, i), 1e-12, "R at " + ns[i]);
			assertTrue(x(m, EXACT, 0, 0, i) > prevX && x(m, EXACT, 0, 0, i) <= 0.5 + 1e-12, "X increases to 1/D");
			assertTrue(q(m, EXACT, 0, 0, i) > prevQ);
			prevX = x(m, EXACT, 0, 0, i);
			prevQ = q(m, EXACT, 0, 0, i);
		}
		assertEquals(1.0, m.getClassData()[0], 0, "class data restored");
	}

	@Test
	void customersSweepOnAllClassesScalesEveryPopulation() throws Exception {
		ExactModel m = twoClasses(1, 2);
		double[] f = { 1, 2, 3 };
		whatIf(m, ExactConstants.WHAT_IF_CUSTOMERS, f, -1, -1);
		new SolverDispatcher().solve(m);
		for (int i = 0; i < f.length; i++) {
			ExactModel single = twoClasses((int) f[i] * 1, (int) f[i] * 2);
			new SolverDispatcher().solve(single);
			for (int cl = 0; cl < 2; cl++) {
				assertEquals(x(single, EXACT, 0, cl, 0), x(m, EXACT, 0, cl, i), 1e-12);
				assertEquals(q(single, EXACT, 1, cl, 0), q(m, EXACT, 1, cl, i), 1e-12);
			}
		}
		assertArrayEquals(new double[] { 1, 2 }, m.getClassData(), 0);
	}

	@Test
	void demandsSweepEqualsSingleSolvesAndThroughputFallsAsDemandGrows() throws Exception {
		// whatIfDemands sets the service time of the station to value/visits (unit visits)
		ExactModel m = twoQueues(3);
		double[] ds = { 1, 2, 4, 8 };
		whatIf(m, ExactConstants.WHAT_IF_DEMANDS, ds, 0, 0);
		new SolverDispatcher().solve(m);
		double prev = Double.MAX_VALUE;
		for (int i = 0; i < ds.length; i++) {
			ExactModel single = MultiFixtures.closed(new int[] { LI, LI }, new double[] { 3 }, new double[][] { { ds[i] }, { 2 } });
			new SolverDispatcher().solve(single);
			assertEquals(x(single, EXACT, 0, 0, 0), x(m, EXACT, 0, 0, i), 1e-12);
			assertEquals(q(single, EXACT, 1, 0, 0), q(m, EXACT, 1, 0, i), 1e-12);
			assertTrue(x(m, EXACT, 0, 0, i) < prev, "throughput strictly decreases with demand");
			prev = x(m, EXACT, 0, 0, i);
		}
		assertEquals(1.0, m.getServiceTimes()[0][0][0], 0, "service times restored");
	}

	@Test
	void demandsSweepOnAllClassesMultipliesTheDemands() throws Exception {
		ExactModel m = twoClasses(1, 1);
		whatIf(m, ExactConstants.WHAT_IF_DEMANDS, new double[] { 1, 3 }, -1, 0);
		new SolverDispatcher().solve(m);
		ExactModel single = MultiFixtures.closed(new int[] { LI, DELAY }, new double[] { 1, 1 }, new double[][] { { 3, 6 }, { 3, 1 } });
		new SolverDispatcher().solve(single);
		for (int cl = 0; cl < 2; cl++) {
			assertEquals(x(single, EXACT, 0, cl, 0), x(m, EXACT, 0, cl, 1), 1e-12);
		}
	}

	@Test
	void arrivalSweepOnOpenModelFollowsMm1ResponseTime() throws Exception {
		ExactModel m = MultiFixtures.model(new int[] { LI }, new int[] { OPEN }, new double[] { 0.1 }, new double[][] { { 2 } });
		double[] ls = { 0.1, 0.2, 0.3, 0.4 };
		whatIf(m, ExactConstants.WHAT_IF_ARRIVAL, ls, 0, -1);
		new SolverDispatcher().solve(m);
		double prev = 0;
		for (int i = 0; i < ls.length; i++) {
			assertEquals(2 / (1 - ls[i] * 2), r(m, EXACT, 0, 0, i), 1e-9, "R=D/(1-lambda D)");
			assertEquals(ls[i] * 2, u(m, EXACT, 0, 0, i), 1e-9);
			assertTrue(r(m, EXACT, 0, 0, i) > prev);
			prev = r(m, EXACT, 0, 0, i);
		}
		assertEquals(0.1, m.getClassData()[0], 0);
	}

	@Test
	void arrivalSweepOnAllOpenClassesScalesTheRates() throws Exception {
		ExactModel m = MultiFixtures.model(new int[] { LI }, new int[] { OPEN, OPEN }, new double[] { 0.1, 0.2 },
				new double[][] { { 1, 1 } });
		whatIf(m, ExactConstants.WHAT_IF_ARRIVAL, new double[] { 1, 2 }, -1, -1);
		new SolverDispatcher().solve(m);
		// total utilisation (0.1+0.2)*1 = 0.3, doubled 0.6; R = D/(1-U)
		assertEquals(1 / 0.7, r(m, EXACT, 0, 0, 0), 1e-9);
		assertEquals(1 / 0.4, r(m, EXACT, 0, 0, 1), 1e-9);
	}

	@Test
	void populationMixKeepsTotalAndMovesCustomersBetweenClasses() throws Exception {
		ExactModel m = twoClasses(2, 2);
		double[] fr = { 0.25, 0.5, 0.75 };
		whatIf(m, ExactConstants.WHAT_IF_MIX, fr, 0, -1);
		new SolverDispatcher().solve(m);
		double prevX0 = 0;
		for (int i = 0; i < fr.length; i++) {
			int n1 = (int) Math.round(fr[i] * 4);
			ExactModel single = twoClasses(n1, 4 - n1);
			new SolverDispatcher().solve(single);
			for (int cl = 0; cl < 2; cl++) {
				assertEquals(x(single, EXACT, 0, cl, 0), x(m, EXACT, 0, cl, i), 1e-12);
			}
			double total = 0;
			for (int st = 0; st < 2; st++) {
				for (int cl = 0; cl < 2; cl++) {
					total += q(m, EXACT, st, cl, i);
				}
			}
			assertEquals(4.0, total, 1e-9, "customers conserved");
			assertTrue(x(m, EXACT, 0, 0, i) > prevX0, "more class-0 customers -> more class-0 throughput");
			prevX0 = x(m, EXACT, 0, 0, i);
		}
		assertArrayEquals(new double[] { 2, 2 }, m.getClassData(), 0);
	}

	@Test
	void compareAlgorithmsStoresResultsForEveryAlgorithmAndRestoresTheChoice() throws Exception {
		ExactModel m = twoClasses(2, 1);
		whatIf(m, ExactConstants.WHAT_IF_DEMANDS, new double[] { 1, 2 }, 0, 0);
		m.setWhatifAlgorithm(SolverAlgorithm.EXACT, true);
		m.setWhatifAlgorithm(SolverAlgorithm.BARD_SCHWEITZER, true);
		m.setAlgorithmType(SolverAlgorithm.EXACT);
		new SolverDispatcher().solve(m);
		assertEquals(SolverAlgorithm.EXACT, m.getAlgorithmType());
		for (int i = 0; i < 2; i++) {
			double ex = x(m, EXACT, 0, 0, i);
			double bs = x(m, SolverAlgorithm.BARD_SCHWEITZER, 0, 0, i);
			assertEquals(ex, bs, 0.1 * ex, "Bard-Schweitzer within 10% of exact");
		}
	}

	@Test
	void stoppingFromTheListenerEndsTheSweepAndMarksResultsNotOk() throws Exception {
		ExactModel m = repairman(1);
		whatIf(m, ExactConstants.WHAT_IF_CUSTOMERS, new double[] { 1, 2, 3, 4 }, 0, -1);
		SolverDispatcher d = new SolverDispatcher();
		int[] calls = { 0 };
		d.addSolverListener(i -> {
			calls[0]++;
			d.stop();
		});
		d.solve(m);
		assertEquals(1, calls[0], "no further iteration after stop");
		assertFalse(m.areResultsOK());
		assertEquals(1.0, m.getClassData()[0], 0, "initial data restored even when stopped");
	}

	@Test
	void invalidSweepsAreRejectedWithInputDataException() {
		// arrival on a closed class / closed model
		ExactModel c = repairman(1);
		whatIf(c, ExactConstants.WHAT_IF_ARRIVAL, new double[] { 1, 2 }, 0, -1);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(c));
		ExactModel c2 = repairman(1);
		whatIf(c2, ExactConstants.WHAT_IF_ARRIVAL, new double[] { 1, 2 }, -1, -1);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(c2));

		// customers on an open class, and on an open model
		ExactModel o = MultiFixtures.model(new int[] { LI }, new int[] { OPEN }, new double[] { 0.1 }, new double[][] { { 1 } });
		whatIf(o, ExactConstants.WHAT_IF_CUSTOMERS, new double[] { 1, 2 }, 0, -1);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(o));
		ExactModel o2 = MultiFixtures.model(new int[] { LI }, new int[] { OPEN }, new double[] { 0.1 }, new double[][] { { 1 } });
		whatIf(o2, ExactConstants.WHAT_IF_CUSTOMERS, new double[] { 1, 2 }, -1, -1);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(o2));

		// fractional population
		ExactModel f = repairman(1);
		whatIf(f, ExactConstants.WHAT_IF_CUSTOMERS, new double[] { 1, 1.5 }, 0, -1);
		InputDataException e = assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(f));
		assertTrue(e.getMessage().contains("fractional"), e.getMessage());

		// demands without station, and on a load-dependent station
		ExactModel d = repairman(1);
		whatIf(d, ExactConstants.WHAT_IF_DEMANDS, new double[] { 1, 2 }, 0, -1);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(d));
		ExactModel ld = MultiFixtures.model(new int[] { ExactConstants.STATION_LD }, new int[] { 0 }, new double[] { 2 }, new double[][] { { 1 } });
		whatIf(ld, ExactConstants.WHAT_IF_DEMANDS, new double[] { 1, 2 }, 0, 0);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(ld));

		// mix: no class, one closed class, three closed classes
		ExactModel mx = twoClasses(2, 2);
		whatIf(mx, ExactConstants.WHAT_IF_MIX, new double[] { 0.25, 0.5 }, -1, -1);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(mx));
		ExactModel one = repairman(2);
		whatIf(one, ExactConstants.WHAT_IF_MIX, new double[] { 0.25, 0.5 }, 0, -1);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(one));
		ExactModel three = MultiFixtures.closed(new int[] { LI, DELAY }, new double[] { 1, 1, 2 },
				new double[][] { { 1, 1, 1 }, { 1, 1, 1 } });
		whatIf(three, ExactConstants.WHAT_IF_MIX, new double[] { 0.25, 0.5 }, 0, -1);
		assertThrows(InputDataException.class, () -> new SolverDispatcher().solve(three));
	}
}
