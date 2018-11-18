package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Distributions;

import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import cern.jet.random.Uniform;

import java.util.Date;

/**
 * Multivariate uniform distribution with specifiable lower and upper bounds per dimension
 */
public class MultiVariateUniform implements MultiVariateRealDistribution {

	private int ndim;
	private double p;
	private Uniform[] distributions;
	private double[] lb;
	private double[] ub;

	/**
	 * Constructor for the Multivariate Uniform distribution. Accepts two double arrays representing
	 * the lower and upper bounds, for each dimension
	 * @param ndim number of dimensions of distribution
	 * @param lb lower bound vector
	 * @param ub upper bound vector
	 * @param seed integer seed
	 * @throws InternalErrorException
	 */
	public MultiVariateUniform(int ndim, double[] lb, double[] ub, int seed) throws InternalErrorException {
		if (lb.length != ndim || ub.length != ndim)
			throw new InternalErrorException("Distribution parameters and dimensions don't agree!");

		double V = 1;
		distributions = new Uniform[ndim];
		this.lb = new double[ndim];
		this.ub = new double[ndim];

		for (int i=0; i<ndim; i++) {
			if (lb[i] > ub[i]) throw new InternalErrorException("Invalid bounds for uniform distribution!");
			V = V*(ub[i]-lb[i]);
			Date date = new Date();
			distributions[i] = new Uniform(lb[i], ub[i], date.hashCode()+seed);
			this.lb[i] = lb[i];
			this.ub[i] = ub[i];
		}

		this.ndim = ndim;
		this.p = 1./V;
	}

	/**
	 * Constructor with default seed = 0
	 * @param ndim
	 * @param lb
	 * @param ub
	 * @throws InternalErrorException
	 */
	public MultiVariateUniform(int ndim, double[] lb, double[] ub) throws InternalErrorException {
		this(ndim, lb, ub, 0);
	}

	/**
	 * Copy constructor
	 * @param MVU
	 * @throws InternalErrorException
	 */
	public MultiVariateUniform(MultiVariateUniform MVU) throws InternalErrorException {
		this(MVU.ndim, MVU.lb, MVU.ub);
	}

	/**
	 * Copy constructor with specified seed
	 * @param MVU
	 * @param seed
	 * @throws InternalErrorException
	 */
	public MultiVariateUniform(MultiVariateUniform MVU, int seed) throws InternalErrorException {
		this(MVU.ndim, MVU.lb, MVU.ub, seed);
	}

	@Override
	public int getDim() {
		return this.ndim;
	}

	@Override
	public double pdf(double[] point) {
		return this.p;
	}

	@Override
	public double[] getSample() {
		double[] sample = new double[ndim];
		for (int i=0; i<ndim; i++) {
			sample[i] = this.distributions[i].nextDouble();
		}
		return sample;
	}

	@Override
	public MultiVariateRealDistribution copy() throws InternalErrorException {
		return new MultiVariateUniform(this);
	}

	@Override
	public MultiVariateRealDistribution copy(int seed) throws InternalErrorException {
		return new MultiVariateUniform(this, seed);
	}

}
