package jmt.engine.NodeSections;

import jmt.engine.NetStrategies.ServiceStrategy;
public class ExhaustivePollingServer extends PollingServer {
    public
    ExhaustivePollingServer(Integer numberOfServers,
                            Integer[] numberOfVisitsPerClass,
                            ServiceStrategy[] serviceStrategies,
                            ServiceStrategy[] switchoverStrategies)
    {
        super(numberOfServers, numberOfVisitsPerClass, serviceStrategies, switchoverStrategies);
    }
}
