package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.Parameter;

import java.util.function.Function;

public class ShiftedGeometric extends DiscreteDistribution {
    private double probability;

    // Constructor with NO parameter, because the XML initialized the distribution parameter through Parameter.
    public ShiftedGeometric() {
        super();
    }

    public ShiftedGeometric(double p) throws IncorrectDistributionParameterException {
        if (p < 0 || p > 1) {
            throw new IncorrectDistributionParameterException("The probability should belong to [0,1]");
        }
        outdated();
        this.probability = p;
        this.cached = true;
    }

    public ShiftedGeometric(Double p) throws IncorrectDistributionParameterException {
        this(p.doubleValue());
    }

    public boolean updatePar(double p) throws IncorrectDistributionParameterException {
        if (p < 0 || p > 1) {
            throw new IncorrectDistributionParameterException("The probability should belong to [0,1]");
        }
        outdated();
        this.probability = p;
        this.cached = true;
        return true;
    }

    @Override
    public boolean updatePar(Parameter p) throws IncorrectDistributionParameterException {
        if (p instanceof ShiftedGeometricPar && p.check()) {
            return updatePar(((ShiftedGeometricPar) p).getProbability());
        }
        return false;
    }

    @Override
    public int getUpper() {
        final int UPPER_BOUND_FACTOR = 100;
        return (int) (1 / probability * UPPER_BOUND_FACTOR);
    }

    @Override
    public int getlower() {
        return 1;
    }

    protected <T> T unboxParameter(Parameter p, Function<Double, T> function) throws IncorrectDistributionParameterException {
        if (p instanceof ShiftedGeometricPar && p.check()) {
            return function.apply(((ShiftedGeometricPar) p).getProbability());
        } else {
            throw new IncorrectDistributionParameterException(
                    "Error: The probability should belong to [0,1]\n" +
                            "Error: the Parameter must be the `ShiftedGeometricPar`");
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

    protected int nextRand(double probability) {
        return (int) Math.ceil(Math.log(engine.raw()) / Math.log(1 - probability));
    }

    @Override
    public int nextRand() {
        return checkCached(nextRand(probability));
    }

    @Override
    public int nextRand(Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, this::nextRand);
    }

    protected double pmf(double probability, int x) {
        if (x >= 1) {
            return probability * Math.pow(1 - probability, x - 1);
        } else {
            return 0.0;
        }
    }

    @Override
    public double pmf(int x) {
        return checkCached(pmf(probability, x));
    }

    @Override
    public double pmf(int x, Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, (probability) -> pmf(probability, x));
    }

    protected double cdf(double probability, int x) {
        if (x < 1) {
            return 0.0;
        } else {
            return 1 - Math.pow(1 - probability, x);
        }
    }

    @Override
    public double cdf(int x) {
        return checkCached(cdf(probability, x));
    }

    @Override
    public double cdf(int x, Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, (probability) -> cdf(probability, x));
    }

    protected double theorMean(double probability) {
        return 1 / probability;
    }

    @Override
    public double theorMean() {
        return checkCached(theorMean(probability));
    }

    @Override
    public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, this::theorMean);
    }

    protected double theorVariance(double probability) {
        return (1 - probability) / (probability * probability);
    }

    @Override
    public double theorVariance() {
        return checkCached(theorVariance(probability));
    }

    @Override
    public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
        return unboxParameter(p, this::theorVariance);
    }
}
