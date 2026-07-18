package jmt.jmva.analytical.solvers.priority.prioMVACalculators;

import jmt.jmva.analytical.solvers.priority.interpolators.Interpolator;
import jmt.jmva.analytical.solvers.priority.interpolators.LinearInterpolator;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;

public abstract class InterpolatedPrioMVAUtilCalculator extends BasePrioMVAUtilCalculator {

    /**
     * Allows for a generalisation of the CL version of PrioMVA so instead of
     * just linear interpolation any interpolation can be used
     */

    /**
     * y_2 can equal 0
     * (when the queue length rounds up to population of the class so
     * N_class - x2 = 0 and the utilisation at 0 is 0)
     * and in this situation,
     * the following ints decide what should be done
     */
    protected static final int NO_CHANGE = 0;
    protected static final int RETURN_Y_ONE = 1;
    protected static final int RETURN_ZERO = 2;
    protected static final int LINEAR_INTERPOLATE = 3;

    private final Interpolator interpolator;
    private final int behaviourWhenZero;
    private Interpolator linearInterpolator;

    protected InterpolatedPrioMVAUtilCalculator(Interpolator interpolator, int behaviourWhenZero) {
        this.interpolator = interpolator;
        this.behaviourWhenZero = behaviourWhenZero;
        if (behaviourWhenZero == LINEAR_INTERPOLATE) {
            linearInterpolator = new LinearInterpolator();
        }
    }

    @Override
    public double getNonEmptyClosedUtilisation(MVAPopulation population, int numCustomers, int station, int clas) {
        double queueLength = queueLengthMap.get(population)[station][clas];
        double higherQueueLength = Math.ceil(queueLength);
        double lowerQueueLength = Math.floor(queueLength);

        if (higherQueueLength == lowerQueueLength) {
            population.decClassPopulation(clas, queueLength);
            double util = utilMap.get(population)[station][clas];
            population.incClassPopulation(clas, queueLength);
            return util;
        }

        population.decClassPopulation(clas, higherQueueLength);
        double higherUtil = utilMap.get(population)[station][clas];
        population.incClassPopulation(clas, higherQueueLength);

        population.decClassPopulation(clas, lowerQueueLength);
        // The lower util is greater than higher util as less is being subtracted
        // from the population so lowerUtil is just the util associated to the lower util
        double lowerUtil = utilMap.get(population)[station][clas];
        population.incClassPopulation(clas, lowerQueueLength);

        if (higherUtil == 0) {
            return interpolateWhenYTwoIsZero(lowerQueueLength, lowerUtil, higherQueueLength, higherUtil, queueLength);
        }

        return interpolator.interpolate(lowerQueueLength, lowerUtil, higherQueueLength, higherUtil, queueLength);
    }

    public double interpolateWhenYTwoIsZero(double lowerQueueLength, double lowerUtil, double higherQueueLength,
                                            double higherUtil, double queueLength) {
        switch (behaviourWhenZero) {
        case NO_CHANGE:
            return interpolator.interpolate(lowerQueueLength, lowerUtil, higherQueueLength, higherUtil, queueLength);
        case RETURN_Y_ONE:
            return lowerUtil;
        case RETURN_ZERO:
            return 0.0;
        case LINEAR_INTERPOLATE:
            return linearInterpolator.interpolate(lowerQueueLength, lowerUtil, higherQueueLength, higherUtil, queueLength);
        default:
            throw new RuntimeException("Behaviour when y2 = 0 not specified");
        }
    }
}
