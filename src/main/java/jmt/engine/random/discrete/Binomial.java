package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.math.Arithmetic;
import jmt.engine.random.Parameter;

import java.util.Hashtable;

public class Binomial extends DiscreteDistribution {

	private int n;
	private double p;

	// Constructor with NO parameter, because the XML initialized the distribution parameter through `Parameter`.
	public Binomial(){ super(); }

	public Binomial(double p, int n) throws IncorrectDistributionParameterException {
		if ( n<0) {
			throw new IncorrectDistributionParameterException("`numberOfExperiment` should be positive.");
		}
		if (p<0 || p>1) {
			throw new IncorrectDistributionParameterException("`probability` should be in range [0, 1]");
		}
		outdated();		// Set all cache flag false;
		this.p = p;
		this.n = n;
		this.cached = true;
	}

	// Because the input is number of items, and the input of Binomial distribution is number of experiments.
	public Binomial(Double p, Integer numberOfItems) throws IncorrectDistributionParameterException {
		this(p.doubleValue(), numberOfItems.intValue()-1);
	}
	public Binomial(Integer numberOfItems, Double p) throws IncorrectDistributionParameterException {
		this(p.doubleValue(), numberOfItems.intValue()-1);
	}

	public boolean updatePar(double p, int n)throws IncorrectDistributionParameterException{
		if ( n<0) {
			throw new IncorrectDistributionParameterException("`numberOfExperiment` should be positive.");
		}
		if (p<0 || p>1) {
			throw new IncorrectDistributionParameterException("`probability` should be in range [0, 1]");
		}
		outdated();		// Set all cache flag false;
		this.p = p;
		this.n = n;
		this.cached = true;
		return true;
	}

	@Override
	public boolean updatePar(Parameter p) throws IncorrectDistributionParameterException{
		if (p instanceof BinomialPar && p.check()){
			return updatePar(((BinomialPar) p).getProbability(), ((BinomialPar) p).getNumberOfExperiment());
		}
		return false;
	}

	@Override
	public int getUpper() {
		if(cached){
			return n;
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
	// Because the input is number of items, and the input of Binomial distribution is number of experiments.
	// when nextRand() called, we need to the next item's id, so plus 1.
	public int nextRand() {
		return inverseTransformSampling(0, n)+1;
	}

	@Override
	public double pmf(int x) {
		if(cached){
			if( x<0 || x>n ){
				return 0.0;
			}
			return binomial_pmf(n, x, p);
		}
		return -1.0;
	}

	@Override
	public double cdf(int x) {
		if(cached){
			if (x < 0) {
				return 0.0;
			} else if (x >= n) {
				return 1.0;
			}
			double result = 0.0;
			for(int i=0; i<=x; i++){
				result += binomial_pmf(n, i, p);
			}
			return result;
		}
		return -1.0;
	}

	@Override
	public double theorMean() {
		if(cached){
			return p * n;
		}
		return -1.0;
	}

	@Override
	public double theorVariance() {
		if(cached){
			return n * p * (1-p);
		}
		return -1.0;
	}

	@Override
	// Because the input is number of items, and the input of Binomial distribution is number of experiments.
	// when nextRand() called, we need to the next item's id, so plus 1.
	public int nextRand(Parameter par) throws IncorrectDistributionParameterException {
		if (par instanceof BinomialPar && par.check()) {
			BinomialPar up = (BinomialPar) par;
			int num = up.getNumberOfExperiment();
			double p = up.getProbability();

			Hashtable<Integer,Double> lst = new Hashtable<Integer,Double>();
			double CDF = 0.0;
			for(int i=0; i<=num; i++){
				CDF += binomial_pmf(num, i, p);
				lst.put(i, CDF);
			}
			return binarySearch(0, num, engine.nextDouble(), lst)+1;
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: the Parameter must be the `BinomialPar`\n" +
				"Error: numberOfExperiment must be a integer > 0\n" +
				"Error: Probability must be in range [0, 1]");
		}
	}

	@Override
	public double pmf(int x, Parameter par) throws IncorrectDistributionParameterException {
		if (par instanceof BinomialPar && par.check()) {
			BinomialPar up = (BinomialPar) par;
			int num = up.getNumberOfExperiment();
			double p = up.getProbability();

			if( x<0 || x>num ){
				return 0.0;
			}
			return binomial_pmf(num, x, p);
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: the Parameter must be the `BinomialPar`\n" +
				"Error: numberOfExperiment must be a integer > 0\n" +
				"Error: Probability must be in range [0, 1]");
		}
	}

	@Override
	public double cdf(int x, Parameter par) throws IncorrectDistributionParameterException {
		if (par instanceof BinomialPar && par.check()) {
			BinomialPar up = (BinomialPar) par;
			int num = up.getNumberOfExperiment();
			double p = up.getProbability();

			if (x < 0) {
				return 0.0;
			} else if (x >= num) {
				return 1.0;
			}
			double result = 0.0;
			for(int i=0; i<=x; i++){
				result += binomial_pmf(num, i, p);
			}
			return result;
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: the Parameter must be the `BinomialPar`\n" +
				"Error: numberOfExperiment must be a integer > 0\n" +
				"Error: Probability must be in range [0, 1]");
		}
	}

	@Override
	public double theorMean(Parameter par) throws IncorrectDistributionParameterException {
		if (par instanceof BinomialPar && par.check()) {
			BinomialPar up = (BinomialPar) par;
			return up.getProbability() * up.getNumberOfExperiment();
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: the Parameter must be the `BinomialPar`\n" +
				"Error: numberOfExperiment must be a integer > 0\n" +
				"Error: Probability must be in range [0, 1]");
		}
	}

	@Override
	public double theorVariance(Parameter par) throws IncorrectDistributionParameterException {
		if (par instanceof BinomialPar && par.check()) {
			BinomialPar up = (BinomialPar) par;
			double num = (double) up.getNumberOfExperiment();
			double p = up.getProbability();
			return num * p * (1-p);
		} else {
			throw new IncorrectDistributionParameterException(
				"Error: the Parameter must be the `BinomialPar`\n" +
				"Error: numberOfExperiment must be a integer > 0\n" +
				"Error: Probability must be in range [0, 1]");
		}
	}

	private static double binomial_pmf(int n, int x, double p){
		return Arithmetic.binomial(n, x) * Math.pow(p, x) * Math.pow((1-p), (n-x));
	}
}
