package jmt.engine.QueueNet;

import java.util.HashMap;
import java.util.Map;

public class NodeListWithJobNum {
	private Map<NetNode, Map<JobClass, Integer>> nodesWithJobs;
	private NodeList nodes;
	private int total = 0;
	
	public NodeListWithJobNum(){
		nodesWithJobs= new HashMap<NetNode, Map<JobClass, Integer>>();
		nodes = new NodeList();
	}
	
	public void add(NetNode netNode, Map<JobClass, Integer> num){
		nodesWithJobs.put(netNode, num);
		nodes.add(netNode);
                for(Integer i: num.values())
                    total = total + i;
	}
	
	public NodeList getNodeList(){
		return this.nodes;
	}
	
	public Map<JobClass, Integer> getClassesWithNum(NetNode netNode){
		return nodesWithJobs.get(netNode);
	}
	
	public int getTotalNum(){
		return total;
	}
}