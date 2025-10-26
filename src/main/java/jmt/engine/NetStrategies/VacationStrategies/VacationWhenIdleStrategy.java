package jmt.engine.NetStrategies.VacationStrategies;


import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.VacationStrategy;
import jmt.engine.random.Distribution;
import jmt.engine.random.Parameter;

public class VacationWhenIdleStrategy implements VacationStrategy {
    public static final int SINGLE_VACATION = 0;
    public static final int MULTIPLE_VACATION = 1;

    private final int type;
    private final Distribution vacationTimeDistr;
    private final Parameter vacationTimePar;

    public VacationWhenIdleStrategy(Integer type, Distribution vacationTimeDistr, Parameter vacationTimePar) {
        this.type = type;
        this.vacationTimeDistr = vacationTimeDistr;
        this.vacationTimePar = vacationTimePar;
    }

    @Override
    public double nextVacationLength() throws NetException {
        try {
            return vacationTimeDistr.nextRand(vacationTimePar);
        } catch (IncorrectDistributionParameterException e) {
            throw new NetException("Error in VacationWhenIdleStrategy: " + e.getMessage());
        }
    }

    public int getType() {
        return type;
    }
}
