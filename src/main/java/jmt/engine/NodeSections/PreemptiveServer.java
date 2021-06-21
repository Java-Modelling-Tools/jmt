package jmt.engine.NodeSections;

import jmt.engine.NetStrategies.ServiceStrategy;
public class PreemptiveServer extends Server {
	public PreemptiveServer(Integer numberOfServers, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies) {
	    super(numberOfServers, true, numberOfVisitsPerClass, serviceStrategies);
    }
}
