package jmt.engine.NodeSections;

import jmt.engine.NetStrategies.ServiceStrategy;

public class GatedPollingServer extends PollingServer {
    private int currentQueue;
    public
    GatedPollingServer(Integer numberOfServers,
                       Integer[] numberOfVisitsPerClass,
                       ServiceStrategy[] serviceStrategies,
                       ServiceStrategy[] switchoverStrategies)
    {
        super(numberOfServers, numberOfVisitsPerClass, serviceStrategies, switchoverStrategies);
    }
}
