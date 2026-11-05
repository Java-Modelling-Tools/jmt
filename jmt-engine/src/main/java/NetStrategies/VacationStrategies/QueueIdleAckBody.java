package NetStrategies.VacationStrategies;

/**
 * Acknowledgement body exchanged when a queue becomes idle in a vacation model.
 */
/**
 * Acknowledgement body exchanged when a queue becomes idle in a vacation model.
 */
/**
 * Acknowledgement body exchanged when a queue becomes idle in a vacation model.
 */
/**
 * Acknowledgement body exchanged when a queue becomes idle in a vacation model.
 */
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
