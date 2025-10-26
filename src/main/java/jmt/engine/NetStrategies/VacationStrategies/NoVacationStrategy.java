package jmt.engine.NetStrategies.VacationStrategies;

import jmt.engine.NetStrategies.VacationStrategy;

public class NoVacationStrategy implements VacationStrategy {
    @Override
    public double nextVacationLength() {
        return 0;
    }
}
