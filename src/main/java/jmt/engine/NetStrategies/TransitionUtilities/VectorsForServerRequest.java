package jmt.engine.NetStrategies.TransitionUtilities;

import java.util.Set;

public class VectorsForServerRequest {

    private TransitionVector enabling;
    private TransitionVector resource;
    private int numberOfRequestsToNodes;

    public VectorsForServerRequest(TransitionVector enabling, TransitionVector resource, int numberOfRequestsToNodes){
        this.enabling = enabling;
        this.resource = resource;
        this.numberOfRequestsToNodes = numberOfRequestsToNodes;
    }

    public TransitionVector getEnabling() {
        return enabling;
    }

    public TransitionVector getResource() {
        return resource;
    }

    public int getRequestsToNodes(){
        return numberOfRequestsToNodes;
    }

}
