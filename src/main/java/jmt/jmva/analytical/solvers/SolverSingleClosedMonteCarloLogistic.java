package jmt.jmva.analytical.solvers;

import jmt.framework.data.ArrayUtils;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

/**
 * Solves a single class closed model, using Monte Carlo Logistic algorithm.
 * @author  Ong Wai Hong
 */
public class SolverSingleClosedMonteCarloLogistic extends Solver {

	public SolverMultiClosedMonteCarloLogistic solver;

	public static final boolean DEBUG = false;

	// number of customers or jobs in the system
	protected int customers = 0;

	protected int max_samples;
	protected int precision;
	protected double[][][] serviceTimes;
	protected double[][] custVisits;

	public SolverSingleClosedMonteCarloLogistic(int customers, int stations, int max_samples, int precision) throws InternalErrorException {
		this.customers = customers;
		this.stations = stations;
		this.max_samples = max_samples;
		this.precision = precision;
		initialiseSolver();

		name = new String[stations];
		type = new int[stations];

		throughput = new double[stations];
		queueLen = new double[stations];
		utilization = new double[stations];
		residenceTime = new double[stations];
	}

	private void initialiseSolver() throws InternalErrorException {
		int[] population = new int[1];
		population[0] = customers;
		solver = new SolverMultiClosedMonteCarloLogistic(1, stations, population, max_samples, precision);
	}

	public boolean hasSufficientProcessingCapacity() {
		// closed class: no saturation problem
		return true;
	}

	@Override
	public boolean input(String[] n, int[] t, double[][] s, double[] v) {
		serviceTimes = new double[stations][1][s[0].length];
		custVisits = new double[stations][1];
		if ((n.length != stations) || (t.length != stations) || (s.length != stations) || (v.length != stations)) {
			return false; // wrong input.
		}
		for (int i = 0; i < stations; i++) {
			if (t[i]==LD) {
				serviceTimes[i][0] = new double[s[i].length-1];
				for (int j = 0; j < s[i].length-1; j++)
					serviceTimes[i][0][j] = s[i][j+1]; /*HACK-countering Hack because adjustLD does awful things*/
			} else {
				serviceTimes[i][0] = new double[s[i].length];
				for (int j = 0; j < s[i].length; j++)
					serviceTimes[i][0][j] = s[i][j];
			}
			custVisits[i][0] = v[i];
		}
		return solver.input(t, serviceTimes, custVisits);
	}

	public void solve() {
		solver.solve();

		totUser = customers;
		totRespTime = solver.sysResponseTime;
		totThroughput = solver.sysThroughput;

		queueLen = ArrayUtils.extract1(solver.queueLen, 0);
		throughput = ArrayUtils.extract1(solver.throughput, 0);
		residenceTime = ArrayUtils.extract1(solver.residenceTime, 0);
		utilization = ArrayUtils.extract1(solver.utilization, 0);
	}

}
