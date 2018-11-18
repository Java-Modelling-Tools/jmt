package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Distributions;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import cern.jet.random.ChiSquare;
import cern.jet.random.engine.MersenneTwister64;
import jmt.engine.math.Gamma;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.util.Arrays;
import java.util.Date;

/**
 * Multivariate student t. Implemented with an embedded MVG and a chi-squared multiplier
 */
public class MultiVariateStudentT implements MultiVariateRealDistribution {

	private DoubleMatrix1D mean;
	private DoubleMatrix2D cov;
	private DoubleMatrix2D covInv;
	private double NC;
	private Algebra A;
	private int ndim;
	private int df;
	private ChiSquare ScalarX2;
	private MultiVariateGaussian MVG;

	/**
	 * Multivariate student t constructor. Covariance matrix needs to be square. Length of mean vector needs
	 * to be the same size as of covariance. degree of freedom needs to be positive integer
	 * @param mean double vector representing the mean
	 * @param cov covariance matrix
	 * @param df degree of freedom
	 * @param seed integer seed
	 * @throws InternalErrorException
	 */
	public MultiVariateStudentT(double[] mean, double[][] cov, int df, int seed) throws InternalErrorException {
		this.df = df;
		double[] zero_mean = new double[mean.length];
		Arrays.fill(zero_mean, 0.);
		MVG = new MultiVariateGaussian(zero_mean, cov, seed);
		this.mean = new DenseDoubleMatrix1D(mean);

		this.ndim = this.mean.size();
		Date date = new Date();
		this.ScalarX2 = new ChiSquare(df,  new MersenneTwister64(date.hashCode() + seed));
		A = new Algebra();

		this.cov = new DenseDoubleMatrix2D(cov);
		this.covInv = this.A.inverse(this.cov);

		this.NC = this.A.det(this.cov);
		this.NC *= Math.pow(Math.PI*df, ndim);
		this.NC = Math.pow(this.NC, 0.5);
		this.NC *= Gamma.gamma(0.5*df);
		this.NC /= Gamma.gamma(0.5*(df+ndim));
	}

	/**
	 * Constructor with default seed = 0
	 * @param mean mean vector
	 * @param cov covariance square array
	 * @param df degree of freedom
	 * @throws InternalErrorException
	 */
	public MultiVariateStudentT(double[] mean, double[][] cov, int df) throws InternalErrorException {
		this(mean, cov, df, 0);
	}

	/**
	 * Copy constructor with default seed = 0
	 * @param MVST
	 * @throws InternalErrorException
	 */
	public MultiVariateStudentT(MultiVariateStudentT MVST) throws InternalErrorException {
		this(MVST.mean.toArray(), MVST.cov.toArray(), MVST.df);
	}

	/**
	 * Copy constructor with specified seed
	 * @param MVST
	 * @param seed
	 * @throws InternalErrorException
	 */
	public MultiVariateStudentT(MultiVariateStudentT MVST, int seed) throws InternalErrorException {
		this(MVST.mean.toArray(), MVST.cov.toArray(), MVST.df, seed);
	}

	@Override
	public MultiVariateRealDistribution copy() throws InternalErrorException {
		return new MultiVariateStudentT(this);
	}

	@Override
	public MultiVariateRealDistribution copy(int seed) throws InternalErrorException {
		return new MultiVariateStudentT(this, seed);
	}

	public double[] getSample() {
		DoubleMatrix1D x = new DenseDoubleMatrix1D(MVG.getSample());
		double u = this.ScalarX2.nextDouble();
		u = u/df;
		u = Math.sqrt(u);
		x.assign(Functions.div(u));
		x.assign(this.mean, Functions.plus);
		return x.toArray();
	}

	public int getDim() {
		return ndim;
	}

	public double[] getMean() {
		return mean.toArray();
	}

	public double[][] getCovariance() {
		return cov.toArray();
	}

	public int getDF() {
		return df;
	}

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

		double p = this.A.mult(x,e);
		p /= df;
		p += 1;
		p = Math.pow(p, -.5*(df+ndim));
		p /= this.NC;
		return p;
	}

}
