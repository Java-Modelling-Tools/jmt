package solvers.multiSolverAlgorithms;

import analytical.ExactConstants;
import analytical.ExactModel;
import solvers.SolverAlgorithm;
import solvers.dispatchers.SolverMultiDispatcher;

/** Builds an {@link ExactModel} and solves it through {@link SolverMultiDispatcher}, the path production uses. */
public final class MultiFixtures {

	public static final int DELAY = ExactConstants.STATION_DELAY;
	public static final int LI = ExactConstants.STATION_LI;
	public static final int PRS = ExactConstants.STATION_PRS;
	public static final int CLOSED = ExactConstants.CLASS_CLOSED;
	public static final int OPEN = ExactConstants.CLASS_OPEN;

	private MultiFixtures() {
	}

	/** Solved measures, all [station][class]. */
	public static final class Result {
		public final double[][] q, x, r, u;

		Result(ExactModel m, SolverAlgorithm alg) {
			int s = m.getStations(), c = m.getClasses();
			q = new double[s][c];
			x = new double[s][c];
			r = new double[s][c];
			u = new double[s][c];
			for (int i = 0; i < s; i++) {
				for (int j = 0; j < c; j++) {
					q[i][j] = m.getQueueLen(alg)[i][j][0];
					x[i][j] = m.getThroughput(alg)[i][j][0];
					r[i][j] = m.getResidTimes(alg)[i][j][0];
					u[i][j] = m.getUtilization(alg)[i][j][0];
				}
			}
		}

		public double classQ(int c) {
			double t = 0;
			for (double[] row : q) {
				t += row[c];
			}
			return t;
		}
	}

	/**
	 * Model with unit visits. demand[station][class] is the service time; classData is the population (closed)
	 * or the arrival rate (open).
	 */
	public static ExactModel model(int[] stationTypes, int[] classTypes, double[] classData, double[][] demand) {
		int s = stationTypes.length, c = classTypes.length;
		ExactModel m = new ExactModel();
		m.resize(s, c);
		m.setStationTypes(stationTypes);
		m.setClassTypes(classTypes);
		// resize() does not resize the priority array, so a model with more than one class needs it set
		m.setClassPriorities(new int[c]);
		m.setClassData(classData);
		int pop = 0;
		for (int j = 0; j < c; j++) {
			pop += classTypes[j] == CLOSED ? (int) classData[j] : 0;
		}
		double[][][] st = new double[s][c][];
		double[][] v = new double[s][c];
		for (int i = 0; i < s; i++) {
			for (int j = 0; j < c; j++) {
				// a load-dependent station takes one service time per customer; here a constant rate
				st[i][j] = new double[stationTypes[i] == ExactConstants.STATION_LD ? Math.max(1, pop) : 1];
				java.util.Arrays.fill(st[i][j], demand[i][j]);
				v[i][j] = 1;
			}
		}
		m.setServiceTimes(st);
		m.setVisits(v);
		m.resetResults();
		return m;
	}

	public static ExactModel closed(int[] stationTypes, double[] pops, double[][] demand) {
		int[] t = new int[pops.length];
		return model(stationTypes, t, pops, demand);
	}

	public static Result solve(ExactModel m, SolverAlgorithm alg) throws Exception {
		m.setAlgorithmType(alg);
		SolverMultiDispatcher.solveMulti(m, 0);
		return new Result(m, alg);
	}
}
