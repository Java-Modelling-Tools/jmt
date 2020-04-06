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

package jmt.engine.NetStrategies.RoutingStrategies;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.common.exception.LoadException;
import jmt.engine.NetStrategies.RoutingStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.random.Empirical;
import jmt.engine.random.EmpiricalEntry;
import jmt.engine.random.EmpiricalPar;
import jmt.engine.random.engine.RandomEngine;

/**
 * The node, into which job must be routed, is chosen using an empirical strategy.
 * An empirical strategy is based on an empirical distribution, that is a
 * distribution constructed from the data provided by the user.
 *
 * @author Federico Granata, Stefano Omini, Bertoli Marco
 */
public class EmpiricalStrategy extends RoutingStrategy {

	//the names of the output nodes
	private String[] nodeNames;
	//the empirical distribution
	private Empirical distribution;
	//the parameter of the empirical distribution
	private EmpiricalPar param;
	//the NetNode objects corresponding to the output nodes
	private NetNode[] nodes;

	/**
	 * Creates an empirical strategy using the passed data.
	 * @param entries Entries used to construct the empirical distribution (each entry
	 * contains a node name and the corresponding routing probability).
	 * @throws LoadException
	 * @throws IncorrectDistributionParameterException
	 */
	public EmpiricalStrategy(EmpiricalEntry[] entries) throws LoadException, IncorrectDistributionParameterException {
		nodeNames = new String[entries.length];
		double[] probabilities = new double[entries.length];
		for (int i = 0; i < entries.length; i++) {
			EmpiricalEntry entry = entries[i];
			if (entry.getValue() instanceof String) {
				//sets the name of the node; the corresponding NetNode object will be found later
				nodeNames[i] = (String) entry.getValue();
				//sets the corresponding routing probability
				probabilities[i] = entry.getProbability();
			} else {
				throw new LoadException("Name of the node is not a String");
			}
		}

		//uses the obtained probabilities to create the empirical distribution
		//and its parameter
		distribution = new Empirical();
		param = new EmpiricalPar(probabilities);
	}

	/**
	 * Creates an empirical strategy by adding nodes and naming them with the specified names.
	 * @param distribution The empirical distribution
	 * @param param The parameters of the empirical distribution
	 * @throws LoadException
	 */
	public EmpiricalStrategy(Empirical distribution, EmpiricalPar param) throws LoadException {
		this.distribution = distribution;
		this.param = param;
		//gets the values contained in the empirical parameter (in this case these
		//objects are the names of the output nodes)
		Object[] values = param.getValues();
		nodeNames = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof String) {
				//sets the name of the i-th output node
				nodeNames[i] = (String) values[i];
			} else {
				throw new LoadException("Name of the node is not a String");
			}
		}
	}

	/**
	 * It controls whether the distribution is correct or not.
	 * For the empirical distribution, the parameter is correct if the sum of the
	 * routing probabilities are greater than zero and they sum to 1.0.
	 */
	@Override
	public boolean check() {
		return param.check();
	}

	/**
	 * Gets the output node, into which the job must be routed, using an
	 * empirical strategy.
	 * @param nodeList List of the output nodes.
	 * @param jobClass Class of current job to be routed.
	 * @return Selected node.
	 */
	@Override
		public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		NodeList nodeList = ownerNode.getOutputNodes();
		if (nodes == null) {
			//it is the first execution of this method: find the NetNode objects
			nodes = new NetNode[nodeNames.length];
			for (int i = 0; i < nodeNames.length; i++) {
				nodes[i] = ownerNode.getNetSystem().getNode(nodeNames[i]);
			}
		}

		try {
			//the empirical distribution returns the position of the chosen output node
			int nodePos = (int) Math.round(distribution.nextRand(param));
			if (nodePos >= 0) {
				return nodes[nodePos];
			}
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setRandomEngine(RandomEngine randomEngine) {
		distribution.setRandomEngine(randomEngine); 
	}

}
