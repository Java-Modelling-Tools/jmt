package NetStrategies.VacationStrategies;

import NetStrategies.VacationStrategy;

public class NoVacationStrategy implements VacationStrategy {
    @Override
    public double nextVacationLength() {
        return 0;
    }
}
