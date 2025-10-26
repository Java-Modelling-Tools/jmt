package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.Parameter;

public class PoissonPar implements Parameter {
    private double lambda;

    public PoissonPar(double lambda) throws IncorrectDistributionParameterException {
        this.lambda = lambda;
        if (lambda <= 0) {
            throw new IncorrectDistributionParameterException("Lambda must be greater than 0");
        }
    }

    public PoissonPar(Double lambda) throws IncorrectDistributionParameterException {
        this(lambda.doubleValue());
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) throws IncorrectDistributionParameterException {
        if (lambda <= 0) {
            throw new IncorrectDistributionParameterException("Lambda must be greater than 0");
        }
        this.lambda = lambda;
    }

    @Override
    public boolean check() {
        return lambda > 0;
    }

    @Override
    public void setMean(double value) throws IncorrectDistributionParameterException {
        if (value <= 0) {
            throw new IncorrectDistributionParameterException("Mean must be greater than 0");
        }
        this.lambda = value;
    }
}
