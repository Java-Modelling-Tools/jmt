package solvers.singleSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.singleSolverAlgorithms.SolverFixtures.*;

import org.junit.jupiter.api.Test;

import solvers.Solver;
import solvers.singleSolverAlgorithms.SolverFixtures.Algo;
import solvers.singleSolverAlgorithms.SolverFixtures.Model;

/**
 * Load-dependent stations. A multi-server station with c servers of mean service time S is an LD station with
 * service time S/min(n, c) at load n, birth-death rates mu(n) = min(n, c)/S. The customers think for Z at a delay.
 * For the machine-interference birth-death chain: p_n = p_{n-1} * ((N-n+1)/Z) / mu(n).
 */
class SingleClassLoadDependentTest {

	private static final double TOL = 1e-9;

	/** LD station with c servers (index 0 of the service array unused) and a delay of think time z. */
	private static Model multiServerWithDelay(int n, int c, double s, double z, boolean ldFirst) {
		double[] ld = multiServer(n, c, s);
		double[] dl = new double[n + 1];
		dl[0] = z;
		if (ldFirst) {
			return new Model(n, new int[] { Solver.LD, Solver.DELAY }, new double[][] { ld, dl }, new double[] { 1, 1 });
		}
		return new Model(n, new int[] { Solver.DELAY, Solver.LD }, new double[][] { dl, ld }, new double[] { 1, 1 });
	}

	/**
	 * N=2, c=2, S=1, Z=1: p1 = p0 * 2/1 = 2 p0, p2 = p1 * (1/1)/(2/1) = p0, so p = (1/4, 1/2, 1/4).
	 * X = sum mu(n) p_n = 1*1/2 + 2*1/4 = 1, Q_station = 1/2 + 2/4 = 1, Q_delay = X*Z = 1, R_station = Q/X = 1,
	 * utilisation (probability of being busy) = 1 - p0 = 3/4.
	 */
	@Test
	void twoServersTwoCustomers() throws Exception {
		for (boolean ldFirst : new boolean[] { true, false }) {
			Model m = multiServerWithDelay(2, 2, 1, 1, ldFirst);
			Solver s = solve(MVA, m);
			int ld = ldFirst ? 0 : 1;
			int dl = 1 - ld;
			assertEquals(1.0, s.getTotThroughput(), TOL, "X, LD first=" + ldFirst);
			assertEquals(1.0, s.getQueueLen(ld), TOL, "Q station");
			assertEquals(1.0, s.getQueueLen(dl), TOL, "Q delay");
			assertEquals(1.0, s.getResTime(ld), TOL, "R station");
			assertEquals(1.0, s.getResTime(dl), TOL, "R delay");
			assertEquals(0.75, s.getUtilization(ld), TOL, "U station");
			assertEquals(1.0, s.getThroughput(ld), TOL, "station throughput");
			assertPopulationIdentity("MVA", s, m, TOL);
		}
	}

	/**
	 * N=3, c=2, S=1, Z=1: p1 = 3 p0, p2 = p1 * 2/2 = 3 p0, p3 = p2 * 1/2 = 1.5 p0; sum 8.5 p0 = 1, p0 = 2/17.
	 * X = (1*3 + 2*3 + 2*1.5) p0 = 12 p0 = 24/17, Q_station = (3 + 6 + 4.5) p0 = 27/17, Q_delay = X*Z = 24/17,
	 * R_station = Q/X = 27/24, U = 1 - p0 = 15/17.
	 */
	@Test
	void twoServersThreeCustomers() throws Exception {
		Model m = multiServerWithDelay(3, 2, 1, 1, true);
		Solver s = solve(MVA, m);
		assertMeasures("MVA", s, 24.0 / 17, new double[] { 27.0 / 17, 24.0 / 17 }, new double[] { 27.0 / 24, 1.0 },
				new double[] { 15.0 / 17, Double.NaN }, TOL);
	}

	/** A single LD station holds every customer and runs at its full rate: X = mu(N) = N/S for N <= c. */
	@Test
	void singleLoadDependentStation() throws Exception {
		Model m = new Model(3, new int[] { Solver.LD }, new double[][] { multiServer(3, 2, 1) }, new double[] { 1 });
		Solver s = solve(MVA, m);
		assertEquals(2.0, s.getTotThroughput(), TOL, "X = mu(3) = 2");
		assertEquals(3.0, s.getQueueLen(0), TOL, "Q = N");
		assertEquals(1.5, s.getResTime(0), TOL, "R = N/X");
	}

	/** A one-server LD station (S at every load) is a load-independent queue: same as the repairman, X = 15/19. */
	@Test
	void oneServerLoadDependentEqualsLoadIndependent() throws Exception {
		Model ld = multiServerWithDelay(3, 1, 1, 2, true);
		Solver s = solve(MVA, ld);
		Solver li = solve(MVA, repairman(3, 1, 2));
		assertEquals(15.0 / 19, s.getTotThroughput(), TOL);
		assertEquals(li.getQueueLen(0), s.getQueueLen(0), TOL);
		assertEquals(li.getResTime(0), s.getResTime(0), TOL);
		assertEquals(li.getUtilization(0), s.getUtilization(0), TOL);
		assertEquals(li.getQueueLen(1), s.getQueueLen(1), TOL);
	}

	/** An LD station with at least N servers never queues: it is a delay. LD(c=3, S=2) + LI(D=1) = repairman(3, 1, 2). */
	@Test
	void enoughServersEqualsDelay() throws Exception {
		Model m = new Model(3, new int[] { Solver.LI, Solver.LD }, new double[][] { li(1), multiServer(3, 3, 2) }, new double[] { 1, 1 });
		Solver s = solve(MVA, m);
		double x = 15.0 / 19;
		assertMeasures("MVA", s, x, new double[] { 27.0 / 19, 30.0 / 19 }, new double[] { 1.8, 2.0 }, new double[] { x, Double.NaN }, TOL);
		assertPopulationIdentity("MVA", s, new Model(3, new int[] { Solver.LI, Solver.DELAY }, new double[][] { li(1), li(2) }, new double[] { 1, 1 }), TOL);
	}

	/** Two LD stations, both with 2 servers: symmetric, so Q = N/2 each and X = mu summed over the symmetric chain. */
	@Test
	void symmetricLoadDependentStations() throws Exception {
		// N=2, two stations each rate mu(1)=1, mu(2)=2. Product form: G = sum over (n1,n2), weight 1/(prod mu)^...
		// weights per station f(0)=1, f(1)=1, f(2)=1/2. States (2,0),(0,2): 1/2 each; (1,1): 1. Sum G(2) = 2.
		// G(1) = 2 (states (1,0),(0,1)). X = G(1)/G(2) = 1, Q_each = 1 by symmetry.
		double[] a = multiServer(2, 2, 1);
		Model m = new Model(2, new int[] { Solver.LD, Solver.LD }, new double[][] { a, a.clone() }, new double[] { 1, 1 });
		Solver s = solve(MVA, m);
		assertEquals(1.0, s.getTotThroughput(), TOL);
		assertEquals(1.0, s.getQueueLen(0), TOL);
		assertEquals(1.0, s.getQueueLen(1), TOL);
	}

	/** RECAL's single-class wrapper takes the same LD model; its multi-class core only handles LI and delay stations. */
	@Test
	void recalRejectsLoadDependentStations() throws Exception {
		Model m = multiServerWithDelay(2, 2, 1, 1, true);
		Solver s = RECAL.make(m.customers, m.stations());
		assertFalse(s.input(m.names, m.types, m.serv, m.visits));
	}
}
