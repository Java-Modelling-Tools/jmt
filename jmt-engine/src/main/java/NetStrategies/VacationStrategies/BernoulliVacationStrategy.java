package NetStrategies.VacationStrategies;


import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.common.exception.NetException;
import NetStrategies.VacationStrategy;
import random.Distribution;
import random.Parameter;
import random.engine.MersenneTwister;
import random.engine.RandomEngine;

public class BernoulliVacationStrategy extends VacationWhenIdleStrategy {
    private final double p;

    private final RandomEngine rng = new MersenneTwister();

    public BernoulliVacationStrategy(Double p, Distribution vacationTimeDistr, Parameter vacationTimePar) throws NetException {
        super(MULTIPLE_VACATION, vacationTimeDistr, vacationTimePar);
        if (p < 0.0 || p > 1.0) {
            throw new NetException("Error in BernoulliVacationStrategy: Probability p must be in [0,1]");
        }
        this.p = p;
    }

    @Override
    public double nextVacationLength() throws NetException {
        return super.nextVacationLength();
    }

    public boolean nextRandBool() {
        return rng.raw() <= p;
    }
}
