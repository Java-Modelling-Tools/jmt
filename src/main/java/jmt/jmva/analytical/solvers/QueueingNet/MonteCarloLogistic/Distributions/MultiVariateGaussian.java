package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Distributions;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.linalg.CholeskyDecomposition;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.random.*;
import cern.jet.random.engine.MersenneTwister64;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.util.Date;

/**
 * Multivariate Gaussian distribution. Implements the MVRD interface.
 * Specified by a mean vector, covariance matrix and optionally an integer seed
 */
public class MultiVariateGaussian implements MultiVariateRealDistribution {

	private DoubleMatrix1D mean;
	private DoubleMatrix2D cov;
	private DoubleMatrix2D covInv;
	private double NC;
	private DoubleMatrix2D L;
	private Algebra A;
	private int ndim;
	private Normal ScalarGaussian;

	/**
	 * Full constructor. Requires that covariance is square, and mean has same length as rows/cols on
	 * covariance. Requires that the covariance is symmetric, and positive definite
	 * @param mean double array representing vector of mean
	 * @param cov covariance matrix
	 * @param seed integer seed
	 * @throws InternalErrorException
	 */
	public MultiVariateGaussian(double[] mean, double[][] cov, int seed) throws InternalErrorException {
		this.A = new Algebra();
		try {
			this.mean = new DenseDoubleMatrix1D(mean);
			this.cov = new DenseDoubleMatrix2D(cov);
		} catch (IllegalArgumentException e) {
			throw new InternalErrorException("Covariance needs to be a rectangular 2D array!");
		}

		if (this.cov.rows() != this.cov.columns())
			throw new InternalErrorException("Covariance matrix needs to be square!");
		if ((this.mean.size() != this.cov.rows()) || (this.mean.size() != this.cov.columns())) 
			throw new InternalErrorException("Mean needs to be the same size as the length of covariance matrix!");

		CholeskyDecomposition C = new CholeskyDecomposition(this.cov);
		this.L = C.getL();
		if (!C.isSymmetricPositiveDefinite()) 
			throw new InternalErrorException("Supplied covariance matrix not symmetric and positive definite!");

		this.ndim = this.mean.size();

		Date date = new Date();
		this.ScalarGaussian = new Normal(0, 1, new MersenneTwister64(date.hashCode() + seed));

		this.NC = this.A.det(this.cov);
		this.NC *= Math.pow(2*Math.PI, ndim);
		this.NC = Math.pow(this.NC, 0.5);

		this.covInv = this.A.inverse(this.cov);
	}

	/**
	 * Constructor MVG with default seed = 0
	 * @param mean mean vector
	 * @param cov covariance
	 * @throws InternalErrorException
	 */
	public MultiVariateGaussian(double[] mean, double[][] cov) throws InternalErrorException {
		this(mean, cov, 0);
	}

	/**
	 * Copy constructor
	 * @param MVG MVG to copy from
	 * @throws InternalErrorException
	 */
	public MultiVariateGaussian(MultiVariateGaussian MVG) throws InternalErrorException {
		this(MVG.mean.toArray(), MVG.cov.toArray());
	}

	/**
	 * Copy constructor with different seed
	 * @param MVG MVG to copy from
	 * @param seed specified integer seed
	 * @throws InternalErrorException
	 */
	public MultiVariateGaussian(MultiVariateGaussian MVG, int seed) throws InternalErrorException {
		this(MVG.mean.toArray(), MVG.cov.toArray(), seed);
	}

	@Override
	public MultiVariateRealDistribution copy() throws InternalErrorException {
		return new MultiVariateGaussian(this);
	}

	@Override
	public MultiVariateRealDistribution copy(int seed) throws InternalErrorException {
		return new MultiVariateGaussian(this, seed);
	}

	@Override
	public int getDim() {
		return this.ndim;
	}

	public double[] getMean() {
		return mean.toArray();
	}

	public double[][] getCovariance() {
		return cov.toArray();
	}

	@Override
	public double pdf(double[] point) throws InternalErrorException {
		if (ndim==0)
			return 1.0;

		DoubleMatrix1D x = new DenseDoubleMatrix1D(point);

		for (int i=0; i<this.ndim; i++) {
			x.set(i, x.get(i)-this.mean.get(i));
		}

		if (x.size() != this.ndim)
			throw new InternalErrorException("Dimensionality of point needs to be same as distribution!");

		DoubleMatrix1D e = this.A.mult(this.covInv, x);

		double p = -0.5*this.A.mult(x,e);
		p = Math.exp(p);
		p /= this.NC;
		return p;
	}

	@Override
	public double[] getSample() {
		if (ndim==0)
			return new double[] {};

		DoubleMatrix1D sample = new DenseDoubleMatrix1D(this.ndim);

		for (int i=0; i<this.ndim; i++) {
			sample.set(i, this.ScalarGaussian.nextDouble());
		}

		sample = this.A.mult(this.L, sample);

		for (int i=0; i<this.ndim; i++) {
			sample.set(i, this.mean.get(i) + sample.get(i));
		}

		return sample.toArray();
	}

}
