package jmt.engine.NetStrategies.VacationStrategies;

public class QueueIdleAckBody {
    private final boolean serverStartsVacation;

    public QueueIdleAckBody(boolean serverStartsVacation) {
        this.serverStartsVacation = serverStartsVacation;
    }

    public boolean isServerStartsVacation() {
        return serverStartsVacation;
    }

    @Override
    public String toString() {
        return "{serverStartsVacation: " + serverStartsVacation + "}";
    }
}
