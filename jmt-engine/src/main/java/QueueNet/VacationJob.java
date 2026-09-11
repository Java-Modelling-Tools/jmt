package QueueNet;

/**
 * A class containing the information about a server vacation job for performance indexing.
 *
 * Currently, the information stored is not used, but can be added if needed
 * by some advanced performance metrics in the future.
 */
public class VacationJob {
    private double time;

    public VacationJob(double time) {
        this.time = time;
    }

    public double getTime() {
        return time;
    }
}
