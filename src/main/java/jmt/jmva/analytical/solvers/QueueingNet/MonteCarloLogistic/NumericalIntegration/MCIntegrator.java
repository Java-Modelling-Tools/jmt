package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration;

import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Distributions.MultiVariateRealDistribution;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.math.MathContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Monte Carlo Integrator object. Takes in a function, a distribution, and a maximum number of samples to
 * perform MC Integration. Requires that the dimensionality of the distribution equals to the dimensionality
 * of the integrated function. Will throw an error when compute() is called if dimensions don't match
 */
public class MCIntegrator implements Integrator {

	private Function function;
	private MultiVariateRealDistribution dist;
	private int max_num_samples;
	private MathContext MC;
	private int nthread;
	private List<Future<BigDecimal>> results;
	private int[] weights;
	private ExecutorService executor;

	public MCIntegrator(Function function, MultiVariateRealDistribution dist, int max_num_samples) throws InternalErrorException {
		this(function, dist, max_num_samples, MathContext.DECIMAL128);
	}

	public MCIntegrator(Function function, MultiVariateRealDistribution dist, int max_num_samples, MathContext MC) throws InternalErrorException {
		this(function, dist, max_num_samples, MC, 1);
	}

	/**
	 * Full constructor
	 * @param function Function to integrate over
	 * @param dist Distribution to integrate function with
	 * @param max_num_samples maximum number of samples to integrate
	 * @param MC MathContext object to perform BigDecimal arithmetic with
	 * @param nthread number of threads to perform the integration
	 * @throws InternalErrorException
	 */
	public MCIntegrator(Function function, MultiVariateRealDistribution dist, int max_num_samples, MathContext MC, int nthread) throws InternalErrorException {
		this.function = function;
		this.dist = dist;
		this.max_num_samples = max_num_samples;
		this.MC = MC;
		this.nthread = nthread;
		if (max_num_samples < nthread)
			this.nthread = max_num_samples;
		executor = Executors.newFixedThreadPool(nthread);
	}

	@Override
	public void initialise(Function function) {
		this.function = function;
	}

	/**
	 * Initialize the threads by calculating the number of samples per thread, and copying the distribution.
	 * Each thread samples from its own copy of the distribution with a seed equal to the thread number
	 * @throws InternalErrorException
	 */
	private void initialize_threads() throws InternalErrorException {
		results = new ArrayList<>(nthread);
		weights = new int[nthread];
		int N = max_num_samples/nthread;
		int R = max_num_samples%nthread;
		for (int i=0; i<nthread; i++) {
			int num = N;
			if (R>0) {num++; R--;}
			weights[i] = num;
			results.add(executor.submit(new SamplingTask(dist, function.copy(), num, MC, i)));
		}
	}

	private void kill_threads() {
		executor.shutdown();
	}

	/**
	 * Fetches the results from the individual threads, and combines it to give the final answer
	 * @return
	 * @throws InternalErrorException
	 */
	@Override
	public BigDecimal compute() throws InternalErrorException {
		initialize_threads();
		BigDecimal I = BigDecimal.ZERO;
		BigDecimal R = BigDecimal.ZERO;
		try {
			for (int i = 0; i < nthread; i++) {
				Future<BigDecimal> result = results.get(i);
				R = result.get();
				R = R.multiply(new BigDecimal(weights[i]), MC);
				I = I.add(R, MC);
			}
			I = I.divide(new BigDecimal(max_num_samples), MC);
		} catch (ExecutionException | InterruptedException e) {
			System.out.print("Error! Last sampled point was: ");
			System.out.println(R.toString());
			e.printStackTrace();
			throw new InternalErrorException("Unexpected exception! Please see error trace for details.");
		}
		kill_threads();
		return I;
	}

	/**
	 * Self-contained task which carries out Monte Carlo Sampling and Summation, in its own thread
	 * Recombined later
	 */
	private class SamplingTask implements Callable<BigDecimal> {

		private MultiVariateRealDistribution distTCommon; // points to a common dist
		private MultiVariateRealDistribution distTPrivate; // private dist for computing pdf
		private Function functionT;
		private int num_samplesT;
		private MathContext MCT;
		private int tnum;

		public BigDecimal call() throws InternalErrorException {
			return computeT();
		}

		public SamplingTask(MultiVariateRealDistribution d, Function f, int n, MathContext MC, int num) throws InternalErrorException {
			tnum = num;
			distTCommon = d;
			distTPrivate = d.copy(tnum);
			functionT = f;
			num_samplesT = n;
			MCT = MC;
		}

		private BigDecimal computeT() throws InternalErrorException {
			BigDecimal I = BigDecimal.ZERO;
			int J = 0;
			BigDecimal f;
			BigDecimal phi;
			double[] x;
			for (int i=0; i<num_samplesT; i++) {
				x = this.getSampleT();
				f = functionT.evaluate(x);
				phi = new BigDecimal(distTPrivate.pdf(x));
				f = f.divide(phi, MCT);

				I = I.add(f, MCT);

				J++;
			}
			return I.divide(new BigDecimal(J), MCT);
		}

		synchronized
		private double[] getSampleT() { return distTPrivate.getSample(); }

	}

}
