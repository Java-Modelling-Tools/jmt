package jmt.engine.NetStrategies;

import java.util.HashMap;

import jmt.common.AutoCheck;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.QueueNet.NodeListWithJobNum;
import jmt.engine.QueueNet.NodeSection;

public abstract class ForkStrategy implements AutoCheck {

	public abstract NodeListWithJobNum getOutNodes(NodeList nodes, JobClass jobClass);

	public NodeListWithJobNum getOutNodes(NodeSection callingSection, JobClass jobClass) {
		return getOutNodes(callingSection.getOwnerNode().getOutputNodes(),
				jobClass);
	}
	
	public boolean check() {
		return true;
	}
}
