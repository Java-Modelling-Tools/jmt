package QueueNet;

import NetStrategies.TransitionUtilities.TransitionVector;

public class RequestToPlace {
    public TransitionVector enablingVector;
    public TransitionVector resourceVector;

    public RequestToPlace(TransitionVector enablingVector, TransitionVector resourceVector){
        this.enablingVector = enablingVector;
        this.resourceVector = resourceVector;
    }
}
