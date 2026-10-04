package NetStrategies;


import jmt.common.exception.NetException;

public interface VacationStrategy {
    double nextVacationLength() throws NetException;
}
