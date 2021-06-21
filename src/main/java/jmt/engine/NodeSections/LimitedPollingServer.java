package jmt.engine.NodeSections;

import jmt.engine.NetStrategies.ServiceStrategy;

public class LimitedPollingServer extends PollingServer {
    public
    LimitedPollingServer(Integer numberOfServers,
                         Integer[] numberOfVisitsPerClass,
                         ServiceStrategy[] serviceStrategies,
                         ServiceStrategy[] switchoverStrategies,
                         Integer pollingK)
    {
        super(numberOfServers, numberOfVisitsPerClass, serviceStrategies, switchoverStrategies);
    }

}

