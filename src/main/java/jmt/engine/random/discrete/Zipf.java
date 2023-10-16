package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.Parameter;

import java.util.Hashtable;

public class Zipf extends DiscreteDistribution {

	private double alpha;
	private int numberOfElements;

	private double Hmax;
	private boolean meanCalculated;
	private double mean;
	private boolean varCalculated;
	private double variance;


	// This constructor is used for method with outside `Parameter`
	// In XML the `distribution` and `distributionPar` initialize separately.
	public Zipf(){ super(); }

	// This constructor is used for directly assigning the parameter
	// In XML, assigning the distribution parameter through <subParameter>.
	public Zipf(double alpha, int numberOfElements) throws IncorrectDistributionParameterException{
		if ( numberOfElements<=0) {
			throw new IncorrectDistributionParameterException("`numberOfElements` should be positive.");
		}
		if ( alpha<=0) {
			throw new IncorrectDistributionParameterException("`alpha` should be positive.");
		}

		outdated();		// Set all cache flag false;
		this.alpha = alpha;
		this.numberOfElements = numberOfElements;
		this.Hmax = Harmonic(numberOfElements, alpha);
		this.cached = true;

		// Update and cache mean and variance.
		theorMean();
		theorVariance();
	}

	public Zipf(Double alpha, Integer numberOfElements) throws IncorrectDistributionParameterException{
		this(alpha.doubleValue(), numberOfElements.intValue());
	}
	public Zipf(Integer numberOfElements, Double alpha) throws IncorrectDistributionParameterException{
		this(alpha.doubleValue(), numberOfElements.intValue());
	}

	public boolean updatePar(double alpha, int numberOfElements)throws IncorrectDistributionParameterException{
		if ( numberOfElements<=0) {
			throw new IncorrectDistributionParameterException("`numberOfElements` should be positive.");
		}
		if ( alpha<=0) {
			throw new IncorrectDistributionParameterException("`alpha` should be positive.");
		}
		outdated();		// Set all cache flag false;
		this.alpha = alpha;
		this.numberOfElements = numberOfElements;
		this.Hmax = Harmonic(numberOfElements, alpha);
		this.cached = true;

		theorMean();
		theorVariance();

		return true;
	}

	@Override
	public boolean updatePar(Parameter p) throws IncorrectDistributionParameterException{
		if (p instanceof ZipfPar && p.check()){
			return updatePar(((ZipfPar) p).getAlpha(), ((ZipfPar) p).getNumberOfElements());
		}
		return false;
	}

	@Override
	public void outdated(){
		super.outdated();		// inform the super-calss to outdate the cache.
		this.varCalculated = false;
		this.meanCalculated = false;
	}

	@Override
	public int getUpper() {
		if(cached){
			return numberOfElements;
		}
		return -1;
	}

	@Override
	public int getlower() {
		if(cached){
			return 0;
		}
		return -1;
	}

	@Override
	public int nextRand(){
		// Use `cdf()` and upper/lower bound to calculate the next random variable.
		return this.inverseTransformSampling(0, numberOfElements);
	}

	@Override
	public double pmf(int x){
		if(cached){
			if (x <= 0 || x > numberOfElements) {
				return 0.0;
			}
			return 1/(Math.pow(x, alpha) * Hmax);
		}
		return -1.0;
	}

	@Override
	public double cdf(int x){
		if(cached) {
			if (x <= 0) {
				return 0.0;
			} else if (x >= numberOfElements) {
				return 1.0;
			}
			return Harmonic(x, alpha) / Hmax;
		}
		return -1.0;
	}

	@Override
	public double theorMean(){
		if(cached){
			if(!meanCalculated){
				this.mean = Harmonic(numberOfElements, alpha-1) / Hmax;
				this.meanCalculated = true;
			}
			return this.mean;
		}
		return -1.0;
	}

	@Override
	public double theorVariance(){
		if(cached){
			if(!varCalculated) {
				double hs1 = Harmonic(numberOfElements, alpha - 1);
				double hs2 = Harmonic(numberOfElements, alpha - 2);
				this.variance = hs2 / Hmax - (hs1 * hs1) / (Hmax * Hmax);
				this.varCalculated = true;
			}
			return this.variance;
		}
		return -1.0;
	}


	@Override
	public int nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p instanceof ZipfPar && p.check()) {
			ZipfPar up = (ZipfPar) p;
			int max = up.getNumberOfElements();

			// the Parameter p is used to call `cdf(int x, Parameter p)`
			Hashtable<Integer,Double> cdf_array = createdCDFList(0, max, p);
			return binarySearch(0, max, engine.nextDouble(), cdf_array);
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: Max must be a integer > 0\n" +
				"Error: Alpha must be (0, 100), to avoid the data oveflow\n" +
				"Error: the Parameter must be the `ZipfPar`");
		}
	}

	@Override
	public double pmf(int x, Parameter p) throws IncorrectDistributionParameterException {
		if (p instanceof ZipfPar && p.check()) {
			ZipfPar up = (ZipfPar) p;
			double alpha = up.getAlpha();
			int max = up.getNumberOfElements();

			if (x <= 0 || x > max) {
				return 0.0;
			}
			return 1/(Math.pow(x, alpha) * Harmonic(max, alpha));
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: Max must be a integer > 0\n" +
				"Error: Alpha must be (0, 100), to avoid the data oveflow\n" +
				"Error: the Parameter must be the `ZipfPar`");
		}
	}

	@Override
	public double cdf(int x, Parameter p) throws IncorrectDistributionParameterException {
		if (p instanceof ZipfPar && p.check()) {
			ZipfPar up = (ZipfPar) p;
			double alpha = up.getAlpha();
			int max = up.getNumberOfElements();

			if (x <= 0) {
				return 0.0;
			} else if (x >= max) {
				return 1.0;
			}
			return Harmonic(x, alpha) / Harmonic(max, alpha);
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: Max must be a integer > 0\n" +
				"Error: Alpha must be (0, 100), to avoid the data oveflow\n" +
				"Error: the Parameter must be the `ZipfPar`");
		}
	}

	@Override
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if (p instanceof ZipfPar && p.check()) {
			ZipfPar up = (ZipfPar) p;
			double alpha = up.getAlpha();
			int max = up.getNumberOfElements();

			return Harmonic(max, alpha-1) / Harmonic(max, alpha);
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: Max must be a integer > 0\n" +
				"Error: Alpha must be (0, 100), to avoid the data oveflow\n" +
				"Error: the Parameter must be the `ZipfPar`");
		}
	}

	@Override
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if (p instanceof ZipfPar && p.check()) {
			ZipfPar up = (ZipfPar) p;
			double alpha = up.getAlpha();
			int max = up.getNumberOfElements();

			double hs1 = Harmonic(max, alpha-1);
			double hs2 = Harmonic(max, alpha-2);
			double hmax = Harmonic(max, alpha);
			return hs2/hmax - (hs1 * hs1)/(hmax * hmax);
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: Max must be a integer > 0\n" +
				"Error: Alpha must be (0, 100), to avoid the data oveflow\n" +
				"Error: the Parameter must be the `ZipfPar`");
		}
	}


	private static double Harmonic(int num, double alpha){
		double total = 0.0;
		for(int i=1; i<=num; i++){
			total += 1 / Math.pow(i, alpha);
		}
		return total;
	}
}

