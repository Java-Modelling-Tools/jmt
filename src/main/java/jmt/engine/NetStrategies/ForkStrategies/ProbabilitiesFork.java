package jmt.engine.NetStrategies.ForkStrategies;

import java.util.HashMap;
import java.util.Map;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.common.exception.LoadException;
import jmt.engine.NetStrategies.ForkStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.QueueNet.NodeListWithJobNum;
import jmt.engine.random.Empirical;
import jmt.engine.random.EmpiricalEntry;
import jmt.engine.random.EmpiricalPar;

public class ProbabilitiesFork extends ForkStrategy {
	
	//store the probabilites of selecting branches
	private double probabilities[];
	
	//the names of branches
	private String[] nodeNames;
	
	private Empirical distribution;
	
	private EmpiricalPar[] params;
	
	private NetNode[] nodes;
	
	//the data structure storing the details of each branch
	//name
	//probability of chosen
	//the probabilities p(n) that n jobs are forked to this branch
	private OutPath[] outPaths;

	public ProbabilitiesFork(EmpiricalEntry[] entries) throws LoadException, IncorrectDistributionParameterException{
		probabilities = new double[entries.length];
		nodeNames = new String[entries.length];
		
		distribution = new Empirical();
		params = new EmpiricalPar[entries.length];
		
		for (int i = 0; i < entries.length; i++) {
			EmpiricalEntry entry = entries[i];
			if (entry.getValue() instanceof String) {
				String nodeName = (String) entry.getValue();
				nodeNames[i] = nodeName;
				probabilities[i] = entry.getProbability();
				double[] temp = {probabilities[i], 1.0 - probabilities[i]};
				params[i] = new EmpiricalPar(temp);
			} else {
				throw new LoadException("Name of the node is not a String");
			}
		}		

	}
	
	public ProbabilitiesFork(OutPath[] outPaths) throws LoadException, IncorrectDistributionParameterException{
		this.outPaths = outPaths;
		
		EmpiricalEntry[] entries = new EmpiricalEntry[outPaths.length];
		for (int i = 0; i < entries.length; i ++) {
			entries[i] = outPaths[i].getProbabilityOfChosen();
		}
		
		probabilities = new double[entries.length];
		nodeNames = new String[entries.length];
		
		distribution = new Empirical();
		params = new EmpiricalPar[entries.length];
		
		for (int i = 0; i < entries.length; i++) {
			EmpiricalEntry entry = entries[i];
			if (entry.getValue() instanceof String) {
				String nodeName = (String) entry.getValue();
				nodeNames[i] = nodeName;
				probabilities[i] = entry.getProbability();
				//temp = {P(Chosen), p(Not Chosen)}
				double[] temp = {probabilities[i], 1.0 - probabilities[i]};
				//parameters of each branch
				params[i] = new EmpiricalPar(temp);
			} else {
				throw new LoadException("Name of the node is not a String");
			}
		}
	}

	@Override
	public NodeListWithJobNum getOutNodes(NodeList nodeList, JobClass jobClass) {
		NodeListWithJobNum nl = new NodeListWithJobNum();
		try {

			if (nodes == null) {
				//it is the first execution of this method: find the NetNode objects
				//corresponding to the nodeNames
				findNodes();
			}
			
			for (int i = 0; i < params.length; i ++) {
				//the empirical distribution returns the position of the chosen output node
				int nodePos = (int) distribution.nextRand(params[i]);
				if (nodePos == 0 && nodeList.contains(this.nodes[i])) {
					EmpiricalEntry[] JobsPerLinkDis = outPaths[i].getJobsPerLinkDis();
					EmpiricalPar numParam = new EmpiricalPar(JobsPerLinkDis);
					
					Integer num = Integer.parseInt((String)JobsPerLinkDis[(int)distribution.nextRand(numParam)].getValue());
					Map<JobClass, Integer> map = new HashMap<JobClass, Integer>();
                                        map.put(jobClass, num);
                                        nl.add(this.nodes[i], map);
				}
			}
			return nl;
			
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private boolean findNodes() {
		nodes = new NetNode[nodeNames.length];

		for (int i = 0; i < nodeNames.length; i++) {
			nodes[i] = NetSystem.getNode(nodeNames[i]);
			if (nodes[i] == null) {
				//the passed name does not correspond to a node of the model
				return false;
			}
		}
		return true;
	}

}
