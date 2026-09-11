package NetStrategies.TransitionUtilities;

import QueueNet.JobInfo;

import java.util.List;

public class EventFinishPacket {
    public int numberOfRequestsToNodes;

    public List<JobInfo> list;

    public EventFinishPacket(int n, List<JobInfo> l){
        this.numberOfRequestsToNodes = n;
        this.list = l;
    }
}
