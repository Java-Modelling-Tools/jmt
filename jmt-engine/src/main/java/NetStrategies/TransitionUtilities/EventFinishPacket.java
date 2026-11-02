package NetStrategies.TransitionUtilities;

import QueueNet.JobInfo;

import java.util.List;

/**
 * Packet exchanged when a packet switching event finishes in the JSIM engine.
 */
/**
 * Packet exchanged when a packet switching event finishes in the JSIM engine.
 */
/**
 * Packet exchanged when a packet switching event finishes in the JSIM engine.
 */
/**
 * Packet exchanged when a packet switching event finishes in the JSIM engine.
 */
public class EventFinishPacket {
    public int numberOfRequestsToNodes;

    public List<JobInfo> list;

    public EventFinishPacket(int n, List<JobInfo> l){
        this.numberOfRequestsToNodes = n;
        this.list = l;
    }
}
