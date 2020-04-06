/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

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
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.random.Empirical;
import jmt.engine.random.EmpiricalEntry;
import jmt.engine.random.EmpiricalPar;
import jmt.engine.random.engine.RandomEngine;

public class ProbabilitiesFork extends ForkStrategy {

	private String[] nodeNames;
	private Empirical distribution;
	private EmpiricalPar[] params;
	private EmpiricalPar[] numParams;
	private NetNode[] nodes;

	public ProbabilitiesFork(OutPath[] outPaths) throws LoadException, IncorrectDistributionParameterException {
		nodeNames = new String[outPaths.length];
		distribution = new Empirical();
		params = new EmpiricalPar[outPaths.length];
		numParams = new EmpiricalPar[outPaths.length];
		for (int i = 0; i < outPaths.length; i++) {
			EmpiricalEntry entry = outPaths[i].getProbabilityOfChosen();
			if (entry.getValue() instanceof String) {
				nodeNames[i] = (String) entry.getValue();
				//parameters of each branch
				double probability = entry.getProbability();
				params[i] = new EmpiricalPar(new double [] { probability, 1.0 - probability });
				numParams[i] = new EmpiricalPar(outPaths[i].getJobsPerLinkDis());
			} else {
				throw new LoadException("Name of the node is not a String");
			}
		}
	}

	@Override
	public void setRandomEngine(RandomEngine randomEngine){
		distribution.setRandomEngine(randomEngine); 
	}

	@Override	
    public NodeListWithJobNum getOutNodes(NetNode ownerNode, JobClass jobClass) {
	//NodeList nodeList = ownerNode.getOutputNodes();
		if (nodes == null) {
			//it is the first execution of this method: find the NetNode objects
			nodes = new NetNode[nodeNames.length];
			for (int i = 0; i < nodeNames.length; i++) {
				nodes[i] = ownerNode.getNetSystem().getNode(nodeNames[i]);
			}
		}

		try {
			NodeListWithJobNum nl = new NodeListWithJobNum();
			for (int i = 0; i < params.length; i ++) {
				//the empirical distribution returns 0 if this output node is chosen
				int chosen = (int) Math.round(distribution.nextRand(params[i]));
				if (chosen == 0) {
					int numPos = (int) Math.round(distribution.nextRand(numParams[i]));
					Integer num = Integer.valueOf((String) numParams[i].getValue(numPos));
					Map<JobClass, Integer> map = new HashMap<JobClass, Integer>();
					map.put(jobClass, num);
					nl.add(nodes[i], map);
				}
			}
			return nl;
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

}
