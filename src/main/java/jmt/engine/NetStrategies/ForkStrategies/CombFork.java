package jmt.engine.NetStrategies.ForkStrategies;

import java.util.HashMap;
import java.util.HashSet;
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
import jmt.engine.random.engine.RandomEngine;

public class CombFork extends ForkStrategy{
	
	private String[] nodeNums;
	private double[] probabilities;
	private EmpiricalPar param;
	private NetNode[] nodes;
	private Empirical distribution;
	private RandomEngine engine = RandomEngine.makeDefault();
        
	public CombFork(EmpiricalEntry[] entries)throws LoadException, IncorrectDistributionParameterException {
		nodeNums = new String[entries.length];
		probabilities = new double[entries.length];
		distribution = new Empirical();
		for (int i = 0; i < entries.length; i++) {
			EmpiricalEntry entry = entries[i];
			if (entry.getValue() instanceof String) {
				String nodeNum = (String) entry.getValue();
				nodeNums[i] = nodeNum;
				probabilities[i] = entry.getProbability();
			} else {
				throw new LoadException("Name of the node is not a String");
			}
		}
	}
	
	@Override
	public NodeListWithJobNum getOutNodes(NodeList nodes, JobClass jobClass) {
		NodeListWithJobNum nl = new NodeListWithJobNum();

		try {

			if (nodes == null) {
				//it is the first execution of this method: find the NetNode objects
				//corresponding to the nodeNames
				findNodes();
			}
			param = new EmpiricalPar(probabilities);
			int temp = (int)distribution.nextRand(param);
			Integer num = Integer.parseInt(nodeNums[temp]);
			
			//using set to avoid duplication
			HashSet<Integer> pickedIndex = new HashSet<Integer>();
			//if the nodes picked are less the required number, continue the picking
			while (pickedIndex.size() < num) {
				//generate random number 1 ~ size of branches
                                //replaced Math.Random() code
				int index = (int)(engine.raw2() * nodes.size());
				//add to the NodeListWithJobNum
				//OLD
				//nl.add(nodes.get(index), 1);
				//END
				pickedIndex.add(index);
			}
			//add to the NodeListWithJobNum
			for (Integer i : pickedIndex) {
				Map<JobClass, Integer> map = new HashMap<JobClass, Integer>();
                                map.put(jobClass, 1);
                                nl.add(nodes.get(i), map);
			}
			
			return nl;
			
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean findNodes() {
		nodes = new NetNode[nodeNums.length];

		for (int i = 0; i < nodeNums.length; i++) {
			nodes[i] = NetSystem.getNode(nodeNums[i]);
			if (nodes[i] == null) {
				//the passed name does not correspond to a node of the model
				return false;
			}
		}
		return true;
	}

}
