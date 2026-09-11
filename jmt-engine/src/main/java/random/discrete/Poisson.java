package random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import random.Parameter;

import java.util.function.Function;

import static math.Arithmetic.factorial;

public class Poisson extends DiscreteDistribution {
    private double lambda;

    // Constructor with NO parameter, because the XML initialized the distribution parameter through Parameter.
    public Poisson() {
        super();
    }

    public Poisson(double lambda) throws IncorrectDistributionParameterException {
        if (lambda <= 0) {
            throw new IncorrectDistributionParameterException("lambda should be positive.");
        }
        this.lambda = lambda;
        outdated();
        this.cached = true;
    }

    public Poisson(Double lambda) throws IncorrectDistributionParameterException {
        this(lambda.doubleValue());
    }

    public boolean updatePar(double lambda) throws IncorrectDistributionParameterException {
        if (lambda <= 0) {
            throw new IncorrectDistributionParameterException("lambda should be positive.");
        }
        outdated();
        this.lambda = lambda;
        this.cached = true;
        return true;
    }

    @Override
    public boolean updatePar(Parameter p) throws IncorrectDistributionParameterException {
        if (p instanceof PoissonPar && p.check()) {
            return updatePar(((PoissonPar) p).getLambda());
        }
        return false;
    }

    @Override
    public int getUpper() {
        final int UPPER_BOUND_FACTOR = 100;
        return (int) (lambda * UPPER_BOUND_FACTOR);
    }

    @Override
    public int getlower() {
        return 0;
    }

    protected <T> T unboxParameter(Parameter p, Function<Double, T> function) throws IncorrectDistributionParameterException {
        if (p instanceof PoissonPar && p.check()) {
            return function.apply(((PoissonPar) p).getLambda());
        } else {
            throw new IncorrectDistributionParameterException(
                    "Error: lambda should be greater than zero\n" +
                            "Error: the Parameter must be the `PoissonPar`");
        }
    }

    protected int checkCached(int value) {
        if (cached) {
            return value;
        }
        return -1;
    }

    protected double checkCached(double value) {
        if (cached) {
            return value;
        }
        return -1.0;
    }

    protected int nextRand(double lambda) {
        // Knuth's algorithm for Poisson distribution
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;
        do {
            ++k;
            p *= engine.raw();
        } while (p > L);
        return k - 1;
    }

    @Override
    public int nextRand() {
        return checkCached(nextRand(lambda));
    }

    @Override
    public int nextRand(Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, this::nextRand);
    }

    protected double pmf(double lambda, int x) {
        if (x < 0) {
            return 0.0;
        }
        return Math.pow(lambda, x) * Math.exp(-lambda) / factorial(x);
    }

    @Override
    public double pmf(int x) {
        return checkCached(pmf(lambda, x));
    }

    @Override
    public double pmf(int x, Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, lambdaValue -> pmf(lambdaValue, x));
    }

    protected double cdf(double lambda, int x) {
        if (x < 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (int k = 0; k <= x; k++) {
            sum += pmf(lambda, k);
        }
        return sum;
    }

    @Override
    public double cdf(int x) {
        return checkCached(cdf(lambda, x));
    }

    @Override
    public double cdf(int x, Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, lambdaValue -> cdf(lambdaValue, x));
    }

    protected double theorMean(double lambda) {
        return lambda;
    }

    @Override
    public double theorMean() {
        return checkCached(theorMean(lambda));
    }

    @Override
    public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, this::theorMean);
    }

    protected double theorVariance(double lambda) {
        return lambda;
    }

    @Override
    public double theorVariance() {
        return checkCached(theorVariance(lambda));
    }

    @Override
    public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, this::theorVariance);
    }
}
