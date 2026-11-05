package QueueNet;

import NetStrategies.TransitionUtilities.TransitionVector;

/**
 * Request describing a job that must be placed into a queue.
 */
/**
 * Request describing a job that must be placed into a queue.
 */
/**
 * Request describing a job that must be placed into a queue.
 */
/**
 * Request describing a job that must be placed into a queue.
 */
public class RequestToPlace {
    public TransitionVector enablingVector;
    public TransitionVector resourceVector;

    public RequestToPlace(TransitionVector enablingVector, TransitionVector resourceVector){
        this.enablingVector = enablingVector;
        this.resourceVector = resourceVector;
    }
}
