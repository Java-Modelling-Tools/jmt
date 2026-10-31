package solvers.singleSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import solvers.Solver;
import solvers.SolverSingleClosedCoMoM;
import solvers.SolverSingleClosedMonteCarloLogistic;
import solvers.SolverSingleClosedRECAL;

/** Small single-class closed models and a uniform way to run every single-class algorithm on them. */
final class SolverFixtures {

	private SolverFixtures() {
	}

	/** A single-class closed model in the layout of {@link Solver#input}. */
	static final class Model {
		final int customers;
		final String[] names;
		final int[] types;
		final double[][] serv;
		final double[] visits;

		Model(int customers, int[] types, double[][] serv, double[] visits) {
			this.customers = customers;
			this.types = types;
			this.serv = serv;
			this.visits = visits;
			this.names = new String[types.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = "st" + i;
			}
		}

		int stations() {
			return types.length;
		}
	}

	/** Load-independent or delay station with service time s. */
	static double[] li(double s) {
		return new double[] { s };
	}

	/** Model with only LI/DELAY stations: types[i], service time s[i], unit visits. */
	static Model liModel(int n, int[] types, double... s) {
		double[][] serv = new double[s.length][];
		double[] v = new double[s.length];
		for (int i = 0; i < s.length; i++) {
			serv[i] = li(s[i]);
			v[i] = 1;
		}
		return new Model(n, types, serv, v);
	}

	/** K identical queueing stations of demand d. */
	static Model balanced(int n, int k, double d) {
		int[] t = new int[k];
		double[] s = new double[k];
		for (int i = 0; i < k; i++) {
			t[i] = Solver.LI;
			s[i] = d;
		}
		return liModel(n, t, s);
	}

	/** Queueing station of demand d (index 0) plus delay station of think time z (index 1). */
	static Model repairman(int n, double d, double z) {
		return liModel(n, new int[] { Solver.LI, Solver.DELAY }, d, z);
	}

	/** Service time array of an LD station with c servers of mean service time s: s / min(n, c), index 0 unused. */
	static double[] multiServer(int n, int c, double s) {
		double[] r = new double[n + 1];
		for (int j = 1; j <= n; j++) {
			r[j] = s / Math.min(j, c);
		}
		return r;
	}

	/** A solving strategy for single-class closed models. */
	interface Algo {
		String name();

		Solver make(int n, int k) throws Exception;

		/** Absolute tolerance achievable by the algorithm on well-conditioned small models. */
		double tol();
	}

	private static Algo algo(String name, double tol, Factory f) {
		return new Algo() {
			public String name() {
				return name;
			}

			public Solver make(int n, int k) throws Exception {
				return f.make(n, k);
			}

			public double tol() {
				return tol;
			}

			public String toString() {
				return name;
			}
		};
	}

	private interface Factory {
		Solver make(int n, int k) throws Exception;
	}

	// CoMoM and RECAL scale service times by 1e5 and truncate them to int, hence the 1e-5 tolerance.
	static final Algo MVA = algo("MVA", 1e-9, SolverSingleClosedMVA::new);
	static final Algo CONVOLUTION = algo("Convolution", 1e-9, SolverSingleClosedExact::new);
	static final Algo COMOM = algo("CoMoM", 1e-5, SolverSingleClosedCoMoM::new);
	static final Algo RECAL = algo("RECAL", 1e-5, SolverSingleClosedRECAL::new);
	static final Algo MONTE_CARLO = algo("MonteCarloLogistic", 0.05,
			(n, k) -> new SolverSingleClosedMonteCarloLogistic(n, k, 20000, 32));

	/** Algorithms that are correct on models with delay stations and visits (the others are disabled bug tests). */
	static final Algo[] WORKING = { MVA, RECAL };

	/** Algorithms that are correct on load-independent queueing stations with unit visits. */
	static final Algo[] LI_ONLY = { MVA, CONVOLUTION, RECAL };

	/** Builds, feeds and solves; the caller reads the results from the returned solver. */
	static Solver solve(Algo a, Model m) throws Exception {
		Solver s = a.make(m.customers, m.stations());
		assertTrue(s.input(m.names, m.types, m.serv, m.visits), a.name() + ": input rejected");
		s.solve();
		if (s instanceof SolverSingleClosedExact) {
			((SolverSingleClosedExact) s).indices();
		}
		return s;
	}

	/**
	 * The system throughput expectation to pass to assertMeasures: the CoMoM/RECAL wrappers report NaN
	 * (see SingleClassBugsTest), so it is skipped for them and the per-station throughputs are checked instead.
	 */
	static double sysX(Algo a, double x) {
		return a == RECAL || a == COMOM ? Double.NaN : x;
	}

	/** Expected per-station results of a closed form; null arrays and NaN entries are not checked. */
	static void assertMeasures(Algo a, Solver s, double x, double[] q, double[] r, double[] u) {
		assertMeasures(a.name(), s, x, q, r, u, a.tol());
	}

	/** As above; a NaN expected x skips the system throughput check. */
	static void assertMeasures(String who, Solver s, double x, double[] q, double[] r, double[] u, double tol) {
		if (!Double.isNaN(x)) {
			assertEquals(x, s.getTotThroughput(), tol, who + " system throughput");
		}
		for (int i = 0; i < q.length; i++) {
			assertEquals(q[i], s.getQueueLen(i), tol, who + " queue length of station " + i);
			assertTrue(!Double.isNaN(s.getQueueLen(i)) && s.getQueueLen(i) >= -tol, who + " Q[" + i + "] is sane");
		}
		if (r != null) {
			for (int i = 0; i < r.length; i++) {
				if (Double.isNaN(r[i])) {
					continue;
				}
				assertEquals(r[i], s.getResTime(i), tol, who + " residence time of station " + i);
			}
		}
		if (u != null) {
			for (int i = 0; i < u.length; i++) {
				if (Double.isNaN(u[i])) {
					continue;
				}
				assertEquals(u[i], s.getUtilization(i), tol, who + " utilisation of station " + i);
			}
		}
	}

	/** sum(Q) == N - X*Z where Z is the total demand of the delay stations. */
	static void assertPopulationIdentity(String who, Solver s, Model m, double tol) {
		double z = 0;
		double sumQ = 0;
		for (int i = 0; i < m.stations(); i++) {
			sumQ += s.getQueueLen(i);
			if (m.types[i] == Solver.DELAY) {
				z += m.serv[i][0] * m.visits[i];
			}
		}
		// system throughput from the first station with visits (RECAL/CoMoM report NaN for getTotThroughput)
		double x = Double.NaN;
		for (int i = 0; i < m.stations() && Double.isNaN(x); i++) {
			if (m.visits[i] > 0) {
				x = s.getThroughput(i) / m.visits[i];
			}
		}
		// delay stations carry X*Z customers, so sum over queueing stations = N - X*Z
		double queueing = 0;
		for (int i = 0; i < m.stations(); i++) {
			if (m.types[i] != Solver.DELAY) {
				queueing += s.getQueueLen(i);
			}
		}
		assertEquals(m.customers - x * z, queueing, tol, who + " sum(Q_queueing) = N - X*Z");
		assertEquals(m.customers, sumQ, tol, who + " sum(Q) = N");
	}
}
