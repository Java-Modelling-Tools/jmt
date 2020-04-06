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
import jmt.engine.QueueNet.NodeListWithJobNum;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.random.Empirical;
import jmt.engine.random.EmpiricalEntry;
import jmt.engine.random.EmpiricalPar;
import jmt.engine.random.engine.RandomEngine;

public class CombFork extends ForkStrategy {

	private String[] nodeNums;
	private Empirical distribution;
	private EmpiricalPar param;
	private NetNode[] nodes;

	public CombFork(EmpiricalEntry[] entries) throws LoadException, IncorrectDistributionParameterException {
		nodeNums = new String[entries.length];
		double[] probabilities = new double[entries.length];
		for (int i = 0; i < entries.length; i++) {
			EmpiricalEntry entry = entries[i];
			if (entry.getValue() instanceof String) {
				nodeNums[i] = (String) entry.getValue();
				probabilities[i] = entry.getProbability();
			} else {
				throw new LoadException("Name of the node is not a String");
			}
		}
		distribution = new Empirical();
		param = new EmpiricalPar(probabilities);
	}
	@Override
	public void setRandomEngine(RandomEngine randomEngine){
		distribution.setRandomEngine(randomEngine); 
	}
	@Override
	public NodeListWithJobNum getOutNodes(NetNode ownerNode, JobClass jobClass) {
		NetSystem netSystem = ownerNode.getNetSystem();
		RandomEngine engine = netSystem.getEngine();
		NodeListWithJobNum nl = new NodeListWithJobNum();

		if (nodes == null) {
			//it is the first execution of this method: find the NetNode objects
			nodes = ownerNode.getOutputNodes().toArray();
		}

		try {
			int numPos = (int) Math.round(distribution.nextRand(param));
			Integer num = Integer.parseInt(nodeNums[numPos]);

			// Reservoir Sampling
			int[] indices = new int[num];
			for (int i = 0; i < num; i++) {
				indices[i] = i;
			}
			for (int i = num; i < nodes.length; i++) {
				int j = (int) Math.floor(engine.raw() * (i + 1));
				if (j < num) {
					indices[j] = i;
				}
			}
			
			for (int i = 0; i < indices.length; i++) {
				Map<JobClass, Integer> map = new HashMap<JobClass, Integer>();
				map.put(jobClass, Integer.valueOf(1));
				nl.add(nodes[indices[i]], map);
			}
			return nl;
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

}
